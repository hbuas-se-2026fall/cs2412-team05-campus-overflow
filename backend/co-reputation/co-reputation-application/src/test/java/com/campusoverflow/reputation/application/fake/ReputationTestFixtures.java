package com.campusoverflow.reputation.application.fake;

import com.campusoverflow.identity.api.CourseSummary;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.reputation.application.LeaderboardPeriod;
import com.campusoverflow.reputation.application.LeaderboardStore;
import com.campusoverflow.reputation.domain.BadgeAward;
import com.campusoverflow.reputation.domain.BadgeRepository;
import com.campusoverflow.reputation.domain.BadgeType;
import com.campusoverflow.reputation.domain.LedgerEntry;
import com.campusoverflow.reputation.domain.LedgerRepository;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.event.IntegrationEvent;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.event.ProcessedEventStore;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 声誉应用层测试替身。
 *
 * <p>用记录式 Fake 而不是 Mockito 深桩，是为了让断言落在「服务对外部协作者做了什么」上
 * （写了哪些流水、发了哪些事件、授了哪些徽章），而不是落在调用顺序上——
 * 调用顺序一改就红、但行为其实没变的测试，维护成本远高于它挡住的回归。</p>
 */
public final class ReputationTestFixtures {

    private ReputationTestFixtures() {
    }

    /** 账户仓储。保存的是同一个实例（{@link ReputationAccount} 是可变对象，服务就地 apply）。 */
    public static final class InMemoryAccounts implements ReputationAccountRepository {

        private final Map<Long, ReputationAccount> byUser = new LinkedHashMap<>();
        private int saveCalls;

        public InMemoryAccounts put(ReputationAccount account) {
            byUser.put(account.userId(), account);
            return this;
        }

        public int saveCalls() {
            return saveCalls;
        }

        @Override
        public Optional<ReputationAccount> findByUserId(long userId) {
            return Optional.ofNullable(byUser.get(userId));
        }

        @Override
        public List<ReputationAccount> findAllByUserId(Collection<Long> userIds) {
            return userIds.stream().map(byUser::get).filter(Objects::nonNull).toList();
        }

        @Override
        public List<ReputationAccount> topByReputation(int limit) {
            return byUser.values().stream()
                    .sorted(Comparator.comparingInt(ReputationAccount::reputation).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<ReputationAccount> findAll() {
            return new ArrayList<>(byUser.values());
        }

        @Override
        public ReputationAccount save(ReputationAccount account) {
            byUser.put(account.userId(), account);
            saveCalls++;
            return account;
        }
    }

    /** 只追加的账本。重复判定按 (eventId, userId, reason) 三元组，与表上的唯一键一致。 */
    public static final class InMemoryLedger implements LedgerRepository {

        private final List<LedgerEntry> entries = new ArrayList<>();
        private long nextId = 1;

        public InMemoryLedger append(long userId, ReputationReason reason, int delta, Instant at) {
            append(LedgerEntry.of(userId, "seed-" + nextId, reason, delta, "SEED", 0, at));
            return this;
        }

        public List<LedgerEntry> entries() {
            return List.copyOf(entries);
        }

        public LedgerEntry only() {
            if (entries.size() != 1) {
                throw new AssertionError("期望恰好 1 条流水，实际 " + entries.size());
            }
            return entries.get(0);
        }

        @Override
        public boolean exists(String eventId, long userId, ReputationReason reason) {
            return entries.stream().anyMatch(e -> e.eventId().equals(eventId)
                    && e.userId() == userId && e.reason() == reason);
        }

        @Override
        public void append(LedgerEntry entry) {
            entries.add(new LedgerEntry(nextId++, entry.userId(), entry.eventId(), entry.reason(), entry.delta(),
                    entry.refType(), entry.refId(), entry.createdAt()));
        }

        @Override
        public PageResult<LedgerEntry> findByUser(long userId, PageRequest page) {
            List<LedgerEntry> mine = entries.stream().filter(e -> e.userId() == userId).toList();
            int from = Math.min(page.offset(), mine.size());
            int to = Math.min(from + page.size(), mine.size());
            return new PageResult<>(mine.subList(from, to), mine.size(), page.page(), page.size());
        }

        @Override
        public long countByUserAndReason(long userId, ReputationReason reason) {
            return entries.stream().filter(e -> e.userId() == userId && e.reason() == reason).count();
        }

        @Override
        public int sumPositiveVoteDeltaSince(long userId, Instant since) {
            return entries.stream()
                    .filter(e -> e.userId() == userId && e.reason().isVote() && e.delta() > 0
                            && !e.createdAt().isBefore(since))
                    .mapToInt(LedgerEntry::delta)
                    .sum();
        }

        @Override
        public Map<Long, Long> sumByUser() {
            Map<Long, Long> sums = new HashMap<>();
            for (LedgerEntry e : entries) {
                sums.merge(e.userId(), (long) e.delta(), Long::sum);
            }
            return sums;
        }
    }

    /** 徽章仓储。{@code has} 是幂等的唯一依据。 */
    public static final class InMemoryBadges implements BadgeRepository {

        private final List<BadgeAward> awards = new ArrayList<>();

        public Set<BadgeType> typesOf(long userId) {
            Set<BadgeType> types = new HashSet<>();
            awards.stream().filter(a -> a.userId() == userId).forEach(a -> types.add(a.type()));
            return types;
        }

        public int awardCount() {
            return awards.size();
        }

        @Override
        public boolean has(long userId, BadgeType type) {
            return awards.stream().anyMatch(a -> a.userId() == userId && a.type() == type);
        }

        @Override
        public void award(BadgeAward award) {
            awards.add(award);
        }

        @Override
        public List<BadgeAward> findByUser(long userId) {
            return awards.stream().filter(a -> a.userId() == userId).toList();
        }
    }

    /** 排行榜存储。记录增量调用，并按预设顺序返回榜单。 */
    public static final class InMemoryLeaderboard implements LeaderboardStore {

        private final List<String> increments = new ArrayList<>();
        private List<Entry> preset = List.of();

        public InMemoryLeaderboard preset(Entry... entries) {
            preset = List.of(entries);
            return this;
        }

        public List<String> increments() {
            return List.copyOf(increments);
        }

        @Override
        public void increment(long userId, int delta, Instant at) {
            increments.add(userId + ":" + delta);
        }

        @Override
        public List<Entry> top(LeaderboardPeriod period, Instant at, int limit) {
            return preset.stream().limit(limit).toList();
        }
    }

    /** 身份上下文 OHS。未登记的用户返回 empty，模拟「已注销」。 */
    public static final class StubIdentity implements IdentityApi {

        private final Map<Long, UserSummary> users = new HashMap<>();

        public StubIdentity put(UserSummary summary) {
            users.put(summary.id(), summary);
            return this;
        }

        @Override
        public Optional<UserSummary> findUser(long userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public Map<Long, UserSummary> findUsers(Collection<Long> userIds) {
            Map<Long, UserSummary> found = new LinkedHashMap<>();
            userIds.forEach(id -> {
                if (users.containsKey(id)) {
                    found.put(id, users.get(id));
                }
            });
            return found;
        }

        @Override
        public Optional<UserSummary> findByDisplayName(String displayName) {
            return users.values().stream().filter(u -> u.displayName().equals(displayName)).findFirst();
        }

        @Override
        public Optional<CourseSummary> findCourse(long courseId) {
            return Optional.empty();
        }

        @Override
        public boolean canManageCourse(long userId, Long courseId) {
            return false;
        }
    }

    /** 事件发布端口。集成事件在真实实现里进发件箱，这里只记录。 */
    public static final class RecordingPublisher implements IntegrationEventPublisher {

        private final List<IntegrationEvent> events = new ArrayList<>();

        public List<IntegrationEvent> events() {
            return List.copyOf(events);
        }

        public IntegrationEvent only() {
            if (events.size() != 1) {
                throw new AssertionError("期望恰好 1 个事件，实际 " + events.size());
            }
            return events.get(0);
        }

        /** 按类型过滤：一次用例往往同时产出声誉变更、徽章、悬赏等多类事件。 */
        public <T extends IntegrationEvent> List<T> eventsOf(Class<T> type) {
            return events.stream().filter(type::isInstance).map(type::cast).toList();
        }

        @Override
        public void publish(IntegrationEvent event) {
            events.add(event);
        }
    }

    /** 消费端幂等守卫。同 (consumer, eventId) 第二次返回 false。 */
    public static final class InMemoryProcessedEvents implements ProcessedEventStore {

        private final Set<String> keys = new HashSet<>();

        @Override
        public boolean markProcessed(String eventId, String consumer) {
            return keys.add(consumer + ":" + eventId);
        }
    }
}
