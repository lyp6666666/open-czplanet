package com.ai.tutor.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutesSmokeTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void shouldResolveExpectedGatewayRoutes() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        assertThat(routes).isNotNull();

        Set<String> ids = routes.stream().map(Route::getId).collect(Collectors.toSet());
        assertThat(ids).contains(
                "appointment-user-route",
                "appointment-api-route",
                "appointment-domain-route",
                "im-chat-route",
                "payment-route"
        );

        // 企业规范：邀请有礼属于 appointment 域能力，网关必须持续暴露 /invite/** 转发入口。
        boolean hasInviteRoute = routes.stream().anyMatch(route -> route.getId().equals("appointment-domain-route")
                && String.valueOf(route.getPredicate()).contains("/invite/**"));
        assertThat(hasInviteRoute).isTrue();
    }
}
