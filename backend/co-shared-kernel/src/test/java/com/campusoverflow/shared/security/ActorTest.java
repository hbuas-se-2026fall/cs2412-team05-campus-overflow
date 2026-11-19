package com.campusoverflow.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ActorTest {

    @Test
    void adminIsAdminButNotTeacher() {
        Actor admin = new Actor(1, "管理员", Role.ADMIN);
        assertThat(admin.isAdmin()).isTrue();
        assertThat(admin.isTeacherOrAdmin()).isTrue();
    }

    @Test
    void teacherIsNotAdminButCountsAsTeacherOrAdmin() {
        Actor teacher = new Actor(2, "张老师", Role.TEACHER);
        assertThat(teacher.isAdmin()).isFalse();
        assertThat(teacher.isTeacherOrAdmin()).isTrue();
    }

    @Test
    void studentAndTaHaveNoManagementPrivilege() {
        assertThat(new Actor(3, "学生", Role.STUDENT).isTeacherOrAdmin()).isFalse();
        assertThat(new Actor(4, "助教", Role.TA).isTeacherOrAdmin()).isFalse();
        assertThat(new Actor(4, "助教", Role.TA).isAdmin()).isFalse();
    }

    @Test
    void isComparesUserIdNotName() {
        Actor actor = new Actor(7, "朱淦", Role.STUDENT);
        assertThat(actor.is(7)).isTrue();
        assertThat(actor.is(8)).isFalse();
    }
}
