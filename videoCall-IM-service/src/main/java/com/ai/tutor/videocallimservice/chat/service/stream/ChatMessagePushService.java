package com.ai.tutor.videocallimservice.chat.service.stream;

import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatStreamMessageEvent;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import com.ai.tutor.videocallimservice.chat.service.adapter.MessageAdapter;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class ChatMessagePushService {

    @Resource
    private MessageMapper messageMapper;

    @Resource
    private SseSessionManager sseSessionManager;

    public boolean pushMessageById(Long msgId) {
        if (msgId == null || msgId <= 0) {
            return false;
        }
        Message msg = messageMapper.getById(msgId);
        if (msg == null || msg.getStatus() == null || msg.getStatus() != 0) {
            return false;
        }

        ChatStreamMessageEvent event = buildStreamEvent(msg);
        // 同一条消息需要同时推给发送方和接收方，这样多端列表、聊天页与状态提示才能保持一致。
        sseSessionManager.sendToUid(msg.getToUid(), "message", event);
        sseSessionManager.sendToUid(msg.getFromUid(), "message", event);
        return true;
    }

    ChatStreamMessageEvent buildStreamEvent(Message msg) {
        ChatStreamMessageEvent event = new ChatStreamMessageEvent();
        event.setMsgId(msg.getId());
        event.setRoomId(msg.getRoomId());
        event.setFromUid(msg.getFromUid());
        event.setToUid(msg.getToUid());
        event.setSendTime(toDate(msg.getCreateTime()));
        event.setBody(MessageAdapter.buildBody(msg));
        return event;
    }

    private static Date toDate(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }
}
