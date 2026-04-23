package com.ai.tutor.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class BizKpiMetricBootstrapTest {

    @Test
    void shouldRegisterZeroValueBizCountersOnStartup() {
        MeterRegistry registry = new SimpleMeterRegistry();
        ObjectProvider<MeterRegistry> registryProvider = new StaticObjectProvider<>(registry);
        ObjectProvider<BizKpiMetrics> metricsProvider = new StaticObjectProvider<>(new BizKpiMetrics(registryProvider));

        BizKpiMetricBootstrap bootstrap = new BizKpiMetricBootstrap(metricsProvider);
        bootstrap.afterSingletonsInstantiated();

        assertNotNull(registry.find(BizKpiMetrics.METRIC_USER_LOGIN_TOTAL)
                .tags("role", "teacher")
                .counter());
        assertNotNull(registry.find(BizKpiMetrics.METRIC_PAYMENT_SUCCESS_TOTAL)
                .tags("biz_type", "info_fee", "channel", "wechat")
                .counter());
        assertNotNull(registry.find(BizKpiMetrics.METRIC_CHAT_UNLOCK_TOTAL)
                .tags("unlock_reason", "payment_success")
                .counter());
        assertNotNull(registry.find(BizKpiMetrics.METRIC_REFUND_TOTAL)
                .counter());
    }

    private static final class StaticObjectProvider<T> implements ObjectProvider<T> {
        private final T value;

        private StaticObjectProvider(T value) {
            this.value = value;
        }

        @Override
        public T getObject(Object... args) {
            return value;
        }

        @Override
        public T getIfAvailable() {
            return value;
        }

        @Override
        public T getIfUnique() {
            return value;
        }

        @Override
        public T getObject() {
            return value;
        }
    }
}
