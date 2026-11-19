package com.campusoverflow.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UtcTimeTest {

    @Test
    void convertsInstantToUtcWallClock() {
        Instant instant = Instant.parse("2026-03-01T08:30:00Z");

        assertThat(UtcTime.toDb(instant)).isEqualTo(LocalDateTime.of(2026, 3, 1, 8, 30));
    }

    @Test
    void convertsDbValueBackToInstant() {
        LocalDateTime dbValue = LocalDateTime.of(2026, 3, 1, 8, 30);

        assertThat(UtcTime.fromDb(dbValue)).isEqualTo(Instant.parse("2026-03-01T08:30:00Z"));
    }

    @Test
    void nullSafeInBothDirections() {
        assertThat(UtcTime.toDb(null)).isNull();
        assertThat(UtcTime.fromDb(null)).isNull();
    }

    @Test
    void roundTripIsLossless() {
        Instant instant = Instant.parse("2026-11-19T00:41:49.123456Z");

        assertThat(UtcTime.fromDb(UtcTime.toDb(instant))).isEqualTo(instant);
    }
}
