package com.campusoverflow.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-09-01T08:00:00Z");
    private final Actor admin = new Actor(1, "admin", Role.ADMIN);
    private final Actor student = new Actor(2, "alice", Role.STUDENT);

    private User newStudent() {
        return User.register("2023001217", "小明", "Ming@Campus.edu", "hash", "计算机工程学院", "计科2301", NOW);
    }

    @Test
    void registerCreatesUnverifiedStudentWithNormalizedEmail() {
        User u = newStudent();
        assertThat(u.role()).isEqualTo(Role.STUDENT);
        assertThat(u.verified()).isFalse();
        assertThat(u.email()).isEqualTo("ming@campus.edu");
        assertThat(u.canLogin()).isTrue();
    }

    @Test
    void rejectsInvalidUsernameDisplayNameAndEmail() {
        assertThatThrownBy(() -> User.register("ab", "小明", "a@b.cn", "h", null, null, NOW))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("学号");
        assertThatThrownBy(() -> User.register("20230001", "小 明", "a@b.cn", "h", null, null, NOW))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> User.register("20230001", "小明", "not-an-email", "h", null, null, NOW))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("邮箱");
    }

    @Test
    void onlyAdminCanVerifyAndOnlyOnce() {
        User u = newStudent();
        u.assignId(10);
        assertThatThrownBy(() -> u.verify(student)).isInstanceOf(ForbiddenException.class);
        u.verify(admin);
        assertThat(u.verified()).isTrue();
        assertThatThrownBy(() -> u.verify(admin)).isInstanceOf(ConflictException.class);
    }

    @Test
    void adminCannotChangeOwnRole() {
        User self = User.reconstitute(1L, "admin01", "admin", "a@b.cn", "h", Role.ADMIN, UserStatus.ACTIVE,
                true, null, null, NOW, 0);
        assertThatThrownBy(() -> self.changeRole(admin, Role.STUDENT)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void staffAccountsAreVerifiedAndCannotBeStudents() {
        User t = User.createStaff(admin, "T2001", "王老师", "wang@campus.edu", "h", Role.TEACHER, "计算机工程学院", NOW);
        assertThat(t.verified()).isTrue();
        assertThatThrownBy(() -> User.createStaff(admin, "T2002", "李老师", "li@campus.edu", "h", Role.STUDENT, null, NOW))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> User.createStaff(student, "T2003", "赵老师", "z@campus.edu", "h", Role.TEACHER, null, NOW))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void studentCanUpdateOwnEditableProfileFields() {
        User u = newStudent();
        String username = u.username();

        u.updateProfile("新昵称", "New@Campus.edu", " 新学院 ", null);

        assertThat(u.displayName()).isEqualTo("新昵称");
        assertThat(u.email()).isEqualTo("new@campus.edu");
        assertThat(u.college()).isEqualTo("新学院");
        assertThat(u.className()).isNull();
        assertThat(u.username()).isEqualTo(username);
        assertThatThrownBy(() -> u.updateProfile("含 空格", "a@b.cn", null, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void passwordPolicyRequiresLettersAndDigits() {
        assertThatThrownBy(() -> PasswordPolicy.validate("short1")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("onlyletters")).isInstanceOf(BusinessRuleException.class);
        PasswordPolicy.validate("Passw0rd!");
    }

    @Test
    void emailValidationAcceptsCommonFormsAndRejectsMalformedOnes() {
        // 与重构前那条 ^[\w.+-]+@[\w-]+(\.[\w-]+)+$ 逐项对齐：
        // 去掉嵌套量词只是换了实现方式，既不放宽也不收紧判定。
        for (String ok : new String[] {"a@b.cn", "ming@campus.edu", "first.last+tag@sub.campus.edu.cn",
                "a-b@my-school.edu", ".leading@b.cn", "a@b-c.cn"}) {
            assertThatCode(() -> User.register("20230001", "小明", ok, "h", null, null, NOW))
                    .as("应接受 %s", ok).doesNotThrowAnyException();
        }
        for (String bad : new String[] {"not-an-email", "a@b", "@b.cn", "a@", "a@@b.cn", "a@b.",
                "a@.b.cn", "a@b..cn", "a b@c.cn", "a@b!c.cn", "ａ@b.cn"}) {
            assertThatThrownBy(() -> User.register("20230001", "小明", bad, "h", null, null, NOW))
                    .as("应拒绝 %s", bad).isInstanceOf(BusinessRuleException.class);
        }
    }

    @Test
    void emailValidationRejectsAdversarialInputWithoutRunawayBacktracking() {
        // java:S5998 指的就是这类输入：@ 之后跟着大量重复的 .x 段。
        // 断言两件事——结论仍是拒绝；且不会因回溯把校验拖成秒级以上
        // （长度上限 100 会先拦一道，所以取 99 字符，让它确实走到格式校验）。
        String adversarial = "a@" + ".a".repeat(48) + "!";
        assertThat(adversarial).hasSize(99);

        long start = System.nanoTime();
        assertThatThrownBy(() -> User.register("20230001", "小明", adversarial, "h", null, null, NOW))
                .isInstanceOf(BusinessRuleException.class);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        // 真发生指数级回溯时这条断言会以「分钟」为单位失败，1 秒的界足够区分，
        // 又留出 CI 机器冷启动与类加载的余量。
        assertThat(elapsed).isLessThan(Duration.ofSeconds(1));
    }
}
