package com.campusoverflow.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MaskingTest {

    @Test
    void masksAccountNumberKeepingPrefixAndSuffix() {
        assertThat(Masking.account("2023001217")).isEqualTo("2023****17");
        assertThat(Masking.account("123")).isEqualTo("****");
    }

    @Test
    void masksEmailLocalPart() {
        assertThat(Masking.email("alice@campus.edu")).isEqualTo("a***@campus.edu");
    }

    @Test
    void returnsFullMaskForNullOrTooShortAccount() {
        assertThat(Masking.account(null)).isEqualTo("****");
        assertThat(Masking.account("123456")).isEqualTo("****");
    }

    @Test
    void returnsGenericMaskWhenEmailIsMissingOrMalformed() {
        assertThat(Masking.email(null)).isEqualTo("***");
        assertThat(Masking.email("not-an-email")).isEqualTo("***");
    }
}
