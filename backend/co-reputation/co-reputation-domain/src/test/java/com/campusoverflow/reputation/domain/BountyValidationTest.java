package com.campusoverflow.reputation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 悬赏聚合的参数边界、到期判定与状态迁移（正常流程见 BountyAndAccountTest）。 */
class BountyValidationTest {

    private static final Instant NOW = Instant.parse("2026-09-01T08:00:00Z");
    private final Actor teacher = new Actor(5, "王老师", Role.TEACHER);

    private Bounty openBounty(int points, int days) {
        return Bounty.open(1, true, teacher, true, points, days, NOW);
    }

    @Test
    void rejectsClosedOrDeletedQuestion() {
        assertThatThrownBy(() -> Bounty.open(1, false, teacher, true, 50, 3, NOW))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-1011"));
        assertThatThrownBy(() -> Bounty.open(0, true, teacher, true, 50, 3, NOW))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-1011"));
    }

    @Test
    void rejectsPointsOutsideAllowedRange() {
        assertThatThrownBy(() -> openBounty(Bounty.MIN_POINTS - 1, 3))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-1003"));
        assertThatThrownBy(() -> openBounty(Bounty.MAX_POINTS + 1, 3))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-1003"));
    }

    @Test
    void rejectsDaysOutsideAllowedRange() {
        assertThatThrownBy(() -> openBounty(50, 0))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-1004"));
        assertThatThrownBy(() -> openBounty(50, Bounty.MAX_DAYS + 1))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-1004"));
    }

    @Test
    void rejectsMissingTimestampAndNonManager() {
        assertThatThrownBy(() -> Bounty.open(1, true, teacher, true, 50, 3, null))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-1005"));

        Actor student = new Actor(6, "小明", Role.STUDENT);
        assertThatThrownBy(() -> Bounty.open(1, true, student, true, 50, 3, NOW))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-3001"));
    }

    @Test
    void awardRejectsIllegalAnswerWinnerOrTime() {
        Bounty bounty = openBounty(50, 3);

        assertThatThrownBy(() -> bounty.award(0, 7, NOW))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-1006"));
        assertThatThrownBy(() -> bounty.award(1, 0, NOW))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-1006"));
        assertThatThrownBy(() -> bounty.award(1, 7, null))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-1006"));
    }

    @Test
    void dueBountyCannotBeSettled() {
        Bounty bounty = openBounty(50, 1);
        Instant afterDeadline = NOW.plusSeconds(24 * 60 * 60);

        assertThat(bounty.isDue(afterDeadline)).isTrue();
        assertThatThrownBy(() -> bounty.award(1, 7, afterDeadline))
                .isInstanceOfSatisfying(ConflictException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-2002"));
    }

    @Test
    void closeMarksBountyExpiredAndCannotBeRepeated() {
        Bounty bounty = openBounty(50, 1);

        bounty.close(NOW);

        assertThat(bounty.status()).isEqualTo(BountyStatus.EXPIRED);
        assertThat(bounty.closedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> bounty.close(NOW))
                .isInstanceOfSatisfying(ConflictException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-2003"));
        assertThatThrownBy(() -> bounty.award(1, 7, NOW))
                .isInstanceOfSatisfying(ConflictException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-2003"));
    }

    @Test
    void closeRejectsMissingTimestamp() {
        Bounty bounty = openBounty(50, 1);

        assertThatThrownBy(() -> bounty.close(null))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-1005"));
        assertThat(bounty.status()).isEqualTo(BountyStatus.OPEN);
    }

    @Test
    void reconstituteKeepsPersistedStateAndReadsAllFields() {
        Instant expiresAt = NOW.plusSeconds(86400);
        Bounty bounty = Bounty.reconstitute(4L, 1, 5, 50, BountyStatus.AWARDED, NOW, expiresAt, 7L, 2L, NOW);

        assertThat(bounty.id()).isEqualTo(4);
        assertThat(bounty.questionId()).isEqualTo(1);
        assertThat(bounty.sponsorId()).isEqualTo(5);
        assertThat(bounty.points()).isEqualTo(50);
        assertThat(bounty.status()).isEqualTo(BountyStatus.AWARDED);
        assertThat(bounty.createdAt()).isEqualTo(NOW);
        assertThat(bounty.expiresAt()).isEqualTo(expiresAt);
        assertThat(bounty.winnerId()).isEqualTo(7);
        assertThat(bounty.answerId()).isEqualTo(2);
        assertThat(bounty.closedAt()).isEqualTo(NOW);
    }

    @Test
    void assignIdRejectsDuplicateAndNonPositiveValues() {
        Bounty bounty = openBounty(50, 3);

        assertThatThrownBy(() -> bounty.assignId(0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(bounty.id()).isNull();

        bounty.assignId(9);
        assertThat(bounty.id()).isEqualTo(9);
        assertThatThrownBy(() -> bounty.assignId(10))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("已分配");
    }
}
