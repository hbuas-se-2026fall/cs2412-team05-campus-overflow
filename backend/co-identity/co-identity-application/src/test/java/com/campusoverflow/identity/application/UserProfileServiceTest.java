package com.campusoverflow.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.campusoverflow.identity.domain.User;
import com.campusoverflow.identity.domain.UserRepository;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("本人资料修改用例")
class UserProfileServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-19T02:00:00Z");

    private final UserRepository users = mock(UserRepository.class);
    private final UserQueryService userQuery = mock(UserQueryService.class);
    private final UserProfileService service = new UserProfileService(users, userQuery);
    private final Actor actor = new Actor(2, "张三", Role.STUDENT);

    private static User user(long id, String displayName, String email) {
        User u = User.register("2023001217", displayName, email, "hash", "计算机学院", "软件2101", NOW);
        u.assignId(id);
        return u;
    }

    @Test
    @DisplayName("昵称被他人占用时抛 ID-2002")
    void updateRejectsDisplayNameTakenByOthers() {
        when(users.findById(2)).thenReturn(Optional.of(user(2, "张三", "zhang@campus.edu")));
        when(users.findByDisplayName("李四")).thenReturn(Optional.of(user(3, "李四", "li@campus.edu")));

        assertThatThrownBy(() -> service.update(actor, new UpdateProfileCommand("李四", "new@campus.edu", null, null)))
                .isInstanceOfSatisfying(ConflictException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-2002"));
    }

    @Test
    @DisplayName("邮箱被他人占用时抛 ID-2003")
    void updateRejectsEmailTakenByOthers() {
        when(users.findById(2)).thenReturn(Optional.of(user(2, "张三", "zhang@campus.edu")));
        when(users.findByDisplayName("张三")).thenReturn(Optional.of(user(2, "张三", "zhang@campus.edu")));
        when(users.findByEmail("li@campus.edu")).thenReturn(Optional.of(user(3, "李四", "li@campus.edu")));

        assertThatThrownBy(() -> service.update(actor, new UpdateProfileCommand("张三", "li@campus.edu", null, null)))
                .isInstanceOfSatisfying(ConflictException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-2003"));
    }

    @Test
    @DisplayName("保留自己的昵称与邮箱不算冲突")
    void updateAllowsKeepingOwnIdentity() {
        User self = user(2, "张三", "zhang@campus.edu");
        when(users.findById(2)).thenReturn(Optional.of(self));
        when(users.findByDisplayName("张三")).thenReturn(Optional.of(self));
        when(users.findByEmail("zhang@campus.edu")).thenReturn(Optional.of(self));
        MeView view = new MeView(2, "2023001217", "张三", "zhang@campus.edu", Role.STUDENT, false, "数学学院",
                "数学2101", java.util.List.of());
        when(userQuery.me(2)).thenReturn(view);

        MeView result = service.update(actor, new UpdateProfileCommand("张三", "zhang@campus.edu", "数学学院",
                "数学2101"));

        assertThat(result).isSameAs(view);
        assertThat(self.college()).isEqualTo("数学学院");
    }

    @Test
    @DisplayName("用户不存在时抛 ID-4001")
    void updateRejectsUnknownUser() {
        when(users.findById(2)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(actor, new UpdateProfileCommand("张三", "z@campus.edu", null, null)))
                .isInstanceOfSatisfying(NotFoundException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-4001"));
    }
}
