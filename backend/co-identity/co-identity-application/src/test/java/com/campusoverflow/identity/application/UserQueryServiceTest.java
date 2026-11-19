package com.campusoverflow.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.campusoverflow.identity.domain.CourseRole;
import com.campusoverflow.identity.domain.User;
import com.campusoverflow.identity.domain.UserRepository;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("个人中心查询用例")
class UserQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-19T02:00:00Z");

    private final UserRepository users = mock(UserRepository.class);
    private final CourseService courses = mock(CourseService.class);
    private final UserQueryService service = new UserQueryService(users, courses);

    @Test
    @DisplayName("查询本人视图：字段完整，并带上我的课程")
    void meReturnsProfileWithCourses() {
        User user = User.register("2023001217", "张三", "zhang@campus.edu", "hash", "计算机学院", "软件2101", NOW);
        user.assignId(2);
        when(users.findById(2)).thenReturn(Optional.of(user));
        when(courses.myCourses(2)).thenReturn(List.of(
                new CourseView(100, "DS-101", "数据结构", "2026-秋", 1, "王老师", CourseRole.STUDENT.name())));

        MeView me = service.me(2);

        assertThat(me.id()).isEqualTo(2);
        assertThat(me.username()).isEqualTo("2023001217");
        assertThat(me.displayName()).isEqualTo("张三");
        assertThat(me.email()).isEqualTo("zhang@campus.edu");
        assertThat(me.role()).isEqualTo(Role.STUDENT);
        assertThat(me.verified()).isFalse();
        assertThat(me.college()).isEqualTo("计算机学院");
        assertThat(me.className()).isEqualTo("软件2101");
        assertThat(me.courses()).hasSize(1);
    }

    @Test
    @DisplayName("查询不存在的用户抛 ID-4001")
    void meRejectsUnknownUser() {
        when(users.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.me(99))
                .isInstanceOfSatisfying(NotFoundException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-4001"));
    }
}
