package com.ai.tutor.appointment.interceptor;

import com.ai.tutor.appointment.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.ai.tutor.utils.RequestHolder.ATTRIBUTE_UID;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从请求头中获取 token
        String token = request.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing or invalid token");
            String requestURI = request.getRequestURI();
            System.out.println("拦截器拦截路径: " + requestURI);
            return false;
        }

        // 去掉 Bearer 前缀
        token = token.substring(7);

        // 校验 token 是否有效
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token expired or invalid");
            return false;
        }

        //  JWT 里目前只存了手机号，所以手机号作为 UID
        String phone = jwtUtil.getPhone(token);

        // ✔ 用 uid 作为统一标识
        request.setAttribute(ATTRIBUTE_UID, phone);

        return true; // 放行请求
    }
}
