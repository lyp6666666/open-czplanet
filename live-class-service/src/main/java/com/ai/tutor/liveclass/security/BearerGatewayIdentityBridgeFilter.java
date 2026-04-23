package com.ai.tutor.liveclass.security;

import com.ai.tutor.common.security.IdentitySignatureUtils;
import com.ai.tutor.liveclass.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class BearerGatewayIdentityBridgeFilter extends OncePerRequestFilter {

    private final JwtProperties jwtProperties;
    private final IdentitySignatureUtils signatureUtils;

    public BearerGatewayIdentityBridgeFilter(JwtProperties jwtProperties, IdentitySignatureUtils signatureUtils) {
        this.jwtProperties = jwtProperties;
        this.signatureUtils = signatureUtils;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith("/live/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        log.info("live-auth bridge path={} query={} auth={} xUid={} xRole={} xTs={} xSign={}",
                request.getRequestURI(),
                request.getQueryString(),
                StringUtils.hasText(request.getHeader("Authorization")),
                request.getHeader("X-Uid"),
                request.getHeader("X-Role"),
                request.getHeader("X-Ts"),
                request.getHeader("X-Auth-Sign"));
        if (hasGatewayIdentityHeaders(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.trim().regionMatches(true, 0, "Bearer ", 0, 7)) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims;
        try {
            claims = parseClaims(authHeader.trim().substring(7).trim());
        } catch (JwtException | IllegalArgumentException ex) {
            filterChain.doFilter(request, response);
            return;
        }

        Long uid = extractUid(claims);
        Integer role = extractRole(claims);
        if (uid == null || role == null) {
            filterChain.doFilter(request, response);
            return;
        }

        long ts = System.currentTimeMillis();
        String method = request.getMethod();
        String requestTarget = resolveRequestTarget(request);
        String sign = signatureUtils.sign(uid, role, ts, method, requestTarget);

        MutableHeaderRequestWrapper wrapped = new MutableHeaderRequestWrapper(request);
        wrapped.putHeader("X-Uid", String.valueOf(uid));
        wrapped.putHeader("X-Role", String.valueOf(role));
        wrapped.putHeader("X-Ts", String.valueOf(ts));
        wrapped.putHeader("X-Auth-Sign", sign);
        filterChain.doFilter(wrapped, response);
    }

    private boolean hasGatewayIdentityHeaders(HttpServletRequest request) {
        return StringUtils.hasText(request.getHeader("X-Uid"))
                && StringUtils.hasText(request.getHeader("X-Role"))
                && StringUtils.hasText(request.getHeader("X-Ts"))
                && StringUtils.hasText(request.getHeader("X-Auth-Sign"));
    }

    private Claims parseClaims(String token) {
        List<String> secrets = jwtProperties.getSecrets();
        JwtException last = null;
        for (String secret : secrets) {
            if (!StringUtils.hasText(secret)) continue;
            try {
                SecretKey key = Keys.hmacShaKeyFor(secret.trim().getBytes(StandardCharsets.UTF_8));
                var parser = Jwts.parserBuilder().setSigningKey(key);
                if (StringUtils.hasText(jwtProperties.getIssuer())) {
                    parser.requireIssuer(jwtProperties.getIssuer().trim());
                }
                return parser.build().parseClaimsJws(token).getBody();
            } catch (JwtException ex) {
                last = ex;
            }
        }
        if (last != null) throw last;
        throw new JwtException("JWT secrets not configured");
    }

    private Long extractUid(Claims claims) {
        Object raw = claims.get("userId");
        if (raw == null) raw = claims.get("uid");
        if (raw instanceof Number number) return number.longValue();
        if (!StringUtils.hasText(raw == null ? null : raw.toString())) return null;
        try {
            return Long.parseLong(raw.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer extractRole(Claims claims) {
        Object raw = claims.get("role");
        if (raw instanceof Number number) return normalizeRole(number.intValue());
        if (!StringUtils.hasText(raw == null ? null : raw.toString())) return null;
        String value = raw.toString().trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "teacher", "1" -> 1;
            case "student", "2" -> 2;
            case "org", "3" -> 3;
            default -> null;
        };
    }

    private Integer normalizeRole(int value) {
        if (value == 1 || value == 2 || value == 3) return value;
        return null;
    }

    private String resolveRequestTarget(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        if (StringUtils.hasText(query)) {
            return uri + "?" + query;
        }
        return uri;
    }

    private static final class MutableHeaderRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> customHeaders = new LinkedHashMap<>();

        private MutableHeaderRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        private void putHeader(String name, String value) {
            customHeaders.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            String value = customHeaders.get(name);
            return value != null ? value : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String value = customHeaders.get(name);
            if (value == null) {
                return super.getHeaders(name);
            }
            return Collections.enumeration(List.of(value));
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>(customHeaders.keySet());
            Enumeration<String> original = super.getHeaderNames();
            while (original.hasMoreElements()) {
                names.add(original.nextElement());
            }
            return Collections.enumeration(names);
        }
    }
}
