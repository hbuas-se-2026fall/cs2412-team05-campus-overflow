package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryAccounts;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryLedger;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationReason;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 适应度函数 FF-8：账户声誉必须等于其流水之和，不等即视为账实不符。 */
class ReconciliationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-12T02:00:00Z");

    private final InMemoryAccounts accounts = new InMemoryAccounts();
    private final InMemoryLedger ledger = new InMemoryLedger();
    private final ReconciliationService service = new ReconciliationService(accounts, ledger);

    @Test
    @DisplayName("账实相符：余额等于流水之和时无差异")
    void reportsNothingWhenBalanced() {
        accounts.put(ReputationAccount.reconstitute(2, 27, 0, 0, 0));
        ledger.append(2, ReputationReason.ANSWER_ACCEPTED, 15, NOW)
                .append(2, ReputationReason.ANSWER_VOTE, 12, NOW);

        assertThat(service.reconcile()).isEmpty();
    }

    @Test
    @DisplayName("账实不符：同时给出账户余额与流水合计，便于定位差多少")
    void reportsMismatchWithBothSides() {
        accounts.put(ReputationAccount.reconstitute(2, 27, 0, 0, 0));
        ledger.append(2, ReputationReason.ANSWER_ACCEPTED, 15, NOW);

        List<ReconciliationService.Mismatch> mismatches = service.reconcile();

        assertThat(mismatches).containsExactly(new ReconciliationService.Mismatch(2, 27, 15));
    }

    @Test
    @DisplayName("账户存在但完全没有流水：按 0 比对，余额非 0 即报差异")
    void accountWithoutLedgerCountsAsZero() {
        accounts.put(ReputationAccount.reconstitute(5, 1000, 1000, 0, 0));

        assertThat(service.reconcile()).containsExactly(new ReconciliationService.Mismatch(5, 1000, 0));
    }
}
