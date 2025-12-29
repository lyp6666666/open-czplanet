package com.ai.tutor.videocallimservice.chat.service.impl;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessagePageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.CursorPageBaseReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatMessageResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CursorPageBaseResp;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import com.ai.tutor.videocallimservice.chat.service.ChatService;
import com.ai.tutor.videocallimservice.chat.service.adapter.MessageAdapter;
import com.ai.tutor.videocallimservice.chat.service.strategy.AbstractMsgHandler;
import com.ai.tutor.videocallimservice.chat.service.strategy.MsgHandlerFactory;
import com.ai.tutor.videocallimservice.common.event.MessageSendEvent;
import com.ai.tutor.videocallimservice.common.util.CursorUtils;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Service
@Slf4j
public class ChatServiceImpl extends ServiceImpl<MessageMapper, Message> implements ChatService {


    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public CursorPageBaseResp<ChatMessageResp> getMsgPage(ChatMessagePageReq request, Long receiveUid) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "request 为空");
        ThrowUtils.throwIf(receiveUid == null, ErrorCode.PARAMS_ERROR, "receiveUid为空");
        CursorPageBaseResp<Message> cursorPage = getCursorPage(request.getRoomId(), request);
        if (cursorPage.isEmpty()) {
            return CursorPageBaseResp.empty();
        }
        return CursorPageBaseResp.init(cursorPage, getMsgRespBatch(cursorPage.getList(), receiveUid));
    }

    private CursorPageBaseResp<Message> getCursorPage(Long roomId, CursorPageBaseReq request) {
        return CursorUtils.getCursorPageByMysql(
                this, // IService<Message>
                request,
                wrapper -> {
                    wrapper.eq(Message::getRoomId, roomId);
                    wrapper.eq(Message::getStatus, 0); // 只查正常消息
                },
                Message::getId // 游标字段：message.id
        );
    }

    @Override
    public Long sendMsg(ChatMessageReq request, Long uid) {
        //todo 校验是否能发送消息
        //根据消息类型，得到专门处理该消息的处理器
        AbstractMsgHandler<?> msgHandler = MsgHandlerFactory.getStrategyNoNull(request.getMsgType());
        Long msgId = msgHandler.checkAndSaveMsg(request, uid);
        applicationEventPublisher.publishEvent(new MessageSendEvent(this, msgId));
        return msgId;
    }


    public ChatMessageResp getMsgResp(Message message, Long receiveUid) {
        return CollUtil.getFirst(getMsgRespBatch(Collections.singletonList(message), receiveUid));
    }

    @Override
    public ChatMessageResp getMsgResp(Long msgId, Long receiveUid) {
        Message msg = messageMapper.getById(msgId);
        return getMsgResp(msg, receiveUid);
    }

    public List<ChatMessageResp> getMsgRespBatch(List<Message> messages, Long receiveUid) {
        if (CollectionUtil.isEmpty(messages)) {
            return new ArrayList<>();
        }
        return MessageAdapter.buildMsgResp(messages, receiveUid);
    }
}
