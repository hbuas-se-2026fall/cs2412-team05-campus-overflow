package com.campusoverflow.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusoverflow.identity.domain.Course;
import com.campusoverflow.identity.domain.CourseMember;
import com.campusoverflow.identity.domain.CourseMemberStatus;
import com.campusoverflow.identity.domain.CourseRepository;
import com.campusoverflow.identity.domain.CourseRole;
import com.campusoverflow.identity.domain.User;
import com.campusoverflow.identity.domain.UserRepository;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("课程与课程成员用例")
class CourseServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-19T02:00:00Z");

    private final CourseRepository courses = mock(CourseRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CourseService service = new CourseService(courses, users, Clock.fixed(NOW, ZoneOffset.UTC));
    private final Actor teacher = new Actor(1, "王老师", Role.TEACHER);
    private final Actor student = new Actor(2, "张三", Role.STUDENT);
    private Course course;

    @BeforeEach
    void setUp() {
        course = Course.create(teacher, "DS-101", "数据结构", "2026-秋", NOW);
        course.assignId(100);
        when(courses.findById(100)).thenReturn(Optional.of(course));
    }

    private static User user(long id, String name) {
        User u = User.register("20230012" + id, name, "u" + id + "@campus.edu", "hash", "计算机学院", "软件2101",
                NOW);
        u.assignId(id);
        return u;
    }

    @Test
    @DisplayName("建课成功：返回课程 ID，并把创建者登记为课程教师")
    void createRegistersTeacherMembership() {
        when(courses.save(any(Course.class))).thenAnswer(invocation -> {
            Course pending = invocation.getArgument(0);
            pending.assignId(100);
            return pending;
        });

        long id = service.create(teacher, new CreateCourseCommand("DS-101", "数据结构", "2026-秋"));

        assertThat(id).isEqualTo(100);
        ArgumentCaptor<CourseMember> member = ArgumentCaptor.forClass(CourseMember.class);
        verify(courses).saveMember(member.capture());
        assertThat(member.getValue().userId()).isEqualTo(1);
        assertThat(member.getValue().role()).isEqualTo(CourseRole.TEACHER);
        assertThat(member.getValue().status()).isEqualTo(CourseMemberStatus.APPROVED);
    }

    @Test
    @DisplayName("同一学期同代码课程重复时抛 ID-2004")
    void createRejectsDuplicateCodeInSameTerm() {
        when(courses.existsByCodeAndTerm("DS-101", "2026-秋")).thenReturn(true);

        assertThatThrownBy(() -> service.create(teacher, new CreateCourseCommand("DS-101", "数据结构", "2026-秋")))
                .isInstanceOfSatisfying(ConflictException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-2004"));

        verify(courses, never()).save(any());
    }

    @Test
    @DisplayName("学生不能建课（ID-3002）")
    void createRequiresTeacherOrAdmin() {
        assertThatThrownBy(() -> service.create(student, new CreateCourseCommand("DS-101", "数据结构", "2026-秋")))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-3002"));
    }

    @Test
    @DisplayName("课程代码非法时抛 ID-1010")
    void createRejectsIllegalCode() {
        assertThatThrownBy(() -> service.create(teacher, new CreateCourseCommand("D", "数据结构", "2026-秋")))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-1010"));
    }

    @Test
    @DisplayName("直接加入课程：登记为已通过的学生成员")
    void joinSavesApprovedStudentMember() {
        when(courses.findMember(100, 2)).thenReturn(Optional.empty());

        service.join(student, 100);

        ArgumentCaptor<CourseMember> member = ArgumentCaptor.forClass(CourseMember.class);
        verify(courses).saveMember(member.capture());
        assertThat(member.getValue().userId()).isEqualTo(2);
        assertThat(member.getValue().role()).isEqualTo(CourseRole.STUDENT);
        assertThat(member.getValue().isApproved()).isTrue();
    }

    @Test
    @DisplayName("已在课程中再次加入抛 ID-2005")
    void joinRejectsExistingMember() {
        when(courses.findMember(100, 2)).thenReturn(Optional.of(CourseMember.student(100, 2, NOW)));

        assertThatThrownBy(() -> service.join(student, 100))
                .isInstanceOfSatisfying(ConflictException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-2005"));
    }

    @Test
    @DisplayName("对不存在的课程操作抛 ID-4002")
    void joinRejectsUnknownCourse() {
        when(courses.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.join(student, 999))
                .isInstanceOfSatisfying(NotFoundException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-4002"));
    }

    @Test
    @DisplayName("教师任命助教：成员角色被更新并保存")
    void assignRoleByTeacher() {
        CourseMember member = CourseMember.student(100, 2, NOW);
        when(courses.findMember(100, 2)).thenReturn(Optional.of(member));

        service.assignRole(teacher, 100, 2, CourseRole.TA);

        assertThat(member.role()).isEqualTo(CourseRole.TA);
        verify(courses).saveMember(member);
    }

    @Test
    @DisplayName("非课程教师不能调整成员角色（ID-3003）")
    void assignRoleRequiresCourseTeacher() {
        assertThatThrownBy(() -> service.assignRole(student, 100, 2, CourseRole.TA))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-3003"));
    }

    @Test
    @DisplayName("调整非成员的角色抛 ID-4003")
    void assignRoleRejectsNonMember() {
        when(courses.findMember(100, 9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignRole(teacher, 100, 9, CourseRole.TA))
                .isInstanceOfSatisfying(NotFoundException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-4003"));
    }

    @Test
    @DisplayName("课程列表中未登录用户看不到自己的课程角色")
    void listAllWithoutActorLeavesMyRoleNull() {
        when(courses.findAll()).thenReturn(List.of(course));
        when(users.findAllById(List.of(1L))).thenReturn(List.of(user(1, "王老师")));

        List<CourseView> views = service.listAll(null);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).teacherName()).isEqualTo("王老师");
        assertThat(views.get(0).myRole()).isNull();
    }

    @Test
    @DisplayName("课程列表中带出当前用户在该课的角色")
    void listAllWithActorExposesMyRole() {
        when(courses.findAll()).thenReturn(List.of(course));
        when(users.findAllById(List.of(1L))).thenReturn(List.of(user(1, "王老师")));
        when(courses.findMembershipsOfUser(2))
                .thenReturn(List.of(CourseMember.reconstitute(100, 2, CourseRole.TA, CourseMemberStatus.APPROVED,
                        NOW)));

        List<CourseView> views = service.listAll(student);

        assertThat(views.get(0).myRole()).isEqualTo(CourseRole.TA.name());
    }

    @Test
    @DisplayName("我的课程只包含本人已加入的课程")
    void myCoursesFiltersByMembership() {
        Course other = Course.create(teacher, "OS-201", "操作系统", "2026-秋", NOW);
        other.assignId(200);
        when(courses.findAll()).thenReturn(List.of(course, other));
        when(users.findAllById(List.of(1L))).thenReturn(List.of(user(1, "王老师")));
        when(courses.findMembershipsOfUser(2))
                .thenReturn(List.of(CourseMember.reconstitute(200, 2, CourseRole.STUDENT,
                        CourseMemberStatus.APPROVED, NOW)));

        List<CourseView> mine = service.myCourses(2);

        assertThat(mine).extracting(CourseView::id).containsExactly(200L);
    }

    @Test
    @DisplayName("成员列表中查不到用户时显示占位名")
    void membersFallBackToPlaceholderName() {
        when(courses.findMembers(100)).thenReturn(List.of(CourseMember.student(100, 9, NOW)));
        when(users.findAllById(List.of(9L))).thenReturn(List.of());

        List<CourseMemberView> views = service.members(100);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).displayName()).isEqualTo("未知用户");
    }

    @Test
    @DisplayName("管理员可审核任意课程的选课申请")
    void reviewApplicationAllowedForAdmin() {
        Actor admin = new Actor(9, "教务管理员", Role.ADMIN);
        CourseMember application = CourseMember.applicant(100, 2, NOW);
        when(courses.findMember(100, 2)).thenReturn(Optional.of(application));

        service.reviewApplication(admin, 100, 2, true);

        assertThat(application.isApproved()).isTrue();
        verify(courses).saveMember(application);
    }

    @Test
    @DisplayName("课程助教可审核本课程的选课申请")
    void reviewApplicationAllowedForCourseTa() {
        Actor ta = new Actor(5, "助教", Role.TA);
        CourseMember application = CourseMember.applicant(100, 2, NOW);
        when(courses.findMember(100, 5)).thenReturn(Optional.of(
                CourseMember.reconstitute(100, 5, CourseRole.TA, CourseMemberStatus.APPROVED, NOW)));
        when(courses.findMember(100, 2)).thenReturn(Optional.of(application));

        service.reviewApplication(ta, 100, 2, false);

        assertThat(application.status()).isEqualTo(CourseMemberStatus.REJECTED);
    }
}
