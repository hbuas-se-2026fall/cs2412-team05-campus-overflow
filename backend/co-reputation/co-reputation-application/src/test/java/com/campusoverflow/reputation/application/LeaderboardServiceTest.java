package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.reputation.api.event.ReputationChangedEvent;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryAccounts;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryLeaderboard;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryProcessedEvents;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.StubIdentity;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 排行榜：增量入 ZSET 要幂等，读取要能容忍「用户已注销」。 */
class LeaderboardServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-12T02:00:00Z");

    private final InMemoryLeaderboard store = new InMemoryLeaderboard();
    private final InMemoryAccounts accounts = new InMemoryAccounts();
    private final StubIdentity identity = new StubIdentity()
            .put(new UserSummary(2, "小明", Role.STUDENT, true, "计算机工程学院"))
            .put(new UserSummary(3, "小红", Role.STUDENT, false, "计算机工程学院"));
    private final InMemoryProcessedEvents processed = new InMemoryProcessedEvents();
    private final LeaderboardService service = new LeaderboardService(store, accounts, identity, processed,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("声誉变化事件：同一条事件只入榜一次")
    void reputationChangedIncrementsOncePerEvent() {
        ReputationChangedEvent event = new ReputationChangedEvent("e1", NOW, 2, 15, 15, "ANSWER_ACCEPTED");

        service.on(event);
        service.on(event);

        assertThat(store.increments()).containsExactly("2:15");
    }

    @Test
    @DisplayName("limit 非正数：直接返回空表，不去查账户和身份")
    void nonPositiveLimitReturnsEmpty() {
        accounts.put(ReputationAccount.reconstitute(2, 50, 0, 0, 0));

        assertThat(service.top(LeaderboardPeriod.ALL, 0)).isEmpty();
        assertThat(service.top(LeaderboardPeriod.ALL, -1)).isEmpty();
    }

    @Test
    @DisplayName("总榜：按声誉倒序编号，身份缺失的用户显示为「已注销用户」")
    void allTimeReadsAccountsAndToleratesMissingUser() {
        accounts.put(ReputationAccount.reconstitute(2, 50, 0, 0, 0))
                .put(ReputationAccount.reconstitute(3, 80, 0, 0, 0))
                .put(ReputationAccount.reconstitute(4, 10, 0, 0, 0));

        List<LeaderboardEntryView> top = service.top(LeaderboardPeriod.ALL, 10);

        assertThat(top).extracting(LeaderboardEntryView::rank).containsExactly(1, 2, 3);
        assertThat(top).extracting(LeaderboardEntryView::userId).containsExactly(3L, 2L, 4L);
        assertThat(top).extracting(LeaderboardEntryView::score).containsExactly(80L, 50L, 10L);
        assertThat(top.get(0).displayName()).isEqualTo("小红");
        assertThat(top.get(0).verified()).isFalse();
        assertThat(top.get(1).displayName()).isEqualTo("小明");
        assertThat(top.get(1).verified()).isTrue();
        assertThat(top.get(2).displayName()).isEqualTo("已注销用户");
        assertThat(top.get(2).verified()).isFalse();
    }

    @Test
    @DisplayName("周榜：走 ZSET 存储，编号同样从 1 开始")
    void weeklyReadsFromStore() {
        store.preset(new LeaderboardStore.Entry(2, 40), new LeaderboardStore.Entry(3, 20));

        List<LeaderboardEntryView> top = service.top(LeaderboardPeriod.WEEK, 2);

        assertThat(top).extracting(LeaderboardEntryView::rank).containsExactly(1, 2);
        assertThat(top).extracting(LeaderboardEntryView::displayName).containsExactly("小明", "小红");
        assertThat(top).extracting(LeaderboardEntryView::score).containsExactly(40L, 20L);
    }
}
