package com.ai.tutor.common.security;

import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.utils.RequestHolder;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FeignIdentityRequestInterceptorTest {
    private static final String STRONG_SECRET = "0123456789abcdef0123456789abcdef";

    @AfterEach
    void tearDown() {
        RequestHolder.remove();
    }

    @Test
    void shouldCanonicalizeQueriesBeforeSigning() {
        IdentitySignProperties properties = new IdentitySignProperties();
        properties.setSecret(STRONG_SECRET);
        IdentitySignatureUtils utils = new IdentitySignatureUtils(properties);
        FeignIdentityRequestInterceptor interceptor = new FeignIdentityRequestInterceptor(utils);

        RequestInfo info = new RequestInfo();
        info.setUid(206L);
        info.setRole(1);
        RequestHolder.set(info);

        RequestTemplate template = new RequestTemplate();
        template.method("GET");
        template.uri("/api/v1/test");
        template.query("b", "2", "1");
        template.query("a", "z", "y");

        interceptor.apply(template);

        Map<String, Collection<String>> queries = template.queries();
        List<String> keys = new ArrayList<>(queries.keySet());
        assertEquals(List.of("a", "b"), keys);
        assertEquals(List.of("y", "z"), new ArrayList<>(queries.get("a")));
        assertEquals(List.of("1", "2"), new ArrayList<>(queries.get("b")));

        String tsHeader = headerValue(template, "X-Ts");
        String signHeader = headerValue(template, "X-Auth-Sign");
        long ts = Long.parseLong(tsHeader);
        String expectedTarget = "/api/v1/test?a=y&a=z&b=1&b=2";
        String expectedSign = utils.sign(206L, 1, ts, "GET", expectedTarget);
        assertEquals(expectedSign, signHeader);
    }

    private String headerValue(RequestTemplate template, String name) {
        Collection<String> values = template.headers().get(name);
        assertNotNull(values);
        return values.iterator().next();
    }
}
