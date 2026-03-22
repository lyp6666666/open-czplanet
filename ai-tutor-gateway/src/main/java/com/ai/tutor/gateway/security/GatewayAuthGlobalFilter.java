package com.ai.tutor.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

@Component
public class GatewayAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String ADMIN_PATH_PATTERN = "/api/admin/**";

    private final JwtClaimsService jwtClaimsService;
    private final GatewaySignService signService;
    private final GatewaySignProperties signProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public GatewayAuthGlobalFilter(JwtClaimsService jwtClaimsService,
                                   GatewaySignService signService,
                                   GatewaySignProperties signProperties) {
        this.jwtClaimsService = Objects.requireNonNull(jwtClaimsService, "jwtClaimsService");
        this.signService = Objects.requireNonNull(signService, "signService");
        this.signProperties = Objects.requireNonNull(signProperties, "signProperties");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isAdminBypass(path) || isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        JwtClaimsService.JwtIdentity identity;
        try {
            identity = jwtClaimsService.parseBearerToken(authHeader);
        } catch (JwtClaimsService.JwtClaimsException ex) {
            return unauthorized(exchange.getResponse());
        }

        long ts = System.currentTimeMillis();
        String method = request.getMethod() == null ? "" : request.getMethod().name();
        String sign = signService.sign(identity.uid(), identity.role(), ts, method, path);

        ServerHttpRequest mutated = request.mutate()
                .headers(headers -> {
                    headers.remove("X-Uid");
                    headers.remove("X-Role");
                    headers.remove("X-Ts");
                    headers.remove("X-Auth-Sign");
                    headers.set("X-Uid", String.valueOf(identity.uid()));
                    headers.set("X-Role", String.valueOf(identity.role()));
                    headers.set("X-Ts", String.valueOf(ts));
                    headers.set("X-Auth-Sign", sign);
                })
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private boolean isWhitelisted(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        List<String> whitelist = signProperties.getWhitelistPaths();
        if (whitelist == null || whitelist.isEmpty()) {
            return false;
        }
        for (String pattern : whitelist) {
            if (pattern == null || pattern.trim().isEmpty()) {
                continue;
            }
            if (pathMatcher.match(pattern.trim(), path)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAdminBypass(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return pathMatcher.match(ADMIN_PATH_PATTERN, path);
    }

    private Mono<Void> unauthorized(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }
}
