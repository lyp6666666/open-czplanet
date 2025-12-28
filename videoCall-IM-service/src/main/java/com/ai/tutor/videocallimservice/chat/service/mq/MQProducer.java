package com.ai.tutor.videocallimservice.chat.service.mq;

import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;


@Component
public class MQProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public void sendMsg(String topic, Object body) {
        Message<Object> build = MessageBuilder.withPayload(body).build();
        rocketMQTemplate.send(topic, build);
    }

    // todo 自定义注解
    public void sendSecureMsg(String topic, Object body, Object key) {
        Message<Object> build = MessageBuilder
                .withPayload(body)
                .setHeader("KEYS", key)
                .build();
        rocketMQTemplate.send(topic, build);
    }

}
