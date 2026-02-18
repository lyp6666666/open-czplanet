package com.ai.tutor.videocallimservice.chat.consumer;

import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatStreamMessageEvent;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import com.ai.tutor.videocallimservice.chat.service.stream.SseSessionManager;
import com.ai.tutor.videocallimservice.common.constant.MQConstant;
import com.ai.tutor.videocallimservice.common.domain.dto.MsgSendMessageDTO;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
@RocketMQMessageListener(consumerGroup = MQConstant.SEND_MSG_GROUP, topic = MQConstant.SEND_MSG_TOPIC)
public class MsgSendConsumer implements RocketMQListener<MsgSendMessageDTO> {

    @Resource
    private MessageMapper messageMapper;

    @Resource
    private SseSessionManager sseSessionManager;

    @Override
    public void onMessage(MsgSendMessageDTO msgSendMessageDTO) {
        if (msgSendMessageDTO == null || msgSendMessageDTO.getMsgId() == null) {
            return;
        }
        Message msg = messageMapper.getById(msgSendMessageDTO.getMsgId());
        if (msg == null || msg.getStatus() == null || msg.getStatus() != 0) {
            return;
        }

        ChatStreamMessageEvent event = new ChatStreamMessageEvent();
        event.setMsgId(msg.getId());
        event.setRoomId(msg.getRoomId());
        event.setFromUid(msg.getFromUid());
        event.setToUid(msg.getToUid());
        event.setSendTime(toDate(msg.getCreateTime()));
        event.setBody(msg.getContent());

        sseSessionManager.sendToUid(msg.getToUid(), "message", event);
        sseSessionManager.sendToUid(msg.getFromUid(), "message", event);
    }

    private static Date toDate(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }
}
