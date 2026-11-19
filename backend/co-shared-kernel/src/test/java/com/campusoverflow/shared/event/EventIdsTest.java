package com.campusoverflow.shared.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventIdsTest {

    @Test
    void generatesParseableUuid() {
        String id = EventIds.next();

        assertThat(UUID.fromString(id)).hasToString(id);
    }

    @Test
    void generatesUniqueIds() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            ids.add(EventIds.next());
        }

        assertThat(ids).hasSize(100);
    }
}
