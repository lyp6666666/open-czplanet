package com.ai.tutor.videocallimservice.common.config;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RocketMQManualConfig {

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Bean
    public DefaultMQProducer defaultMQProducer() {
        DefaultMQProducer producer = new DefaultMQProducer("videoCall-im-producer");
        producer.setNamesrvAddr(nameServer);
        return producer;
    }

    @Bean
    public RocketMQTemplate rocketMQTemplate(DefaultMQProducer producer) {
        RocketMQTemplate template = new RocketMQTemplate();
        template.setProducer(producer);
        return template;
    }
}
