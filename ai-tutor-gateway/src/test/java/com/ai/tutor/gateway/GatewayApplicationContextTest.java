package com.ai.tutor.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayApplicationContextTest {

    @Autowired
    private ApplicationContext ctx;

    @Test
    void shouldLoadGatewayContext() {
        assertNotNull(ctx.getBean(RouteLocator.class));
    }
}
