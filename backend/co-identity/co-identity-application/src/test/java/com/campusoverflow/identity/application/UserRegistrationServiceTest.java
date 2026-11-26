package com.campusoverflow.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campusoverflow.identity.api.event.UserRegisteredEvent;
import com.campusoverflow.identity.domain.PasswordHasher;
import com.campusoverflow.identity.domain.User;
import com.campusoverflow.identity.domain.UserRepository;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Role;
import com.campusoverflow.shared.util.Masking;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("学生自助注册用例")
class UserRegistrationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-19T02:00:00Z");

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final IntegrationEventPublisher events = mock(IntegrationEventPublisher.class);
    private final AuditTrail audit = mock(AuditTrail.class);
    private final UserRegistrationService service = new UserRegistrationService(users, hasher, events, audit,
            Clock.fixed(NOW, ZoneOffset.UTC));
    private final RegisterUserCommand command = new RegisterUserCommand("2023001217", "张三", "Zhang@Campus.edu",
            "abc12345", "计算机学院", "软件2101");

    @BeforeEach
    void hasherReturnsStableHash() {
        when(hasher.hash(anyString())).thenReturn("bcrypt-hash");
    }

    @Test
    @DisplayName("注册成功：返回新用户 ID，并发布注册事件与写审计")
    void registerSucceedsAndPublishesSideEffects() {
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User pending = invocation.getArgument(0);
            pending.assignId(7);
            return pending;
        });

        long id = service.register(command);

        assertThat(id).isEqualTo(7);

        ArgumentCaptor<UserRegisteredEvent> event = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(events).publish(event.capture());
        assertThat(event.getValue().userId()).isEqualTo(7);
        assertThat(event.getValue().displayName()).isEqualTo("张三");
        assertThat(event.getValue().role()).isEqualTo(Role.STUDENT);
        assertThat(event.getValue().occurredAt()).isEqualTo(NOW);

        verify(audit).record(eq("USER_REGISTERED"), eq(7L), eq("USER"), eq("7"),
                eq(Masking.account("2023001217")));
    }

    @Test
    @DisplayName("注册时口令先哈希再入库，邮箱统一转小写")
    void registerHashesPasswordAndNormalisesEmail() {
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User pending = invocation.getArgument(0);
            pending.assignId(8);
            return pending;
        });

        service.register(command);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().passwordHash()).isEqualTo("bcrypt-hash");
        assertThat(saved.getValue().email()).isEqualTo("zhang@campus.edu");
        assertThat(saved.getValue().verified()).isFalse();
        assertThat(saved.getValue().createdAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("学号已存在时抛 ID-2001，且不落库、不发事件")
    void registerRejectsDuplicateUsername() {
        when(users.existsByUsername("2023001217")).thenReturn(true);

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOfSatisfying(ConflictException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-2001"));

        verify(users, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    @DisplayName("昵称已存在时抛 ID-2002")
    void registerRejectsDuplicateDisplayName() {
        when(users.existsByDisplayName("张三")).thenReturn(true);

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOfSatisfying(ConflictException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-2002"));
    }

    @Test
    @DisplayName("邮箱已存在时抛 ID-2003")
    void registerRejectsDuplicateEmail() {
        when(users.existsByEmail("zhang@campus.edu")).thenReturn(true);

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOfSatisfying(ConflictException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-2003"));
    }

    @Test
    @DisplayName("口令不合规时抛 ID-1004，且不触碰仓储与哈希器")
    void registerRejectsWeakPasswordBeforeTouchingStorage() {
        RegisterUserCommand weak = new RegisterUserCommand("2023001217", "张三", "zhang@campus.edu",
                "1234567", "计算机学院", "软件2101");

        assertThatThrownBy(() -> service.register(weak))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.code()).isEqualTo("ID-1004"));

        verifyNoInteractions(users, events, audit);
        verify(hasher, never()).hash(anyString());
    }
}
