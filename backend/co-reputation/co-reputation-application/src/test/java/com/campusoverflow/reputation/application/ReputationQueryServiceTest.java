package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryAccounts;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryBadges;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryLedger;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.StubIdentity;
import com.campusoverflow.reputation.domain.BadgeAward;
import com.campusoverflow.reputation.domain.BadgeType;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 查询侧的两条硬规则：积分只给本人/管理员看；流水属于个人数据。 */
class ReputationQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-12T02:00:00Z");

    private final InMemoryAccounts accounts = new InMemoryAccounts();
    private final InMemoryLedger ledger = new InMemoryLedger();
    private final InMemoryBadges badges = new InMemoryBadges();
    private final StubIdentity identity = new StubIdentity()
            .put(new UserSummary(2, "小明", Role.STUDENT, true, "计算机工程学院"))
            .put(new UserSummary(3, "小红", Role.STUDENT, false, "计算机工程学院"));
    private final ReputationQueryService service = new ReputationQueryService(accounts, ledger, badges, identity);

    private final Actor self = new Actor(2, "小明", Role.STUDENT);
    private final Actor other = new Actor(3, "小红", Role.STUDENT);
    private final Actor admin = new Actor(1, "admin", Role.ADMIN);

    @Test
    @DisplayName("用户不存在：抛 REP-4002，而不是返回一个空档案")
    void profileOfUnknownUserThrowsNotFound() {
        assertThatThrownBy(() -> service.profile(99, self))
                .isInstanceOfSatisfying(NotFoundException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-4002"));
    }

    @Test
    @DisplayName("账户尚未开立：按 0 分开立临时档案，不报错")
    void profileOfUserWithoutAccountDefaultsToZero() {
        ReputationProfileView view = service.profile(2, self);

        assertThat(view.reputation()).isZero();
        assertThat(view.availablePoints()).isZero();
        assertThat(view.frozenPoints()).isZero();
    }

    @Test
    @DisplayName("他人查看：声誉可见，积分字段为 null")
    void pointsAreHiddenFromOthers() {
        accounts.put(ReputationAccount.reconstitute(2, 50, 30, 20, 0));

        ReputationProfileView view = service.profile(2, other);

        assertThat(view.reputation()).isEqualTo(50);
        assertThat(view.availablePoints()).isNull();
        assertThat(view.frozenPoints()).isNull();
    }

    @Test
    @DisplayName("未登录查看：同样看不到积分")
    void pointsAreHiddenFromAnonymous() {
        accounts.put(ReputationAccount.reconstitute(2, 50, 30, 20, 0));

        assertThat(service.profile(2, null).availablePoints()).isNull();
    }

    @Test
    @DisplayName("本人与管理员查看：积分完整可见")
    void selfAndAdminSeePoints() {
        accounts.put(ReputationAccount.reconstitute(2, 50, 30, 20, 0));

        ReputationProfileView mine = service.profile(2, self);
        ReputationProfileView asAdmin = service.profile(2, admin);

        assertThat(mine.availablePoints()).isEqualTo(30);
        assertThat(mine.frozenPoints()).isEqualTo(20);
        assertThat(asAdmin.availablePoints()).isEqualTo(30);
        assertThat(asAdmin.frozenPoints()).isEqualTo(20);
    }

    @Test
    @DisplayName("档案带出徽章：代码、显示名、说明与授予时间")
    void profileMapsBadges() {
        badges.award(new BadgeAward(2, BadgeType.FIRST_ANSWER, NOW));

        ReputationProfileView view = service.profile(2, self);

        assertThat(view.badges()).hasSize(1);
        ReputationProfileView.BadgeView badge = view.badges().get(0);
        assertThat(badge.code()).isEqualTo("FIRST_ANSWER");
        assertThat(badge.name()).isEqualTo(BadgeType.FIRST_ANSWER.displayName());
        assertThat(badge.description()).isEqualTo(BadgeType.FIRST_ANSWER.description());
        assertThat(badge.awardedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("他人查看流水：抛 REP-3002")
    void ledgerOfOthersIsForbidden() {
        assertThatThrownBy(() -> service.ledger(2, other, new PageRequest(1, 20)))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-3002"));
    }

    @Test
    @DisplayName("本人查看流水：按页返回，并带出原因码与中文说明")
    void ledgerOfSelfIsMapped() {
        ledger.append(2, ReputationReason.ANSWER_ACCEPTED, 15, NOW)
                .append(2, ReputationReason.ANSWER_VOTE, 10, NOW)
                .append(3, ReputationReason.ANSWER_ACCEPTED, 15, NOW);

        PageResult<LedgerEntryView> page = service.ledger(2, self, new PageRequest(1, 20));

        assertThat(page.total()).isEqualTo(2);
        assertThat(page.items()).extracting(LedgerEntryView::reason)
                .containsExactly("ANSWER_ACCEPTED", "ANSWER_VOTE");
        assertThat(page.items()).extracting(LedgerEntryView::reasonLabel)
                .containsExactly(ReputationReason.ANSWER_ACCEPTED.label(), ReputationReason.ANSWER_VOTE.label());
        assertThat(page.items()).extracting(LedgerEntryView::delta).containsExactly(15, 10);
        assertThat(page.items()).extracting(LedgerEntryView::createdAt).containsOnly(NOW);
    }

    @Test
    @DisplayName("管理员查看他人流水：放行")
    void adminCanReadAnyLedger() {
        ledger.append(2, ReputationReason.ANSWER_ACCEPTED, 15, NOW);

        assertThat(service.ledger(2, admin, new PageRequest(1, 20)).total()).isEqualTo(1);
    }
}
