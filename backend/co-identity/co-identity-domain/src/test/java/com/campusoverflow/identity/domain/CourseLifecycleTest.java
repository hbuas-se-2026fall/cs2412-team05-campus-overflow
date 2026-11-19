package com.campusoverflow.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 课程聚合的代码校验、重建与主键回填，以及课程内角色的管理权限。 */
class CourseLifecycleTest {

    private static final Instant NOW = Instant.parse("2026-09-01T08:00:00Z");
    private final Actor teacher = new Actor(5, "王老师", Role.TEACHER);

    @Test
    void rejectsMalformedCourseCode() {
        assertThatThrownBy(() -> Course.create(teacher, "D", "数据结构", "2026-秋", NOW))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-1010"));
        assertThatThrownBy(() -> Course.create(teacher, "数据结构课程代码", "数据结构", "2026-秋", NOW))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-1010"));
    }

    @Test
    void rejectsTooShortNameAndTerm() {
        assertThatThrownBy(() -> Course.create(teacher, "DS-101", "数", "2026-秋", NOW))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-1011"));
        assertThatThrownBy(() -> Course.create(teacher, "DS-101", "数据结构", "秋", NOW))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-1012"));
    }

    @Test
    void reconstituteKeepsPersistedValuesAndAssignIdIsOneShot() {
        Course course = Course.reconstitute(3L, "DS-101", "数据结构", "2026-秋", 5L, NOW);

        assertThat(course.id()).isEqualTo(3);
        assertThat(course.code()).isEqualTo("DS-101");
        assertThat(course.name()).isEqualTo("数据结构");
        assertThat(course.term()).isEqualTo("2026-秋");
        assertThat(course.teacherId()).isEqualTo(5);
        assertThat(course.createdAt()).isEqualTo(NOW);

        assertThatThrownBy(() -> course.assignId(4))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("已分配");
    }

    @Test
    void newCourseAcceptsIdAssignment() {
        Course course = Course.create(teacher, "ds-101", "数据结构", "2026-秋", NOW);
        assertThat(course.id()).isNull();

        course.assignId(11);

        assertThat(course.id()).isEqualTo(11);
    }

    @Test
    void onlyTaAndTeacherCanManageCourse() {
        assertThat(CourseRole.STUDENT.canManage()).isFalse();
        assertThat(CourseRole.TA.canManage()).isTrue();
        assertThat(CourseRole.TEACHER.canManage()).isTrue();
    }

    @Test
    void reconstituteKeepsMembershipStateAndReadsAllFields() {
        CourseMember member = CourseMember.reconstitute(3L, 8L, CourseRole.TA, CourseMemberStatus.APPROVED, NOW);

        assertThat(member.courseId()).isEqualTo(3);
        assertThat(member.userId()).isEqualTo(8);
        assertThat(member.role()).isEqualTo(CourseRole.TA);
        assertThat(member.status()).isEqualTo(CourseMemberStatus.APPROVED);
        assertThat(member.joinedAt()).isEqualTo(NOW);
        assertThat(member.isApproved()).isTrue();
    }

    @Test
    void onlyRejectedApplicationCanBeResubmitted() {
        CourseMember approved = CourseMember.student(1, 2, NOW);

        assertThatThrownBy(approved::reapply)
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-1014"));
    }
}
