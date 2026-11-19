package com.campusoverflow.reputation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

/** 声誉账户的建账校验、重建与积分操作的下界保护。 */
class ReputationAccountLifecycleTest {

    @Test
    void openRejectsIllegalUserAndInitialPoints() {
        assertThatThrownBy(() -> ReputationAccount.open(0, 0))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-1001"));
        assertThatThrownBy(() -> ReputationAccount.open(1, -1))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-1001"));
    }

    @Test
    void openStartsWithoutReputationAndKeepsInitialPointsAvailable() {
        ReputationAccount account = ReputationAccount.open(2, 100);

        assertThat(account.userId()).isEqualTo(2);
        assertThat(account.reputation()).isZero();
        assertThat(account.availablePoints()).isEqualTo(100);
        assertThat(account.frozenPoints()).isZero();
        assertThat(account.version()).isZero();
    }

    @Test
    void reconstituteKeepsPersistedSnapshot() {
        ReputationAccount account = ReputationAccount.reconstitute(3, 120, 40, 60, 7);

        assertThat(account.userId()).isEqualTo(3);
        assertThat(account.reputation()).isEqualTo(120);
        assertThat(account.availablePoints()).isEqualTo(40);
        assertThat(account.frozenPoints()).isEqualTo(60);
        assertThat(account.version()).isEqualTo(7);
    }

    @Test
    void applyReputationAccumulatesAndRejectsOverflow() {
        ReputationAccount account = ReputationAccount.open(1, 0);

        account.applyReputation(10);
        account.applyReputation(-3);
        assertThat(account.reputation()).isEqualTo(7);

        assertThatThrownBy(() -> account.applyReputation(-8))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-1008"));
        assertThat(account.reputation()).isEqualTo(7);

        ReputationAccount rich = ReputationAccount.reconstitute(2, Integer.MAX_VALUE, 0, 0, 0);
        assertThatThrownBy(() -> rich.applyReputation(1))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-1008"));
    }

    @Test
    void pointOperationsRejectNonPositiveAmounts() {
        ReputationAccount account = ReputationAccount.open(1, 100);

        assertThatThrownBy(() -> account.freezePoints(0))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-1010"));
        assertThatThrownBy(() -> account.spendFrozen(-1))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-1010"));
        assertThatThrownBy(() -> account.refundFrozen(0))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-1010"));
    }

    @Test
    void spendingMoreThanFrozenIsRejected() {
        ReputationAccount account = ReputationAccount.open(1, 100);
        account.freezePoints(30);

        assertThatThrownBy(() -> account.spendFrozen(31))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-1009"));
        assertThat(account.frozenPoints()).isEqualTo(30);
        assertThat(account.availablePoints()).isEqualTo(70);
    }
}
