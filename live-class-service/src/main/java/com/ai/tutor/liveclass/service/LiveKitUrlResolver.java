package com.ai.tutor.liveclass.service;

import com.ai.tutor.liveclass.config.LiveKitProperties;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class LiveKitUrlResolver {

    private static final String DEFAULT_PUBLIC_HOST = "huoyue.online";

    @Resource
    private LiveKitProperties liveKitProperties;

    public String resolvePublicWsUrl(HttpServletRequest request) {
        String configured = trimToNull(liveKitProperties.getPublicWsUrl());
        if (configured != null) {
            return normalize(configured);
        }

        String wsUrl = trimToNull(liveKitProperties.getWsUrl());
        if (wsUrl == null) {
            return null;
        }
        if (!isLoopback(wsUrl) && !isPrivateHost(wsUrl) && isBrowserSafeWsUrl(wsUrl, request)) {
            return normalize(wsUrl);
        }

        if (request == null) {
            return normalize(wsUrl);
        }

        String forwardedProto = firstForwardedValue(request.getHeader("X-Forwarded-Proto"));
        String forwardedHost = firstForwardedValue(request.getHeader("X-Forwarded-Host"));
        String host = forwardedHost != null ? forwardedHost : firstForwardedValue(request.getHeader("Host"));
        if (host == null || isLoopbackHost(host) || isPrivateHost(host) || isRawIpHost(host)) {
            host = DEFAULT_PUBLIC_HOST;
        }

        String scheme = "http".equalsIgnoreCase(forwardedProto) && !DEFAULT_PUBLIC_HOST.equalsIgnoreCase(stripPort(host))
                ? "ws"
                : "wss";
        return normalize(scheme + "://" + host + "/livekit");
    }

    private static boolean isLoopback(String raw) {
        String lower = raw.toLowerCase();
        return lower.contains("127.0.0.1") || lower.contains("localhost");
    }

    private static boolean isLoopbackHost(String raw) {
        String lower = stripPort(raw).toLowerCase();
        return "127.0.0.1".equals(lower) || "localhost".equals(lower);
    }

    private static boolean isPrivateHost(String raw) {
        String lower = stripPort(raw).toLowerCase();
        return lower.startsWith("0.0.0.0") || lower.startsWith("172.16.") || lower.startsWith("172.17.")
                || lower.startsWith("172.18.") || lower.startsWith("172.19.") || lower.startsWith("172.20.")
                || lower.startsWith("192.168.") || lower.startsWith("10.");
    }

    private static boolean isBrowserSafeWsUrl(String raw, HttpServletRequest request) {
        String value = trimToNull(raw);
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase();
        if (lower.startsWith("wss://")) {
            return true;
        }
        if (request == null) {
            return !lower.startsWith("ws://") && !lower.startsWith("http://");
        }
        String proto = firstForwardedValue(request.getHeader("X-Forwarded-Proto"));
        boolean secureRequest = request.isSecure() || "https".equalsIgnoreCase(proto);
        return !secureRequest;
    }

    private static boolean isRawIpHost(String raw) {
        String host = stripPort(raw);
        if (host.isEmpty()) {
            return false;
        }
        return host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+");
    }

    private static String normalize(String raw) {
        return raw == null ? null : raw.trim().replaceAll("/+$", "");
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String firstForwardedValue(String raw) {
        String value = trimToNull(raw);
        if (value == null) {
            return null;
        }
        int comma = value.indexOf(',');
        return comma < 0 ? value : trimToNull(value.substring(0, comma));
    }

    private static String stripPort(String raw) {
        String value = trimToNull(raw);
        if (value == null) {
            return "";
        }
        if (value.startsWith("[") && value.contains("]")) {
            return value.substring(1, value.indexOf(']'));
        }
        int colon = value.lastIndexOf(':');
        return colon > 0 ? value.substring(0, colon) : value;
    }
}
