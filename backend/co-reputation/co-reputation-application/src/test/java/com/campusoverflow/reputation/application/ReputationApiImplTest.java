package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.reputation.api.ReputationSummary;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryAccounts;
import com.campusoverflow.reputation.domain.ReputationAccount;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 对外 OHS：批量查询要能直接喂给下游做展示，缺账户按 0 分处理而不是抛错。 */
class ReputationApiImplTest {

    private final InMemoryAccounts accounts = new InMemoryAccounts()
            .put(ReputationAccount.reconstitute(2, 50, 0, 0, 0))
            .put(ReputationAccount.reconstitute(3, 120, 0, 0, 0));
    private final ReputationApiImpl api = new ReputationApiImpl(accounts);

    @Test
    @DisplayName("单用户摘要：账户缺失时返回 0 分")
    void summaryFallsBackToZero() {
        assertThat(api.summary(2)).isEqualTo(new ReputationSummary(2, 50));
        assertThat(api.summary(99)).isEqualTo(new ReputationSummary(99, 0));
    }

    @Test
    @DisplayName("批量查询：只返回存在的账户，缺失的 key 不出现")
    void reputationsSkipsUnknownUsers() {
        Map<Long, Integer> result = api.reputations(List.of(2L, 3L, 99L));

        assertThat(result).containsOnlyKeys(2L, 3L);
        assertThat(result).containsEntry(2L, 50).containsEntry(3L, 120);
    }
}
