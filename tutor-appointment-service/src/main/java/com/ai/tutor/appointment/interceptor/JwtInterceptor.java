package com.ai.tutor.appointment.interceptor;

import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.ai.tutor.appointment.utils.JwtUtil;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.common.service.dto.RequestInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.ai.tutor.utils.RequestHolder;

import static com.ai.tutor.utils.RequestHolder.ATTRIBUTE_UID;
import static com.ai.tutor.utils.RequestHolder.ATTRIBUTE_PHONE;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    private static final String ATTRIBUTE_ROLE = "role";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        if (uri != null) {
            if (uri.startsWith("/api/v1/public/")) return true;
            if (uri.startsWith("/api/admin/")) return true;
            if (uri.equals("/user/loginOrRegister") || uri.equals("/user/sendcode")) return true;
            if (uri.startsWith("/swagger-ui/") || uri.equals("/swagger-ui.html")) return true;
            if (uri.startsWith("/v3/api-docs/") || uri.equals("/v3/api-docs")) return true;
            if (uri.startsWith("/swagger-resources/") || uri.startsWith("/webjars/")) return true;
            if (uri.equals("/favicon.ico") || uri.equals("/error") || uri.equals("/actuator/httpexchanges")) return true;
        }

        // 从请求头中获取 token
        String token = request.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "缺少 Authorization: Bearer token");
        }

        // 去掉 Bearer 前缀
        token = token.substring(7);

        // 校验 token 是否有效
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "token 已过期或无效");
        }

        Long userId = jwtUtil.getUserId(token);
        String phone = jwtUtil.getPhone(token);
        UserRoleEnum role = jwtUtil.getRole(token);

        // 统一用 userId 作为用户身份标识，避免手机号变更导致身份漂移
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "token 缺少 userId");
        }

        request.setAttribute(ATTRIBUTE_UID, String.valueOf(userId));
        request.setAttribute(ATTRIBUTE_PHONE, phone);
        request.setAttribute(ATTRIBUTE_ROLE, role);

        RequestInfo info = RequestHolder.get();
        if (info == null) {
            info = new RequestInfo();
        }
        info.setUid(userId);
        info.setRole(role == null ? null : role.getValue());
        RequestHolder.set(info);

        return true; // 放行请求
    }
}
