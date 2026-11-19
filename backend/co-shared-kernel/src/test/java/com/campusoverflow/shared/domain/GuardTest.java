package com.campusoverflow.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GuardTest {

    @Test
    void requireTextReturnsTrimmedValue() {
        assertThat(Guard.requireText("  校园问答  ", 1, 20, "QA-4001", "标题")).isEqualTo("校园问答");
    }

    @Test
    void requireTextCountsCodePointsNotChars() {
        // 4 个码点，但 char 长度为 8（每个 emoji 占两个 char），按字符数计会误判超长
        String emoji = "🎓🎓🎓🎓";
        assertThat(Guard.requireText(emoji, 1, 4, "QA-4001", "标题")).isEqualTo(emoji);
    }

    @Test
    void requireTextRejectsNullAndBlank() {
        assertThatThrownBy(() -> Guard.requireText(null, 1, 20, "QA-4001", "标题"))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                    assertThat(ex.code()).isEqualTo("QA-4001");
                    assertThat(ex.getMessage()).isEqualTo("标题不能为空");
                });
        assertThatThrownBy(() -> Guard.requireText("   ", 1, 20, "QA-4001", "标题"))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("QA-4001"));
    }

    @Test
    void requireTextRejectsTooShortAfterTrim() {
        assertThatThrownBy(() -> Guard.requireText("  a  ", 2, 20, "QA-4002", "标题"))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                    assertThat(ex.code()).isEqualTo("QA-4002");
                    assertThat(ex.getMessage()).contains("长度应在 2–20 个字符之间").contains("当前 1");
                });
    }

    @Test
    void requireTextRejectsTooLong() {
        assertThatThrownBy(() -> Guard.requireText("abcdef", 1, 5, "QA-4002", "标题"))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                    assertThat(ex.code()).isEqualTo("QA-4002");
                    assertThat(ex.getMessage()).contains("当前 6");
                });
    }

    @Test
    void requireTextAcceptsBoundaryLengths() {
        assertThat(Guard.requireText("ab", 2, 5, "QA-4001", "标题")).isEqualTo("ab");
        assertThat(Guard.requireText("abcde", 2, 5, "QA-4001", "标题")).isEqualTo("abcde");
    }

    @Test
    void requireNonNullReturnsValueOrThrows() {
        Object value = new Object();
        assertThat(Guard.requireNonNull(value, "QA-4003", "问题")).isSameAs(value);

        assertThatThrownBy(() -> Guard.requireNonNull(null, "QA-4003", "问题"))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                    assertThat(ex.code()).isEqualTo("QA-4003");
                    assertThat(ex.getMessage()).isEqualTo("问题不能为空");
                });
    }
}
