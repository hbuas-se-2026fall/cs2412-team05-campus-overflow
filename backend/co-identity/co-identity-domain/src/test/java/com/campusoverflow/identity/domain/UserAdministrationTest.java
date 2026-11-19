package com.campusoverflow.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 用户聚合的管理员侧用例与字段读出口径（UserTest 只覆盖了注册与自助资料修改）。 */
class UserAdministrationTest {

    private static final Instant NOW = Instant.parse("2026-09-01T08:00:00Z");
    private final Actor admin = new Actor(1, "admin", Role.ADMIN);

    private User persistedStudent() {
        User user = User.register("2023001217", "小明", "ming@campus.edu", "hash", null, null, NOW);
        user.assignId(2);
        return user;
    }

    @Test
    void adminCanChangeAnotherUsersRole() {
        User user = persistedStudent();

        user.changeRole(admin, Role.TA);

        assertThat(user.role()).isEqualTo(Role.TA);
    }

    @Test
    void adminCanDisableAnotherUserAndDisabledUserCannotLogin() {
        User user = persistedStudent();

        user.disable(admin);

        assertThat(user.status()).isEqualTo(UserStatus.DISABLED);
        assertThat(user.canLogin()).isFalse();
    }

    @Test
    void adminCannotDisableOwnAccount() {
        User self = User.reconstitute(1L, "admin01", "admin", "a@b.cn", "h", Role.ADMIN, UserStatus.ACTIVE,
                true, null, null, NOW, 0);

        assertThatThrownBy(() -> self.disable(admin))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                    assertThat(ex.code()).isEqualTo("ID-1007");
                    assertThat(self.status()).isEqualTo(UserStatus.ACTIVE);
                });
    }

    @Test
    void nonAdminCannotDisableAnyUser() {
        User user = persistedStudent();
        Actor teacher = new Actor(9, "王老师", Role.TEACHER);

        assertThatThrownBy(() -> user.disable(teacher))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-3001"));
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void managementOperationsFailFastOnUnsavedUser() {
        User unsaved = User.register("2023001218", "小红", "hong@campus.edu", "h", null, null, NOW);

        // requireId() 的作用是拦住「尚未持久化就做管理操作」，此处两个用例都走到同一条守卫
        assertThatThrownBy(() -> unsaved.changeRole(admin, Role.TA))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("尚未持久化");
        assertThatThrownBy(() -> unsaved.disable(admin))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("尚未持久化");
    }

    @Test
    void assignIdIsOneShot() {
        User user = persistedStudent();

        assertThatThrownBy(() -> user.assignId(3))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("已分配");
        assertThat(user.id()).isEqualTo(2);
    }

    @Test
    void optionalFieldsAreTruncatedToColumnWidth() {
        String longCollege = "计算机工程学院".repeat(10);

        User user = User.register("2023001219", "小刚", "gang@campus.edu", "h", longCollege, longCollege, NOW);

        assertThat(user.college()).hasSize(50);
        assertThat(user.className()).hasSize(50);
    }

    @Test
    void reconstituteExposesAllPersistedFields() {
        User user = User.reconstitute(7L, "T2001", "王老师", "wang@campus.edu", "hash", Role.TEACHER,
                UserStatus.ACTIVE, true, "计算机工程学院", null, NOW, 3);

        assertThat(user.id()).isEqualTo(7);
        assertThat(user.username()).isEqualTo("T2001");
        assertThat(user.displayName()).isEqualTo("王老师");
        assertThat(user.email()).isEqualTo("wang@campus.edu");
        assertThat(user.passwordHash()).isEqualTo("hash");
        assertThat(user.role()).isEqualTo(Role.TEACHER);
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.verified()).isTrue();
        assertThat(user.college()).isEqualTo("计算机工程学院");
        assertThat(user.className()).isNull();
        assertThat(user.createdAt()).isEqualTo(NOW);
        assertThat(user.version()).isEqualTo(3);
    }
}
