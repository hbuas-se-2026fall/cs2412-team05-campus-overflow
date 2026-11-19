package com.campusoverflow.discovery.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 通知的重建、主键回填与非法入参（创建与已读用例见 DiscoveryDomainTest）。 */
class NotificationLifecycleTest {

    private static final Instant NOW = Instant.parse("2026-09-01T08:00:00Z");

    @Test
    void rejectsNonPositiveRecipient() {
        assertThatThrownBy(() -> Notification.create(0, NotificationType.MENTIONED, "有人提到你", "/q/1", "evt-1", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recipientId");
    }

    @Test
    void rejectsMissingTimestamp() {
        assertThatNullPointerException()
                .isThrownBy(() -> Notification.create(1, NotificationType.MENTIONED, "有人提到你", "/q/1", "evt-1", null));
    }

    @Test
    void reconstituteKeepsReadStateAndAssignIdFillsId() {
        Notification notification = Notification.reconstitute(9L, 2, NotificationType.ANSWER_ACCEPTED,
                "你的回答被采纳", "/q/1", "evt-9", true, NOW);

        assertThat(notification.id()).isEqualTo(9);
        assertThat(notification.recipientId()).isEqualTo(2);
        assertThat(notification.type()).isEqualTo(NotificationType.ANSWER_ACCEPTED);
        assertThat(notification.title()).isEqualTo("你的回答被采纳");
        assertThat(notification.link()).isEqualTo("/q/1");
        assertThat(notification.sourceEventId()).isEqualTo("evt-9");
        assertThat(notification.read()).isTrue();
        assertThat(notification.createdAt()).isEqualTo(NOW);

        Notification fresh = Notification.create(3, NotificationType.BADGE_AWARDED, "获得徽章", null, "evt-10", NOW);
        assertThat(fresh.id()).isNull();
        fresh.assignId(12);
        assertThat(fresh.id()).isEqualTo(12);
        assertThat(fresh.read()).isFalse();
    }

    @Test
    void recipientCanMarkOwnNotificationRead() {
        Notification notification = Notification.create(2, NotificationType.MENTIONED, "有人提到你", "/q/1", "evt-1", NOW);

        notification.markRead(2);

        assertThat(notification.read()).isTrue();
    }
}
