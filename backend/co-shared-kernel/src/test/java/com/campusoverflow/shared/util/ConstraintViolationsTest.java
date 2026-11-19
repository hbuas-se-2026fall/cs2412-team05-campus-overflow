package com.campusoverflow.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConstraintViolationsTest {

    @Test
    void joinsMessagesOfWholeCauseChain() {
        IllegalArgumentException root = new IllegalArgumentException("duplicate key: uk_question_open");
        IllegalStateException middle = new IllegalStateException("could not execute statement", root);
        RuntimeException top = new RuntimeException("data integrity violation", middle);

        String messages = ConstraintViolations.messagesOf(top);

        assertThat(messages).contains("data integrity violation")
                .contains("could not execute statement")
                .contains("uk_question_open");
    }

    @Test
    void skipsNullMessages() {
        RuntimeException top = new RuntimeException("outer", new IllegalStateException());

        String messages = ConstraintViolations.messagesOf(top);

        assertThat(messages.lines()).containsExactly("outer");
    }

    @Test
    void returnsEmptyStringForNullThrowable() {
        assertThat(ConstraintViolations.messagesOf(null)).isEmpty();
    }

    @Test
    void stopsAppendingOnceBufferIsLarge() {
        // 每层 2048 字符，两层后超过 4096，循环必须在下一层前停止，避免日志无界膨胀
        RuntimeException inner = new RuntimeException("x".repeat(2048));
        RuntimeException middle = new RuntimeException("y".repeat(2048), inner);
        RuntimeException top = new RuntimeException("z".repeat(2048), middle);

        String messages = ConstraintViolations.messagesOf(top);

        assertThat(messages).hasSizeLessThan(6144);
        assertThat(messages).doesNotContain("x".repeat(2048));
    }
}
