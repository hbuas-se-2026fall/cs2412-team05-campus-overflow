package com.campusoverflow.discovery.domain.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 搜索投影文档与命中记录的不可变性与字段读出口径。 */
class SearchProjectionTest {

    private static final Instant NOW = Instant.parse("2026-09-01T08:00:00Z");

    @Test
    void searchDocumentCopiesTagsAndKeepsAllFields() {
        List<String> tags = new ArrayList<>(List.of("java", "算法"));

        SearchDocument document = new SearchDocument(1L, 3L, 8L, "红黑树旋转", "正文", tags, NOW);
        tags.add("被篡改");

        assertThat(document.questionId()).isEqualTo(1);
        assertThat(document.courseId()).isEqualTo(3);
        assertThat(document.authorId()).isEqualTo(8);
        assertThat(document.title()).isEqualTo("红黑树旋转");
        assertThat(document.bodyText()).isEqualTo("正文");
        assertThat(document.tags()).containsExactly("java", "算法");
        assertThat(document.createdAt()).isEqualTo(NOW);
    }

    @Test
    void searchHitExposesRankingMetadata() {
        SearchHit hit = new SearchHit(1L, "红黑树旋转", "…片段…", List.of("java"), 3L, 8L, 42, 5, true, false, NOW);

        assertThat(hit.questionId()).isEqualTo(1);
        assertThat(hit.snippet()).isEqualTo("…片段…");
        assertThat(hit.tags()).containsExactly("java");
        assertThat(hit.courseId()).isEqualTo(3);
        assertThat(hit.authorId()).isEqualTo(8);
        assertThat(hit.score()).isEqualTo(42);
        assertThat(hit.answerCount()).isEqualTo(5);
        assertThat(hit.accepted()).isTrue();
        assertThat(hit.bountyOpen()).isFalse();
        assertThat(hit.createdAt()).isEqualTo(NOW);
    }
}
