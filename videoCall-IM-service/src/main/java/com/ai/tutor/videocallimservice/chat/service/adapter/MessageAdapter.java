package com.ai.tutor.videocallimservice.chat.service.adapter;

import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatMessageResp;
import com.ai.tutor.videocallimservice.chat.service.strategy.AbstractMsgHandler;
import com.ai.tutor.videocallimservice.chat.service.strategy.MsgHandlerFactory;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class MessageAdapter {


    public static Message buildMsgSave(ChatMessageReq request, Long uid) {
        return Message.builder()
                .fromUid(uid)
                .roomId(request.getRoomId())
                .type(request.getMsgType())
                .build();
    }

    /**
     * 批量构建聊天消息响应
     */
    public static List<ChatMessageResp> buildMsgResp(List<Message> messages, Long receiveUid) {

        return messages.stream()
                .map(MessageAdapter::buildSingleMsgResp)
                // 帮前端按时间正序排好，方便直接渲染
                .sorted(Comparator.comparing(a -> a.getMessage().getSendTime()))
                .collect(Collectors.toList());
    }

    /**
     * 构建单条 ChatMessageResp
     */
    private static ChatMessageResp buildSingleMsgResp(Message message) {
        ChatMessageResp resp = new ChatMessageResp();
        resp.setFromUser(buildFromUser(message.getFromUid()));
        resp.setMessage(buildMessage(message));
        return resp;
    }

    /**
     * 构建发送者信息
     */
    private static ChatMessageResp.UserInfo buildFromUser(Long fromUid) {
        ChatMessageResp.UserInfo userInfo = new ChatMessageResp.UserInfo();
        userInfo.setUid(fromUid);
        return userInfo;
    }

    /**
     * 构建消息体
     */
    private static ChatMessageResp.Message buildMessage(Message message) {
        ChatMessageResp.Message msg = new ChatMessageResp.Message();
        msg.setId(message.getId());
        msg.setRoomId(message.getRoomId());
        msg.setSendTime(toDate(message.getCreateTime()));
        msg.setBody(buildBody(message));
        return msg;
    }

    /**
     * 构建消息内容（按消息类型返回结构化消息体）。
     */
    public static Object buildBody(Message message) {
        if (message == null || message.getType() == null) {
            return null;
        }
        AbstractMsgHandler<?> handler = MsgHandlerFactory.getStrategyNoNull(message.getType());
        return handler.showMsg(message);
    }

    /**
     * 会话列表预览消息（用于 lastMsgBody 等场景）。
     */
    public static String buildContactPreview(Message message) {
        if (message == null || message.getType() == null) {
            return "";
        }
        AbstractMsgHandler<?> handler = MsgHandlerFactory.getStrategyNoNull(message.getType());
        return handler.showContactMsg(message);
    }

    /**
     * LocalDateTime → Date
     */
    private static Date toDate(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }


}
