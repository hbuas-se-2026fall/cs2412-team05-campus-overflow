package com.campusoverflow.shared.page;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PageRequestTest {

    @Test
    void clampsPageToAtLeastOne() {
        assertThat(new PageRequest(0, 10).page()).isEqualTo(1);
        assertThat(new PageRequest(-5, 10).page()).isEqualTo(1);
        assertThat(new PageRequest(3, 10).page()).isEqualTo(3);
    }

    @Test
    void clampsSizeIntoRange() {
        assertThat(new PageRequest(1, 0).size()).isEqualTo(1);
        assertThat(new PageRequest(1, -1).size()).isEqualTo(1);
        assertThat(new PageRequest(1, 10).size()).isEqualTo(10);
        assertThat(new PageRequest(1, 51).size()).isEqualTo(PageRequest.MAX_SIZE);
    }

    @Test
    void computesOffsetFromClampedValues() {
        assertThat(new PageRequest(1, 10).offset()).isZero();
        assertThat(new PageRequest(3, 10).offset()).isEqualTo(20);
        // page 被夹到 1 后 offset 不能为负
        assertThat(new PageRequest(0, 10).offset()).isZero();
    }
}
