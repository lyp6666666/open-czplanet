package com.ai.tutor.videocallimservice.chat.service.impl;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.entity.TutorApplication;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageSearchReq;
import com.ai.tutor.videocallimservice.chat.domain.enums.TutorApplicationChatAccessStatus;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.SystemMsgReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessagePageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.CursorPageBaseReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatMessageResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CursorPageBaseResp;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.mapper.TutorApplicationMapper;
import com.ai.tutor.videocallimservice.chat.service.ChatService;
import com.ai.tutor.videocallimservice.chat.service.adapter.MessageAdapter;
import com.ai.tutor.videocallimservice.chat.service.strategy.AbstractMsgHandler;
import com.ai.tutor.videocallimservice.chat.service.strategy.MsgHandlerFactory;
import com.ai.tutor.videocallimservice.common.event.MessageSendEvent;
import com.ai.tutor.videocallimservice.common.util.CursorUtils;
import com.ai.tutor.videocallimservice.common.mapper.StudentProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.mapper.TeacherProfileLiteMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
public class ChatServiceImpl extends ServiceImpl<MessageMapper, Message> implements ChatService {


    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private TutorApplicationMapper tutorApplicationMapper;

    @Autowired
    private TeacherProfileLiteMapper teacherProfileLiteMapper;

    @Autowired
    private StudentProfileLiteMapper studentProfileLiteMapper;

    @Value("${tutor-application.skip-payment-check:false}")
    private boolean skipPaymentCheck;

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

    @Override
    public CursorPageBaseResp<ChatMessageResp> searchMsgPage(ChatMessageSearchReq request, Long receiveUid) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "request 为空");
        ThrowUtils.throwIf(receiveUid == null, ErrorCode.PARAMS_ERROR, "receiveUid为空");
        String keyword = request.getKeyword() == null ? "" : request.getKeyword().trim();
        ThrowUtils.throwIf(keyword.isEmpty(), ErrorCode.PARAMS_ERROR, "关键词不能为空");
        request.setKeyword(keyword);

        CursorPageBaseResp<Message> cursorPage = messageMapper.searchCursorPage(request.getRoomId(), request);
        if (cursorPage == null || cursorPage.isEmpty()) {
            return CursorPageBaseResp.empty();
        }
        List<ChatMessageResp> respList = getMsgRespBatch(cursorPage.getList(), receiveUid);
        // 搜索结果更适合按“最新命中优先”展示，方便前端直接渲染结果列表。
        respList.sort((a, b) -> Long.compare(b.getMessage().getId(), a.getMessage().getId()));
        return CursorPageBaseResp.init(cursorPage, respList);
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
        assertCanSend(request, uid);
        //根据消息类型，得到专门处理该消息的处理器
        AbstractMsgHandler<?> msgHandler = MsgHandlerFactory.getStrategyNoNull(request.getMsgType());
        Long msgId = msgHandler.checkAndSaveMsg(request, uid);
        roomMapper.updateAfterSend(request.getRoomId(), msgId);
        if (Integer.valueOf(8).equals(request.getMsgType())) {
            String bizType = extractBizType(request.getBody());
            if ("BROKERAGE_REFUND_REQUEST".equals(bizType)) {
                roomMapper.closeRoom(request.getRoomId());
            }
        }
        applicationEventPublisher.publishEvent(new MessageSendEvent(this, msgId));
        return msgId;
    }

    private void assertCanSend(ChatMessageReq request, Long uid) {
        ThrowUtils.throwIf(request == null || uid == null, ErrorCode.PARAMS_ERROR);
        Integer msgType = request.getMsgType();
        Long roomId = request.getRoomId();
        ThrowUtils.throwIf(msgType == null || roomId == null, ErrorCode.PARAMS_ERROR);

        boolean unlocked = isChatEnabled(roomId);
        if (Integer.valueOf(1).equals(msgType)) {
            ThrowUtils.throwIf(!unlocked, ErrorCode.OPERATION_ERROR, "当前仅可发送家教申请，申请通过并完成支付后再聊天");
            return;
        }
        if (!Integer.valueOf(8).equals(msgType)) {
            ThrowUtils.throwIf(!unlocked, ErrorCode.OPERATION_ERROR, "当前仅可发送家教申请，申请通过并完成支付后再聊天");
            return;
        }

        String bizType = extractBizType(request.getBody());
        if (!unlocked) {
            boolean allowed = "TUTOR_APPLICATION".equals(bizType)
                    || "TUTOR_APPLICATION_STATUS".equals(bizType)
                    || "BROKERAGE_REQUIRED".equals(bizType)
                    || "CONTACT_UNLOCKED".equals(bizType)
                    || "LESSON_REQUEST".equals(bizType)
                    || "LESSON_STATUS".equals(bizType);
            ThrowUtils.throwIf(!allowed, ErrorCode.OPERATION_ERROR, "当前仅可发送家教申请");
        }
    }

    private boolean isChatEnabled(Long roomId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null || room.getStatus() == null || room.getStatus() != 1) {
            return false;
        }
        Long teacherUid = room.getTeacherProfileId() == null ? null : teacherProfileLiteMapper.selectUserIdById(room.getTeacherProfileId());
        Long studentUid = room.getStudentProfileId() == null ? null : studentProfileLiteMapper.selectUserIdById(room.getStudentProfileId());
        if (teacherUid == null || studentUid == null) {
            return false;
        }
        TutorApplication application = tutorApplicationMapper.selectLatestAcceptedBetween(teacherUid, studentUid);
        if (application == null) {
            return false;
        }
        String access = application.getChatAccessStatus();
        if (skipPaymentCheck && TutorApplicationChatAccessStatus.PAYMENT_REQUIRED.name().equals(access)) {
            return true;
        }
        return TutorApplicationChatAccessStatus.CHAT_ENABLED.name().equals(access);
    }

    private static String extractBizType(Object body) {
        if (body instanceof SystemMsgReq) {
            SystemMsgReq req = (SystemMsgReq) body;
            return req.getBizType() == null ? "" : req.getBizType().trim().toUpperCase();
        }
        if (body instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) body;
            Object v = map.get("bizType");
            if (v == null) v = map.get("biz_type");
            return v == null ? "" : String.valueOf(v).trim().toUpperCase();
        }
        return "";
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
