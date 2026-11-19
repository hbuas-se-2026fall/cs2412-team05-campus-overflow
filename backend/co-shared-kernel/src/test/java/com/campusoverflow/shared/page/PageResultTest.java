package com.campusoverflow.shared.page;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PageResultTest {

    @Test
    void emptyHasNoItemsAndZeroTotal() {
        PageResult<String> result = PageResult.empty(1, 10);
        assertThat(result.items()).isEmpty();
        assertThat(result.total()).isZero();
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalPages()).isZero();
    }

    @Test
    void copiesItemsSoCallerCannotMutate() {
        List<String> source = new ArrayList<>(List.of("a", "b"));
        PageResult<String> result = new PageResult<>(source, 2, 1, 10);
        source.add("c");
        assertThat(result.items()).containsExactly("a", "b");
    }

    @Test
    void mapKeepsPagingMetadata() {
        PageResult<Integer> mapped = new PageResult<>(List.of("a", "bb"), 7, 2, 5)
                .map(String::length);
        assertThat(mapped.items()).containsExactly(1, 2);
        assertThat(mapped.total()).isEqualTo(7);
        assertThat(mapped.page()).isEqualTo(2);
        assertThat(mapped.size()).isEqualTo(5);
    }

    @Test
    void totalPagesRoundsUp() {
        assertThat(new PageResult<>(List.of("a"), 10, 1, 10).totalPages()).isEqualTo(1);
        assertThat(new PageResult<>(List.of("a"), 11, 1, 10).totalPages()).isEqualTo(2);
        assertThat(new PageResult<>(List.of("a"), 1, 1, 10).totalPages()).isEqualTo(1);
    }

    @Test
    void totalPagesIsZeroWhenSizeIsZero() {
        assertThat(new PageResult<>(List.of(), 5, 1, 0).totalPages()).isZero();
    }
}
