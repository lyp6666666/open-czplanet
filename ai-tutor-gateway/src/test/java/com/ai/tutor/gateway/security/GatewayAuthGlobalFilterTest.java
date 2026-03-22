package com.ai.tutor.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "gateway.jwt.secrets[0]=test-jwt-secret-1234567890-test-jwt-secret-1234567890",
        "gateway.jwt.issuer=ai-tutor",
        "gateway.sign.secret=0123456789abcdef0123456789abcdef",
        "gateway.sign.whitelist-paths[0]=/api/v1/public/**"
})
@Import(GatewayAuthGlobalFilterTest.TestConfig.class)
class GatewayAuthGlobalFilterTest {

    private static final String JWT_SECRET = "test-jwt-secret-1234567890-test-jwt-secret-1234567890";
    private static final String JWT_ISSUER = "ai-tutor";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private GatewaySignService signService;

    @Test
    void shouldRejectProtectedPathWithoutToken() {
        webTestClient.get()
                .uri("/chat/room/page?pageSize=10")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldAllowWhitelistedPathWithoutToken() {
        webTestClient.get()
                .uri("/api/v1/public/home/config")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldInjectHeadersForValidToken() {
        String token = tokenWithClaims(206L, 1);

        Map<String, String> body = webTestClient.get()
                .uri("/chat/room/page?pageSize=10")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-Uid", "999")
                .header("X-Role", "9")
                .header("X-Ts", "0")
                .header("X-Auth-Sign", "bad")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, String>>() {})
                .returnResult()
                .getResponseBody();

        assertNotNull(body);
        assertEquals("206", body.get("uid"));
        assertEquals("1", body.get("role"));

        String ts = body.get("ts");
        assertNotNull(ts);
        long timestamp = Long.parseLong(ts);

        String sign = body.get("sign");
        assertNotNull(sign);
        String expected = signService.sign(206L, 1, timestamp, "GET", "/chat/room/page");
        assertEquals(expected, sign);
    }

    private String tokenWithClaims(long uid, int role) {
        Key key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", uid);
        claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuer(JWT_ISSUER)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        RouteLocator testRoutes(RouteLocatorBuilder builder) {
            return builder.routes()
                    .route("chat-route", r -> r.path("/chat/**")
                            .uri("forward:/__internal__/echo"))
                    .route("api-route", r -> r.path("/api/**")
                            .uri("forward:/__internal__/echo"))
                    .build();
        }

        @Bean
        RouterFunction<ServerResponse> echoRouter() {
            return RouterFunctions.route(RequestPredicates.GET("/__internal__/echo"), this::echo);
        }

        private Mono<ServerResponse> echo(ServerRequest request) {
            HttpHeaders headers = request.headers().asHttpHeaders();
            Map<String, String> body = new HashMap<>();
            body.put("uid", headers.getFirst("X-Uid"));
            body.put("role", headers.getFirst("X-Role"));
            body.put("ts", headers.getFirst("X-Ts"));
            body.put("sign", headers.getFirst("X-Auth-Sign"));
            return ServerResponse.ok().bodyValue(body);
        }
    }
}
