package com.ai.tutor.appointment.interceptor;

import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.utils.RequestHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleInterceptorTest {

    @AfterEach
    void tearDown() {
        RequestHolder.remove();
    }

    @Test
    void shouldRejectWhenRoleMismatchFromRequestHolder() {
        RoleInterceptor interceptor = new RoleInterceptor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/api/v1/parent/jobs");
        when(request.getMethod()).thenReturn("POST");

        RequestInfo info = new RequestInfo();
        info.setRole(UserRoleEnum.TEACHER.getValue());
        RequestHolder.set(info);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, response, new Object()));
        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectWhenRoleValueUnknown() {
        RoleInterceptor interceptor = new RoleInterceptor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/api/v1/parent/jobs");
        when(request.getMethod()).thenReturn("POST");

        RequestInfo info = new RequestInfo();
        info.setRole(99);
        RequestHolder.set(info);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, response, new Object()));
        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldAllowWhenRoleMatches() {
        RoleInterceptor interceptor = new RoleInterceptor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/api/v1/parent/jobs");
        when(request.getMethod()).thenReturn("POST");

        RequestInfo info = new RequestInfo();
        info.setRole(UserRoleEnum.STUDENT.getValue());
        RequestHolder.set(info);

        interceptor.preHandle(request, response, new Object());
    }
}
