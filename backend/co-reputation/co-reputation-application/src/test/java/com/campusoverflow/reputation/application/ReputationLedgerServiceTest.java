package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.reputation.api.event.ReputationChangedEvent;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryAccounts;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryBadges;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryLedger;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.RecordingPublisher;
import com.campusoverflow.reputation.domain.BadgeType;
import com.campusoverflow.reputation.domain.LedgerEntry;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.domain.BusinessRuleException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 记账核心：幂等去重、余额与流水同源、徽章联动、越界回滚。 */
class ReputationLedgerServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-12T02:00:00Z");
    private static final String EVENT = "evt-1";

    private final InMemoryAccounts accounts = new InMemoryAccounts();
    private final InMemoryLedger ledger = new InMemoryLedger();
    private final InMemoryBadges badges = new InMemoryBadges();
    private final RecordingPublisher events = new RecordingPublisher();
    private final BadgeService badgeService = new BadgeService(badges, events, Clock.fixed(NOW, ZoneOffset.UTC));
    private final ReputationLedgerService service = new ReputationLedgerService(accounts, ledger, badgeService, events,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("正常记账：账户不存在则开立，余额、流水、事件三者一致")
    void recordOpensAccountAndKeepsThreeSourcesInSync() {
        boolean recorded = service.record(2, EVENT, ReputationReason.ANSWER_ACCEPTED, 15, "ANSWER", 77);

        assertThat(recorded).isTrue();
        assertThat(accounts.findByUserId(2)).get()
                .extracting(ReputationAccount::reputation).isEqualTo(15);
        LedgerEntry entry = ledger.only();
        assertThat(entry.userId()).isEqualTo(2);
        assertThat(entry.eventId()).isEqualTo(EVENT);
        assertThat(entry.reason()).isEqualTo(ReputationReason.ANSWER_ACCEPTED);
        assertThat(entry.delta()).isEqualTo(15);
        assertThat(entry.refType()).isEqualTo("ANSWER");
        assertThat(entry.refId()).isEqualTo(77);
        assertThat(entry.createdAt()).isEqualTo(NOW);

        ReputationChangedEvent event = (ReputationChangedEvent) events.only();
        assertThat(event.userId()).isEqualTo(2);
        assertThat(event.delta()).isEqualTo(15);
        assertThat(event.reputation()).isEqualTo(15);
        assertThat(event.reason()).isEqualTo("ANSWER_ACCEPTED");
    }

    @Test
    @DisplayName("增量为 0：直接忽略，不写流水不发事件")
    void zeroDeltaIsIgnored() {
        boolean recorded = service.record(2, EVENT, ReputationReason.ANSWER_ACCEPTED, 0, "ANSWER", 77);

        assertThat(recorded).isFalse();
        assertThat(ledger.entries()).isEmpty();
        assertThat(events.events()).isEmpty();
        assertThat(accounts.saveCalls()).isZero();
    }

    @Test
    @DisplayName("同一事件重复投递：按 (eventId, userId, reason) 去重，只记一次")
    void duplicateEventIsIgnored() {
        service.record(2, EVENT, ReputationReason.ANSWER_ACCEPTED, 15, "ANSWER", 77);
        boolean second = service.record(2, EVENT, ReputationReason.ANSWER_ACCEPTED, 15, "ANSWER", 77);

        assertThat(second).isFalse();
        assertThat(ledger.entries()).hasSize(1);
        assertThat(accounts.findByUserId(2)).get()
                .extracting(ReputationAccount::reputation).isEqualTo(15);
    }

    @Test
    @DisplayName("跨过 100 分阈值时联动授予「小有名气」")
    void crossingThresholdGrantsBadge() {
        accounts.put(ReputationAccount.reconstitute(2, 95, 0, 0, 0));

        service.record(2, EVENT, ReputationReason.ANSWER_ACCEPTED, 15, "ANSWER", 77);

        assertThat(badges.typesOf(2)).containsExactly(BadgeType.REPUTATION_100);
    }

    @Test
    @DisplayName("扣成负分：抛 REP-1008，且流水与事件都不落")
    void negativeReputationIsRejectedBeforePersisting() {
        accounts.put(ReputationAccount.reconstitute(2, 0, 0, 0, 0));

        assertThatThrownBy(() -> service.record(2, EVENT, ReputationReason.ANSWER_VOTE, -2, "ANSWER", 77))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("REP-1008"));

        assertThat(ledger.entries()).isEmpty();
        assertThat(events.events()).isEmpty();
        assertThat(accounts.saveCalls()).isZero();
    }
}
