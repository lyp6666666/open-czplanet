package com.ai.tutor.appointment.interceptor;

import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleInterceptor implements HandlerInterceptor {

    private static final String ATTRIBUTE_ROLE = "role";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return true;
        }
        if (uri.startsWith("/api/v1/public/")) return true;
        if (uri.equals("/user/loginOrRegister") || uri.equals("/user/sendcode")) return true;
        if (uri.startsWith("/swagger-ui/") || uri.equals("/swagger-ui.html")) return true;
        if (uri.startsWith("/v3/api-docs/") || uri.equals("/v3/api-docs")) return true;
        if (uri.startsWith("/swagger-resources/") || uri.startsWith("/webjars/")) return true;
        if (uri.equals("/favicon.ico") || uri.equals("/error") || uri.equals("/actuator/httpexchanges")) return true;

        UserRoleEnum role = (UserRoleEnum) request.getAttribute(ATTRIBUTE_ROLE);
        if (role == null) {
            return true;
        }

        String method = request.getMethod();
        if (method == null) {
            return true;
        }

        if (uri.equals("/api/v1/parent/jobs") && "POST".equalsIgnoreCase(method)) {
            require(role, UserRoleEnum.STUDENT);
        }
        if (uri.startsWith("/api/v1/parent/jobs/") && "PUT".equalsIgnoreCase(method)) {
            require(role, UserRoleEnum.STUDENT);
        }
        if (uri.equals("/api/v1/parent/jobs/mine")) {
            require(role, UserRoleEnum.STUDENT);
        }

        if (uri.equals("/api/v1/tutor/services") && "POST".equalsIgnoreCase(method)) {
            require(role, UserRoleEnum.TEACHER);
        }
        if (uri.startsWith("/api/v1/tutor/services/") && "PUT".equalsIgnoreCase(method)) {
            require(role, UserRoleEnum.TEACHER);
        }
        if (uri.equals("/api/v1/tutor/services/mine")) {
            require(role, UserRoleEnum.TEACHER);
        }

        if (uri.startsWith("/api/v1/tutor/favorites/")) {
            require(role, UserRoleEnum.TEACHER);
        }

        if (uri.startsWith("/api/v1/parent/favorites/")) {
            require(role, UserRoleEnum.STUDENT);
        }

        return true;
    }

    private void require(UserRoleEnum current, UserRoleEnum expected) {
        if (current != expected) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该资源");
        }
    }
}
