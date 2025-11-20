package com.ai.tutor.videocallimservice.chat.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.ai.tutor.videocallimservice.chat.dao.MessageDao;

import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatMessageResp;
import com.ai.tutor.videocallimservice.chat.service.ChatService;
import com.ai.tutor.videocallimservice.chat.service.adapter.MessageAdapter;
import com.ai.tutor.videocallimservice.chat.service.strategy.msg.AbstractMsgHandler;
import com.ai.tutor.videocallimservice.chat.service.strategy.msg.MsgHandlerFactory;
import com.ai.tutor.videocallimservice.common.event.MessageSendEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Resource
    private ApplicationEventPublisher applicationEventPublisher;
    @Resource
    private MessageDao messageDao;


    @Override
    public ChatMessageResp getMsgResp(Long msgId, Long receiveUid) {
        Message msg = messageDao.getById(msgId);

        if (msg == null) {
            return null;
        }

        List<Message> messages = Collections.singletonList(msg);

        // 查询消息标志
        List<MessageMark> msgMark = messageMarkDao.getValidMarkByMsgIdBatch(
                messages.stream().map(Message::getId).collect(Collectors.toList())
        );

        // 使用 MessageAdapter 构建消息响应
        List<ChatMessageResp> respList = MessageAdapter.buildMsgResp(messages, msgMark, receiveUid);

        return CollUtil.getFirst(respList);
    }



    @Transactional
    @Override
    public Long sendMsg(ChatMessageReq request, Long uid) {
        AbstractMsgHandler<?> msgHandler = MsgHandlerFactory.getStrategyNoNull(request.getMsgType());
        Long msgId = msgHandler.checkAndSaveMsg(request, uid);
        //发布消息发送事件
        applicationEventPublisher.publishEvent(new MessageSendEvent(this, msgId));
        return msgId;
    }


}
