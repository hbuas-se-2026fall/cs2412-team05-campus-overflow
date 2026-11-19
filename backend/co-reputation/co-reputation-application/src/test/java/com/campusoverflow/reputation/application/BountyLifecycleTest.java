package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.qa.api.QaQueryApi;
import com.campusoverflow.reputation.api.event.BountyAwardedEvent;
import com.campusoverflow.reputation.api.event.BountyClosedEvent;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryAccounts;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryBadges;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryLedger;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.RecordingPublisher;
import com.campusoverflow.reputation.domain.Bounty;
import com.campusoverflow.reputation.domain.BountyRepository;
import com.campusoverflow.reputation.domain.BountyStatus;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 悬赏的生命周期：采纳结算、自采纳退款、到期退款、最新悬赏查询。
 *
 * <p>与 {@code BountyServiceTest}（并发唯一键）互补：那个类守的是「同时开两个悬赏」，
 * 这个类守的是「钱从冻结到退回/付出的每一步都没漏」。</p>
 */
class BountyLifecycleTest {

    private static final Instant NOW = Instant.parse("2026-11-12T02:00:00Z");
    private static final long SPONSOR = 9;
    private static final long WINNER = 2;
    private static final long QUESTION = 10;

    private final BountyRepository bounties = mock(BountyRepository.class);
    private final InMemoryAccounts accounts = new InMemoryAccounts();
    private final InMemoryLedger ledger = new InMemoryLedger();
    private final InMemoryBadges badges = new InMemoryBadges();
    private final RecordingPublisher events = new RecordingPublisher();
    private final QaQueryApi qa = mock(QaQueryApi.class);
    private final IdentityApi identity = mock(IdentityApi.class);
    private final AuditTrail audit = mock(AuditTrail.class);

    private final ReputationLedgerService ledgerService = new ReputationLedgerService(accounts, ledger,
            new BadgeService(badges, events, Clock.fixed(NOW, ZoneOffset.UTC)), events, Clock.fixed(NOW, ZoneOffset.UTC));
    private final BountyService service = new BountyService(bounties, accounts, ledgerService, qa, identity, events,
            audit, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void setUp() {
        // 发起人已冻结 100 分，可用 900 分
        accounts.put(ReputationAccount.reconstitute(SPONSOR, 0, 900, 100, 0));
    }

    private Bounty openBounty(Instant expiresAt) {
        Bounty bounty = Bounty.reconstitute(5L, QUESTION, SPONSOR, 100, BountyStatus.OPEN, NOW, expiresAt, null, null,
                null);
        when(bounties.findOpenByQuestion(QUESTION)).thenReturn(Optional.of(bounty));
        return bounty;
    }

    @Test
    @DisplayName("问题没有进行中的悬赏：结算直接返回，不碰账户也不发事件")
    void settleDoesNothingWhenNoOpenBounty() {
        when(bounties.findOpenByQuestion(QUESTION)).thenReturn(Optional.empty());

        service.settleOnAccept(QUESTION, 77, WINNER, "evt-1");

        assertThat(accounts.saveCalls()).isZero();
        assertThat(ledger.entries()).isEmpty();
        assertThat(events.events()).isEmpty();
    }

    @Test
    @DisplayName("采纳他人回答：扣冻结、标记 AWARDED、给赢家记 BOUNTY_AWARDED 并发布事件")
    void settleAwardsWinnerAndReleasesFrozenPoints() {
        Bounty bounty = openBounty(NOW.plus(Duration.ofDays(7)));

        service.settleOnAccept(QUESTION, 77, WINNER, "evt-1");

        assertThat(bounty.status()).isEqualTo(BountyStatus.AWARDED);
        assertThat(bounty.winnerId()).isEqualTo(WINNER);
        assertThat(bounty.answerId()).isEqualTo(77);
        assertThat(accounts.findByUserId(SPONSOR)).get()
                .extracting(ReputationAccount::frozenPoints).isEqualTo(0);
        assertThat(accounts.findByUserId(SPONSOR)).get()
                .extracting(ReputationAccount::availablePoints).isEqualTo(900);

        assertThat(ledger.only().userId()).isEqualTo(WINNER);
        assertThat(ledger.only().reason()).isEqualTo(ReputationReason.BOUNTY_AWARDED);
        assertThat(ledger.only().delta()).isEqualTo(100);
        assertThat(ledger.only().refType()).isEqualTo("BOUNTY");
        assertThat(ledger.only().refId()).isEqualTo(5L);

        // 同一次结算会产出三类事件：声誉变更、可能触发的徽章、悬赏发放；这里只关心最后一种。
        List<BountyAwardedEvent> awarded = events.eventsOf(BountyAwardedEvent.class);
        assertThat(awarded).hasSize(1);
        assertThat(awarded.get(0).bountyId()).isEqualTo(5L);
        assertThat(awarded.get(0).winnerId()).isEqualTo(WINNER);
        assertThat(awarded.get(0).points()).isEqualTo(100);
    }

    @Test
    @DisplayName("发起人自己的回答被采纳：不付悬赏，关闭并把冻结分退回")
    void selfAcceptClosesBountyAndRefunds() {
        Bounty bounty = openBounty(NOW.plus(Duration.ofDays(7)));

        service.settleOnAccept(QUESTION, 77, SPONSOR, "evt-1");

        assertThat(bounty.status()).isEqualTo(BountyStatus.EXPIRED);
        assertThat(accounts.findByUserId(SPONSOR)).get()
                .extracting(ReputationAccount::availablePoints).isEqualTo(1000);
        assertThat(accounts.findByUserId(SPONSOR)).get()
                .extracting(ReputationAccount::frozenPoints).isEqualTo(0);
        assertThat(ledger.entries()).isEmpty();

        BountyClosedEvent event = (BountyClosedEvent) events.only();
        assertThat(event.refundedPoints()).isEqualTo(100);
        assertThat(event.sponsorId()).isEqualTo(SPONSOR);
    }

    @Test
    @DisplayName("采纳时悬赏已过期：同样走关闭退款，不产生获奖流水")
    void settleAfterExpiryRefunds() {
        Bounty bounty = openBounty(NOW.minus(Duration.ofSeconds(1)));

        service.settleOnAccept(QUESTION, 77, WINNER, "evt-1");

        assertThat(bounty.status()).isEqualTo(BountyStatus.EXPIRED);
        assertThat(ledger.entries()).isEmpty();
        assertThat(events.only()).isInstanceOf(BountyClosedEvent.class);
    }

    @Test
    @DisplayName("发起人账户不存在：抛 REP-4001，不静默吞掉")
    void settleFailsWhenSponsorAccountMissing() {
        accounts.put(ReputationAccount.reconstitute(999, 0, 0, 0, 0));
        Bounty bounty = Bounty.reconstitute(5L, QUESTION, 404, 100, BountyStatus.OPEN, NOW,
                NOW.plus(Duration.ofDays(7)), null, null, null);
        when(bounties.findOpenByQuestion(QUESTION)).thenReturn(Optional.of(bounty));

        assertThatThrownBy(() -> service.settleOnAccept(QUESTION, 77, WINNER, "evt-1"))
                .isInstanceOfSatisfying(NotFoundException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-4001"));
    }

    @Test
    @DisplayName("到期扫描：逐条关闭退款，并返回处理条数")
    void expireDueRefundsEveryBounty() {
        Bounty first = Bounty.reconstitute(5L, QUESTION, SPONSOR, 100, BountyStatus.OPEN, NOW,
                NOW.minus(Duration.ofDays(1)), null, null, null);
        Bounty second = Bounty.reconstitute(6L, 11L, SPONSOR, 50, BountyStatus.OPEN, NOW,
                NOW.minus(Duration.ofDays(2)), null, null, null);
        accounts.put(ReputationAccount.reconstitute(SPONSOR, 0, 850, 150, 0));
        when(bounties.findDue(NOW, 100)).thenReturn(List.of(first, second));

        int handled = service.expireDue();

        assertThat(handled).isEqualTo(2);
        assertThat(first.status()).isEqualTo(BountyStatus.EXPIRED);
        assertThat(second.status()).isEqualTo(BountyStatus.EXPIRED);
        assertThat(accounts.findByUserId(SPONSOR)).get()
                .extracting(ReputationAccount::availablePoints).isEqualTo(1000);
        assertThat(accounts.findByUserId(SPONSOR)).get()
                .extracting(ReputationAccount::frozenPoints).isEqualTo(0);
        assertThat(events.events()).hasSize(2);
    }

    @Test
    @DisplayName("最新悬赏：带出发起人昵称；用户已注销时降级为「已注销用户」")
    void latestMapsSponsorName() {
        Bounty bounty = Bounty.reconstitute(5L, QUESTION, SPONSOR, 100, BountyStatus.OPEN, NOW,
                NOW.plus(Duration.ofDays(7)), null, null, null);
        when(bounties.findLatestByQuestion(QUESTION)).thenReturn(Optional.of(bounty));
        when(identity.findUser(SPONSOR))
                .thenReturn(Optional.of(new UserSummary(SPONSOR, "王老师", Role.TEACHER, true, "计算机工程学院")));

        BountyView view = service.latest(QUESTION).orElseThrow();

        assertThat(view.sponsorName()).isEqualTo("王老师");
        assertThat(view.status()).isEqualTo("OPEN");
        assertThat(view.points()).isEqualTo(100);

        when(identity.findUser(SPONSOR)).thenReturn(Optional.empty());
        assertThat(service.latest(QUESTION).orElseThrow().sponsorName()).isEqualTo("已注销用户");
    }

    @Test
    @DisplayName("最新悬赏：问题从来没有悬赏时返回 empty，而不是抛错")
    void latestIsEmptyWhenNeverOpened() {
        when(bounties.findLatestByQuestion(QUESTION)).thenReturn(Optional.empty());

        assertThat(service.latest(QUESTION)).isEmpty();
        verify(identity, never()).findUser(anyLong());
        verify(bounties, never()).findDue(any(), anyInt());
    }
}
