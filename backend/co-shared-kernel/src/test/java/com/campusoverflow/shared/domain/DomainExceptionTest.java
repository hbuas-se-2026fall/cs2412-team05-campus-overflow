package com.campusoverflow.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DomainExceptionTest {

    @Test
    void businessRuleExceptionMapsTo422() {
        BusinessRuleException ex = new BusinessRuleException("QA-4001", "标题过短");
        assertThat(ex.code()).isEqualTo("QA-4001");
        assertThat(ex.getMessage()).isEqualTo("标题过短");
        assertThat(ex.suggestedStatus()).isEqualTo(422);
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void conflictExceptionMapsTo409() {
        ConflictException ex = new ConflictException("ID-4090", "重复注册");
        assertThat(ex.code()).isEqualTo("ID-4090");
        assertThat(ex.suggestedStatus()).isEqualTo(409);
    }

    @Test
    void notFoundExceptionMapsTo404() {
        NotFoundException ex = new NotFoundException("QA-4040", "问题不存在");
        assertThat(ex.code()).isEqualTo("QA-4040");
        assertThat(ex.suggestedStatus()).isEqualTo(404);
    }

    @Test
    void forbiddenExceptionMapsTo403() {
        ForbiddenException ex = new ForbiddenException("QA-4030", "无权删除");
        assertThat(ex.code()).isEqualTo("QA-4030");
        assertThat(ex.suggestedStatus()).isEqualTo(403);
    }

    @Test
    void allSubtypesShareDomainExceptionContract() {
        DomainException[] all = {
            new BusinessRuleException("A-1", "1"),
            new ConflictException("A-2", "2"),
            new NotFoundException("A-3", "3"),
            new ForbiddenException("A-4", "4"),
        };
        assertThat(all).allSatisfy(ex -> {
            assertThat(ex.code()).isNotBlank();
            assertThat(ex.suggestedStatus()).isBetween(400, 499);
        });
    }
}
