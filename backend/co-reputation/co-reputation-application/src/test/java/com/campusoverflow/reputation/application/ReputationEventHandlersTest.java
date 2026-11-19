package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.campusoverflow.identity.api.event.UserRegisteredEvent;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryAccounts;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryBadges;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryLedger;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.RecordingPublisher;
import com.campusoverflow.reputation.domain.BadgeType;
import com.campusoverflow.reputation.domain.LedgerEntry;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 事件订阅端的行为：每个处理器各自幂等，且「声誉、徽章、悬赏结算」三件事的触发条件不同。
 *
 * <p>这里刻意用真实的 {@link ReputationLedgerService} 配内存 Fake，而不是把记账服务也 mock 掉——
 * 处理器真正的风险在于「该记的时候没记 / 该拦的时候没拦」，mock 掉记账会把这类缺陷一起屏蔽。</p>
 */
class ReputationEventHandlersTest {

    private static final Instant NOW = Instant.parse("2026-11-12T02:00:00Z");

    private final InMemoryAccounts accounts = new InMemoryAccounts();
    private final InMemoryLedger ledger = new InMemoryLedger();
    private final InMemoryBadges badges = new InMemoryBadges();
    private final RecordingPublisher events = new RecordingPublisher();
    private final BountyService bounties = mock(BountyService.class);
    private final BadgeService badgeService = new BadgeService(badges, events, Clock.fixed(NOW, ZoneOffset.UTC));
    private final ReputationLedgerService ledgerService = new ReputationLedgerService(accounts, ledger, badgeService,
            events, Clock.fixed(NOW, ZoneOffset.UTC));
    private final ReputationEventHandlers handlers = new ReputationEventHandlers(accounts, ledger, ledgerService, badgeService,
            bounties, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("注册事件：按角色开立账户，教师初始 1000 分、学生 0 分")
    void userRegisteredOpensAccountWithRoleBasedPoints() {
        handlers.on(new UserRegisteredEvent("e1", NOW, 2, "小明", Role.STUDENT));
        handlers.on(new UserRegisteredEvent("e2", NOW, 9, "王老师", Role.TEACHER));

        assertThat(accounts.findByUserId(2)).get()
                .extracting(ReputationAccount::availablePoints).isEqualTo(0);
        assertThat(accounts.findByUserId(9)).get()
                .extracting(ReputationAccount::availablePoints).isEqualTo(1000);
    }

    @Test
    @DisplayName("注册事件重复投递：账户已存在则不重复开立")
    void userRegisteredIsIdempotent() {
        accounts.put(ReputationAccount.reconstitute(2, 30, 5, 0, 3));

        handlers.on(new UserRegisteredEvent("e1", NOW, 2, "小明", Role.STUDENT));

        assertThat(accounts.saveCalls()).isZero();
        assertThat(accounts.findByUserId(2)).get()
                .extracting(ReputationAccount::reputation).isEqualTo(30);
    }

    @Test
    @DisplayName("采纳他人回答：记 +15、授予「初露锋芒」、并触发悬赏结算")
    void answerAcceptedRecordsPointsGrantsBadgeAndSettlesBounty() {
        handlers.on(new AnswerAcceptedEvent("e1", NOW, 77, 10, 2, 3, 3, "如何理解聚合根"));

        LedgerEntry entry = ledger.only();
        assertThat(entry.userId()).isEqualTo(2);
        assertThat(entry.reason()).isEqualTo(ReputationReason.ANSWER_ACCEPTED);
        assertThat(entry.delta()).isEqualTo(15);
        assertThat(badges.typesOf(2)).containsExactly(BadgeType.FIRST_ACCEPTED);
        verify(bounties).settleOnAccept(10, 77, 2, "e1");
    }

    @Test
    @DisplayName("自问自答采纳：声誉增量为 0，不记流水也不授徽章，但悬赏结算仍要走")
    void selfAcceptedAnswerEarnsNoReputationButStillSettlesBounty() {
        handlers.on(new AnswerAcceptedEvent("e1", NOW, 77, 10, 2, 2, 2, "自问自答"));

        assertThat(ledger.entries()).isEmpty();
        assertThat(badges.awardCount()).isZero();
        verify(bounties).settleOnAccept(10, 77, 2, "e1");
    }

    @Test
    @DisplayName("问题被点赞：记 QUESTION_VOTE +10")
    void voteOnQuestionUsesQuestionVoteReason() {
        handlers.on(new VoteCastEvent("e1", NOW, "QUESTION", 10, 2, 3, 10, 0, 1));

        LedgerEntry entry = ledger.only();
        assertThat(entry.reason()).isEqualTo(ReputationReason.QUESTION_VOTE);
        assertThat(entry.delta()).isEqualTo(10);
        assertThat(entry.refType()).isEqualTo("QUESTION");
        assertThat(entry.refId()).isEqualTo(10);
    }

    @Test
    @DisplayName("回答由踩改赞：增量按差计算（10 - (-2) = 12）")
    void voteChangeUsesDeltaBetweenOldAndNew() {
        handlers.on(new VoteCastEvent("e1", NOW, "ANSWER", 77, 2, 3, 10, -1, 1));

        assertThat(ledger.only().delta()).isEqualTo(12);
        assertThat(ledger.only().reason()).isEqualTo(ReputationReason.ANSWER_VOTE);
    }

    @Test
    @DisplayName("每日上限：今天已得 195，本次 +10 只记 5")
    void dailyVoteCapTruncatesDelta() {
        ledger.append(2, ReputationReason.ANSWER_VOTE, 195, NOW);

        handlers.on(new VoteCastEvent("e1", NOW, "ANSWER", 77, 2, 3, 10, 0, 1));

        List<LedgerEntry> all = ledger.entries();
        assertThat(all).hasSize(2);
        assertThat(all.get(1).delta()).isEqualTo(5);
    }

    @Test
    @DisplayName("提交回答：只授徽章，不动声誉")
    void answerSubmittedOnlyGrantsBadge() {
        handlers.on(new AnswerSubmittedEvent("e1", NOW, 77, 10, 2, 3, "标题"));

        assertThat(badges.typesOf(2)).containsExactly(BadgeType.FIRST_ANSWER);
        assertThat(ledger.entries()).isEmpty();
        verifyNoInteractions(bounties);
    }

    @Test
    @DisplayName("教师认证：授「名师认可」，不动声誉")
    void answerEndorsedOnlyGrantsBadge() {
        handlers.on(new AnswerEndorsedEvent("e1", NOW, 77, 10, 2, 9, "标题"));

        assertThat(badges.typesOf(2)).containsExactly(BadgeType.TEACHER_ENDORSED);
        assertThat(ledger.entries()).isEmpty();
    }
}
