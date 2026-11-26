package com.campusoverflow.discovery.domain.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.page.PageRequest;
import org.junit.jupiter.api.Test;

/** 搜索条件构造的边界：超长关键词、默认分页、标签归一化与分词上限。 */
class SearchQueryValidationTest {

    @Test
    void rejectsKeywordLongerThanFiftyCodePoints() {
        String tooLong = "a".repeat(51);

        assertThatThrownBy(() -> new SearchQuery(tooLong, null, null, null, false, false, null))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                    assertThat(ex.code()).isEqualTo("DIS-1002");
                    assertThat(ex.getMessage()).contains("50");
                });
    }

    @Test
    void acceptsExactlyFiftyCodePoints() {
        String limit = "a".repeat(50);

        assertThat(new SearchQuery(limit, null, null, null, false, false, null).keyword()).isEqualTo(limit);
    }

    @Test
    void fallsBackToDefaultPaging() {
        SearchQuery query = new SearchQuery("红黑树", null, null, null, false, false, null);

        assertThat(query.page().page()).isEqualTo(1);
        assertThat(query.page().size()).isEqualTo(20);
        assertThat(query.page()).isEqualTo(new PageRequest(1, 20));
    }

    @Test
    void normalizesTagAndTreatsBlankTagAsAbsent() {
        assertThat(new SearchQuery("红黑树", null, "  Java  ", null, false, false, null).tag()).isEqualTo("java");
        assertThat(new SearchQuery("红黑树", null, "   ", null, false, false, null).tag()).isNull();
        assertThat(new SearchQuery("红黑树", null, null, null, false, false, null).tag()).isNull();
    }

    @Test
    void keywordAbsentMeansNoTermsAndNewestSort() {
        SearchQuery query = new SearchQuery(null, null, null, null, false, false, null);

        assertThat(query.terms()).isEmpty();
        assertThat(query.keyword()).isNull();
        assertThat(query.sort()).isEqualTo(SearchSort.NEWEST);
    }

    @Test
    void keepsAtMostFiveTerms() {
        SearchQuery query = new SearchQuery("一一 二二 三三 四四 五五 六六", null, null, null, false, false, null);

        assertThat(query.terms()).containsExactly("一一", "二二", "三三", "四四", "五五");
    }
}
