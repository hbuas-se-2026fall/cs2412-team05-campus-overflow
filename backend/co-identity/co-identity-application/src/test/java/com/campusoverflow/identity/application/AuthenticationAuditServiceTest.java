package com.campusoverflow.identity.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.util.Masking;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("登录审计用例")
class AuthenticationAuditServiceTest {

    private final AuditTrail audit = mock(AuditTrail.class);
    private final AuthenticationAuditService service = new AuthenticationAuditService(audit);

    @Test
    @DisplayName("登录成功：记录 LOGIN_SUCCESS，学号脱敏")
    void loginSucceededRecordsMaskedAccount() {
        service.loginSucceeded(2, "2023001217");

        verify(audit).record("LOGIN_SUCCESS", 2L, "USER", "2", Masking.account("2023001217"));
    }

    @Test
    @DisplayName("登录失败：主体与目标为空，原因是脱敏账号加失败原因")
    void loginFailedRecordsReasonWithoutSubject() {
        service.loginFailed("2023001217", "口令错误");

        verify(audit).record("LOGIN_FAILURE", null, "USER", null,
                Masking.account("2023001217") + " 口令错误");
    }
}
