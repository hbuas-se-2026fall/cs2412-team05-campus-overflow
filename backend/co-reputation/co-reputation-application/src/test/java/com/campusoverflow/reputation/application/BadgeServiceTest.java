package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryBadges;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.RecordingPublisher;
import com.campusoverflow.reputation.api.event.BadgeAwardedEvent;
import com.campusoverflow.reputation.domain.BadgeType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 徽章授予的幂等语义：同一用户同一徽章只落库一次、只发一次事件。 */
class BadgeServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-12T02:00:00Z");

    private final InMemoryBadges badges = new InMemoryBadges();
    private final RecordingPublisher events = new RecordingPublisher();
    private final BadgeService service = new BadgeService(badges, events, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("首次授予：落库并发布 BadgeAwarded，事件带徽章代码与显示名")
    void grantAwardsOnceAndPublishesEvent() {
        service.grant(2, BadgeType.FIRST_ANSWER);

        assertThat(badges.typesOf(2)).containsExactly(BadgeType.FIRST_ANSWER);
        BadgeAwardedEvent event = (BadgeAwardedEvent) events.only();
        assertThat(event.userId()).isEqualTo(2);
        assertThat(event.badgeCode()).isEqualTo("FIRST_ANSWER");
        assertThat(event.badgeName()).isEqualTo(BadgeType.FIRST_ANSWER.displayName());
        assertThat(event.occurredAt()).isEqualTo(NOW);
        assertThat(event.eventId()).isNotBlank();
    }

    @Test
    @DisplayName("重复授予：has 命中后直接返回，不落库也不发事件")
    void secondGrantIsSilentlyIgnored() {
        service.grant(2, BadgeType.FIRST_ANSWER);
        service.grant(2, BadgeType.FIRST_ANSWER);

        assertThat(badges.awardCount()).isEqualTo(1);
        assertThat(events.events()).hasSize(1);
    }

    @Test
    @DisplayName("不同徽章互不影响：同一用户可同时持有多个")
    void differentBadgesAreIndependent() {
        service.grant(2, BadgeType.FIRST_ANSWER);
        service.grant(2, BadgeType.TEACHER_ENDORSED);

        assertThat(badges.typesOf(2)).containsExactlyInAnyOrder(BadgeType.FIRST_ANSWER, BadgeType.TEACHER_ENDORSED);
        assertThat(events.events()).hasSize(2);
    }
}
