package com.campusoverflow.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusoverflow.identity.api.event.UserRegisteredEvent;
import com.campusoverflow.identity.domain.PasswordHasher;
import com.campusoverflow.identity.domain.User;
import com.campusoverflow.identity.domain.UserRepository;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import com.campusoverflow.shared.util.Masking;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("管理员用户管理用例")
class AdminUserServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-19T02:00:00Z");

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final IntegrationEventPublisher events = mock(IntegrationEventPublisher.class);
    private final AuditTrail audit = mock(AuditTrail.class);
    private final AdminUserService service = new AdminUserService(users, hasher, events, audit,
            Clock.fixed(NOW, ZoneOffset.UTC));
    private final Actor admin = new Actor(1, "教务管理员", Role.ADMIN);
    private final Actor student = new Actor(2, "张三", Role.STUDENT);

    @BeforeEach
    void hasherReturnsStableHash() {
        when(hasher.hash(anyString())).thenReturn("bcrypt-hash");
    }

    private static User studentUser(long id) {
        User user = User.register("2023001217", "张三", "zhang@campus.edu", "hash", "计算机学院", "软件2101", NOW);
        user.assignId(id);
        return user;
    }

    @Test
    @DisplayName("非管理员查询待认证列表被拒绝（ID-3001）")
    void listByVerificationRequiresAdmin() {
        assertThatThrownBy(() -> service.listByVerification(student, false, new PageRequest(1, 20)))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-3001"));

        verify(users, never()).findByVerified(any(Boolean.class), any());
    }

    @Test
    @DisplayName("管理员查询待认证列表时学号/工号脱敏")
    void listByVerificationMasksUsername() {
        when(users.findByVerified(false, new PageRequest(1, 20)))
                .thenReturn(new PageResult<>(List.of(studentUser(2)), 1, 1, 20));

        PageResult<UserView> page = service.listByVerification(admin, false, new PageRequest(1, 20));

        assertThat(page.total()).isEqualTo(1);
        UserView view = page.items().get(0);
        assertThat(view.id()).isEqualTo(2);
        assertThat(view.maskedUsername()).isEqualTo(Masking.account("2023001217"));
        assertThat(view.maskedUsername()).doesNotContain("2023001217");
        assertThat(view.displayName()).isEqualTo("张三");
        assertThat(view.verified()).isFalse();
        assertThat(view.createdAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("认证成功：设置已认证并写审计")
    void verifyWritesAudit() {
        User user = studentUser(2);
        when(users.findById(2)).thenReturn(Optional.of(user));

        service.verify(admin, 2);

        assertThat(user.verified()).isTrue();
        verify(users).save(user);
        verify(audit).record("USER_VERIFIED", 1L, "USER", "2", null);
    }

    @Test
    @DisplayName("重复认证抛 ID-2006")
    void verifyRejectsAlreadyVerifiedUser() {
        User user = studentUser(2);
        user.verify(admin);
        when(users.findById(2)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.verify(admin, 2))
                .isInstanceOfSatisfying(ConflictException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-2006"));
    }

    @Test
    @DisplayName("认证不存在的用户抛 ID-4001")
    void verifyRejectsUnknownUser() {
        when(users.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify(admin, 99))
                .isInstanceOfSatisfying(NotFoundException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-4001"));
    }

    @Test
    @DisplayName("管理员创建教师账号：创建即认证，并发布事件与写审计")
    void createStaffSucceeds() {
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User pending = invocation.getArgument(0);
            pending.assignId(10);
            return pending;
        });
        CreateStaffCommand command = new CreateStaffCommand("1001", "王老师", "wang@campus.edu", "pass1234",
                Role.TEACHER, "计算机学院");

        long id = service.createStaff(admin, command);

        assertThat(id).isEqualTo(10);
        verify(events).publish(any(UserRegisteredEvent.class));
        verify(audit).record(eq("STAFF_CREATED"), eq(1L), eq("USER"), eq("10"),
                eq(Role.TEACHER + " " + Masking.account("1001")));
    }

    @Test
    @DisplayName("创建教职工时角色为学生抛 ID-1005")
    void createStaffRejectsStudentRole() {
        CreateStaffCommand command = new CreateStaffCommand("2023001217", "张三", "zhang@campus.edu",
                "pass1234", Role.STUDENT, "计算机学院");

        assertThatThrownBy(() -> service.createStaff(admin, command))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-1005"));

        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("工号/昵称/邮箱任一重复时抛 ID-2001")
    void createStaffRejectsDuplicateIdentity() {
        when(users.existsByEmail("wang@campus.edu")).thenReturn(true);
        CreateStaffCommand command = new CreateStaffCommand("1001", "王老师", "wang@campus.edu", "pass1234",
                Role.TEACHER, "计算机学院");

        assertThatThrownBy(() -> service.createStaff(admin, command))
                .isInstanceOfSatisfying(ConflictException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-2001"));

        verify(events, never()).publish(any());
    }

    @Test
    @DisplayName("非管理员创建教职工被拒绝（ID-3001）")
    void createStaffRequiresAdmin() {
        CreateStaffCommand command = new CreateStaffCommand("1001", "王老师", "wang@campus.edu", "pass1234",
                Role.TEACHER, "计算机学院");

        assertThatThrownBy(() -> service.createStaff(student, command))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-3001"));
    }

    @Test
    @DisplayName("调整角色成功：写审计记录角色变化前后")
    void changeRoleWritesAudit() {
        User user = studentUser(2);
        when(users.findById(2)).thenReturn(Optional.of(user));

        service.changeRole(admin, 2, Role.TA);

        assertThat(user.role()).isEqualTo(Role.TA);
        verify(audit).record("ROLE_CHANGED", 1L, "USER", "2", Role.STUDENT + " -> " + Role.TA);
    }

    @Test
    @DisplayName("管理员不能修改自己的角色（ID-1006）")
    void changeRoleRejectsSelfChange() {
        User self = User.reconstitute(1L, "1001", "教务管理员", "admin@campus.edu", "hash", Role.ADMIN,
                com.campusoverflow.identity.domain.UserStatus.ACTIVE, true, null, null, NOW, 0);
        when(users.findById(1)).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> service.changeRole(admin, 1, Role.TEACHER))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-1006"));
    }

    @Test
    @DisplayName("调整不存在用户的角色抛 ID-4001")
    void changeRoleRejectsUnknownUser() {
        when(users.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeRole(admin, 99, Role.TA))
                .isInstanceOfSatisfying(NotFoundException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-4001"));
    }
}
