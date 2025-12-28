package com.ai.tutor.videocallimservice.chat.consumer;

import com.ai.tutor.videocallimservice.common.constant.MQConstant;
import com.ai.tutor.videocallimservice.common.domain.dto.MsgSendMessageDTO;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;


@Component
@RocketMQMessageListener(consumerGroup = MQConstant.SEND_MSG_GROUP, topic = MQConstant.SEND_MSG_TOPIC)
public class MsgSendConsumer implements RocketMQListener<MsgSendMessageDTO> {

    @Override
    public void onMessage(MsgSendMessageDTO msgSendMessageDTO) {

    }
}
