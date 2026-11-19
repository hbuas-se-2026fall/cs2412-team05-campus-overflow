package com.campusoverflow.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campusoverflow.identity.api.CourseSummary;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.identity.domain.Course;
import com.campusoverflow.identity.domain.CourseMember;
import com.campusoverflow.identity.domain.CourseMemberStatus;
import com.campusoverflow.identity.domain.CourseRepository;
import com.campusoverflow.identity.domain.CourseRole;
import com.campusoverflow.identity.domain.User;
import com.campusoverflow.identity.domain.UserRepository;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("身份上下文开放主机服务")
class IdentityApiImplTest {

    private static final Instant NOW = Instant.parse("2026-11-19T02:00:00Z");

    private final UserRepository users = mock(UserRepository.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final IdentityApiImpl api = new IdentityApiImpl(users, courses);

    private static User user(long id, Role role) {
        User u = User.register("20230012" + id, "用户" + id, "u" + id + "@campus.edu", "hash", "计算机学院",
                "软件2101", NOW);
        u.assignId(id);
        if (role != Role.STUDENT) {
            u.changeRole(new Actor(99, "教务", Role.ADMIN), role);
        }
        return u;
    }

    @Test
    @DisplayName("按 ID 查询用户：存在时返回摘要且不含个人信息")
    void findUserReturnsSummary() {
        when(users.findById(2)).thenReturn(Optional.of(user(2, Role.STUDENT)));

        Optional<UserSummary> summary = api.findUser(2);

        assertThat(summary).isPresent();
        assertThat(summary.get().id()).isEqualTo(2);
        assertThat(summary.get().displayName()).isEqualTo("用户2");
        assertThat(summary.get().role()).isEqualTo(Role.STUDENT);
        assertThat(summary.get().verified()).isFalse();
    }

    @Test
    @DisplayName("按 ID 查询用户：不存在时返回空")
    void findUserReturnsEmptyWhenAbsent() {
        when(users.findById(99)).thenReturn(Optional.empty());

        assertThat(api.findUser(99)).isEmpty();
    }

    @Test
    @DisplayName("批量查询：null 与空集合直接返回空 Map，不访问仓储")
    void findUsersShortCircuitsOnEmptyInput() {
        assertThat(api.findUsers(null)).isEmpty();
        assertThat(api.findUsers(List.of())).isEmpty();

        verifyNoInteractions(users);
    }

    @Test
    @DisplayName("批量查询：去重后一次取回，按 ID 建立索引")
    void findUsersDeduplicatesIds() {
        when(users.findAllById(List.of(2L, 3L))).thenReturn(List.of(user(2, Role.STUDENT), user(3, Role.TEACHER)));

        Map<Long, UserSummary> result = api.findUsers(List.of(2L, 3L, 2L, 3L));

        verify(users).findAllById(List.of(2L, 3L));
        assertThat(result).containsOnlyKeys(2L, 3L);
        assertThat(result.get(3L).role()).isEqualTo(Role.TEACHER);
    }

    @Test
    @DisplayName("按昵称查询用户")
    void findByDisplayNameReturnsSummary() {
        when(users.findByDisplayName("用户2")).thenReturn(Optional.of(user(2, Role.STUDENT)));

        assertThat(api.findByDisplayName("用户2")).map(UserSummary::id).contains(2L);
    }

    @Test
    @DisplayName("查询课程摘要")
    void findCourseReturnsSummary() {
        Course course = Course.create(new Actor(1, "王老师", Role.TEACHER), "DS-101", "数据结构", "2026-秋", NOW);
        course.assignId(100);
        when(courses.findById(100)).thenReturn(Optional.of(course));

        Optional<CourseSummary> summary = api.findCourse(100);

        assertThat(summary).isPresent();
        assertThat(summary.get().code()).isEqualTo("DS-101");
        assertThat(summary.get().teacherId()).isEqualTo(1);
    }

    @Test
    @DisplayName("管课权限：管理员一律通过")
    void canManageCourseGrantsAdmin() {
        when(users.findById(1)).thenReturn(Optional.of(user(1, Role.ADMIN)));

        assertThat(api.canManageCourse(1, 100L)).isTrue();
        verify(courses, never()).findMember(anyLong(), anyLong());
    }

    @Test
    @DisplayName("管课权限：用户不存在时拒绝")
    void canManageCourseRejectsUnknownUser() {
        when(users.findById(99)).thenReturn(Optional.empty());

        assertThat(api.canManageCourse(99, 100L)).isFalse();
    }

    @Test
    @DisplayName("管课权限：未关联课程时全局教师可管、学生不可管")
    void canManageCourseWithoutCourseUsesGlobalRole() {
        when(users.findById(3)).thenReturn(Optional.of(user(3, Role.TEACHER)));
        when(users.findById(2)).thenReturn(Optional.of(user(2, Role.STUDENT)));

        assertThat(api.canManageCourse(3, null)).isTrue();
        assertThat(api.canManageCourse(2, null)).isFalse();
    }

    @Test
    @DisplayName("管课权限：课程内助教可管、学生不可管、待审核不可管、非成员不可管")
    void canManageCourseLooksAtCourseMembership() {
        when(users.findById(2)).thenReturn(Optional.of(user(2, Role.STUDENT)));
        when(courses.findMember(100, 2)).thenReturn(Optional.of(
                CourseMember.reconstitute(100, 2, CourseRole.TA, CourseMemberStatus.APPROVED, NOW)));
        when(courses.findMember(101, 2)).thenReturn(Optional.of(
                CourseMember.reconstitute(101, 2, CourseRole.STUDENT, CourseMemberStatus.APPROVED, NOW)));
        when(courses.findMember(102, 2)).thenReturn(Optional.of(
                CourseMember.reconstitute(102, 2, CourseRole.TA, CourseMemberStatus.PENDING, NOW)));
        when(courses.findMember(103, 2)).thenReturn(Optional.empty());

        assertThat(api.canManageCourse(2, 100L)).isTrue();
        assertThat(api.canManageCourse(2, 101L)).isFalse();
        assertThat(api.canManageCourse(2, 102L)).isFalse();
        assertThat(api.canManageCourse(2, 103L)).isFalse();
    }
}
