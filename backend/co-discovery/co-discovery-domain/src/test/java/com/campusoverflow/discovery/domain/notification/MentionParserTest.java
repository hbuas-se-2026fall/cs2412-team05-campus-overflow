package com.campusoverflow.discovery.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** @提及解析的边界：空文本不得抛异常，且结果集不可变。 */
class MentionParserTest {

    @Test
    void blankTextYieldsNoMentions() {
        assertThat(MentionParser.extract(null)).isEmpty();
        assertThat(MentionParser.extract("   ")).isEmpty();
    }

    @Test
    void loneAtSignAndTooShortNameAreNotMentions() {
        assertThat(MentionParser.extract("@ 你好")).isEmpty();
        assertThat(MentionParser.extract("邮箱是 a@b.cn")).isEmpty();
    }
}
