package com.ai.tutor.payment.strategy;

import com.ai.tutor.payment.enums.PaymentChannel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 支付策略工厂
 */
@Component
@RequiredArgsConstructor
public class PaymentStrategyFactory {

    private final List<PaymentStrategy> strategies;
    private Map<String, PaymentStrategy> strategyMap;

    @PostConstruct
    public void init() {
        strategyMap = strategies.stream()
                .collect(Collectors.toMap(PaymentStrategy::getChannel, Function.identity()));
    }

    public PaymentStrategy getStrategy(String channel) {
        return Optional.ofNullable(strategyMap.get(channel))
                .orElseThrow(() -> new IllegalArgumentException("不支持的支付渠道: " + channel));
    }
}
