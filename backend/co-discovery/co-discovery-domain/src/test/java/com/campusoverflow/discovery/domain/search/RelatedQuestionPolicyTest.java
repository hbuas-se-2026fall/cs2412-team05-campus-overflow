package com.campusoverflow.discovery.domain.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.discovery.domain.search.SearchIndex.RelatedCandidate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 相关问题的相似度计算与排序边界。 */
class RelatedQuestionPolicyTest {

    @Test
    void jaccardIsSharedOverUnion() {
        assertThat(RelatedQuestionPolicy.jaccard(2, 4, 4)).isEqualTo(2.0 / 6.0);
        assertThat(RelatedQuestionPolicy.jaccard(3, 3, 3)).isEqualTo(1.0);
    }

    @Test
    void jaccardOfEmptySetsIsZero() {
        assertThat(RelatedQuestionPolicy.jaccard(0, 0, 0)).isZero();
    }

    @Test
    void jaccardRejectsImpossibleCounts() {
        assertThatThrownBy(() -> RelatedQuestionPolicy.jaccard(5, 4, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RelatedQuestionPolicy.jaccard(-1, 4, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RelatedQuestionPolicy.jaccard(1, 2, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rankReturnsEmptyForMissingInputOrZeroLimit() {
        assertThat(RelatedQuestionPolicy.rank(null, 3, 5)).isEmpty();
        assertThat(RelatedQuestionPolicy.rank(List.of(), 3, 5)).isEmpty();
        assertThat(RelatedQuestionPolicy.rank(List.of(new RelatedCandidate(2, "二分查找", 2, 4, 0, 0, false)), 3, 0))
                .isEmpty();
    }

    @Test
    void rankDropsCandidatesWithoutSharedTagsOrNull() {
        List<RelatedCandidate> candidates = new ArrayList<>();
        candidates.add(null);
        candidates.add(new RelatedCandidate(2, "无关问题", 0, 4, 9, 9, true));
        candidates.add(new RelatedCandidate(3, "红黑树旋转", 2, 4, 0, 0, false));

        List<RelatedCandidate> ranked = RelatedQuestionPolicy.rank(candidates, 3, 5);

        assertThat(ranked).extracting(RelatedCandidate::questionId).containsExactly(3L);
    }

    @Test
    void rankOrdersBySimilarityThenAcceptedThenScore() {
        List<RelatedCandidate> candidates = List.of(
                new RelatedCandidate(1, "低相似", 1, 5, 0, 0, false),
                new RelatedCandidate(2, "同分未采纳", 2, 4, 0, 0, false),
                new RelatedCandidate(3, "同分已采纳", 2, 4, 0, 0, true));

        List<RelatedCandidate> ranked = RelatedQuestionPolicy.rank(candidates, 4, 2);

        assertThat(ranked).extracting(RelatedCandidate::questionId).containsExactly(3L, 2L);
    }
}
