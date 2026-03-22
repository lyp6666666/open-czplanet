package com.ai.tutor.common.security;

import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.utils.RequestHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Objects;

public class GatewayIdentityInterceptor implements HandlerInterceptor {

    private final IdentitySignProperties properties;
    private final IdentitySignatureUtils signatureUtils;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public GatewayIdentityInterceptor(IdentitySignProperties properties, IdentitySignatureUtils signatureUtils) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.signatureUtils = Objects.requireNonNull(signatureUtils, "signatureUtils");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (isWhitelisted(path)) {
            return true;
        }

        String uidHeader = request.getHeader("X-Uid");
        String roleHeader = request.getHeader("X-Role");
        String tsHeader = request.getHeader("X-Ts");
        String signHeader = request.getHeader("X-Auth-Sign");

        if (!StringUtils.hasText(uidHeader) || !StringUtils.hasText(roleHeader)
                || !StringUtils.hasText(tsHeader) || !StringUtils.hasText(signHeader)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        long uid;
        int role;
        long ts;
        try {
            uid = Long.parseLong(uidHeader.trim());
            role = Integer.parseInt(roleHeader.trim());
            ts = Long.parseLong(tsHeader.trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        long skewMs = Math.max(0L, properties.getClockSkewMs());
        long now = System.currentTimeMillis();
        if (Math.abs(now - ts) > skewMs) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        String method = request.getMethod();
        String requestTarget = resolveRequestTarget(request);
        if (!signatureUtils.verify(uid, role, ts, method, requestTarget, signHeader.trim())) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        RequestInfo info = new RequestInfo();
        info.setUid(uid);
        info.setRole(role);
        info.setIp(resolveIp(request));
        RequestHolder.set(info);

        request.setAttribute(RequestHolder.ATTRIBUTE_UID, String.valueOf(uid));
        request.setAttribute(RequestHolder.ATTRIBUTE_ROLE, role);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        RequestHolder.remove();
    }

    private boolean isWhitelisted(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        List<String> whitelist = properties.getWhitelistPaths();
        if (whitelist == null || whitelist.isEmpty()) {
            return false;
        }
        for (String pattern : whitelist) {
            if (!StringUtils.hasText(pattern)) {
                continue;
            }
            if (pathMatcher.match(pattern.trim(), path)) {
                return true;
            }
        }
        return false;
    }

    private String resolveRequestTarget(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            uri = "";
        }
        String query = request.getQueryString();
        if (StringUtils.hasText(query)) {
            return uri + "?" + query;
        }
        return uri;
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            String[] parts = forwarded.split(",");
            if (parts.length > 0 && StringUtils.hasText(parts[0])) {
                return parts[0].trim();
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
