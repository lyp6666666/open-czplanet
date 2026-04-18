package com.ai.tutor.videocallimservice.chat.consumer;

import com.ai.tutor.videocallimservice.chat.service.stream.ChatMessagePushService;
import com.ai.tutor.videocallimservice.common.constant.MQConstant;
import com.ai.tutor.videocallimservice.common.domain.dto.MsgSendMessageDTO;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(consumerGroup = MQConstant.SEND_MSG_GROUP, topic = MQConstant.SEND_MSG_TOPIC)
public class MsgSendConsumer implements RocketMQListener<MsgSendMessageDTO> {

    @Resource
    private ChatMessagePushService chatMessagePushService;

    @Override
    public void onMessage(MsgSendMessageDTO msgSendMessageDTO) {
        if (msgSendMessageDTO == null || msgSendMessageDTO.getMsgId() == null) {
            return;
        }
        chatMessagePushService.pushMessageById(msgSendMessageDTO.getMsgId());
    }
}
