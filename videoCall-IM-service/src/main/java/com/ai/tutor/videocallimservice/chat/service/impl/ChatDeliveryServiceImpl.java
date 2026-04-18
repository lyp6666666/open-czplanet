package com.ai.tutor.videocallimservice.chat.service.impl;

import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatDeliveryAckReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatStreamDeliveryEvent;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.service.ChatDeliveryService;
import com.ai.tutor.videocallimservice.chat.service.stream.SseSessionManager;
import com.ai.tutor.videocallimservice.common.domain.entity.ImUser;
import com.ai.tutor.videocallimservice.common.mapper.ImUserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ChatDeliveryServiceImpl implements ChatDeliveryService {

    @Resource
    private RoomMapper roomMapper;

    @Resource
    private MessageMapper messageMapper;

    @Resource
    private ImUserMapper imUserMapper;

    @Resource
    private SseSessionManager sseSessionManager;

    @Override
    public void ackDelivered(ChatDeliveryAckReq request, Long uid) {
        ThrowUtils.throwIf(request == null || uid == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getRoomId() == null || request.getLastDeliveredMsgId() == null, ErrorCode.PARAMS_ERROR);

        Room room = roomMapper.selectById(request.getRoomId());
        ThrowUtils.throwIf(room == null || room.getStatus() == null || room.getStatus() != 1, ErrorCode.NOT_FOUND_ERROR);

        Long teacherUid = resolveUserId(1, room.getTeacherProfileId());
        Long studentUid = resolveUserId(2, room.getStudentProfileId());
        ThrowUtils.throwIf(teacherUid == null || studentUid == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(teacherUid) && !uid.equals(studentUid), ErrorCode.NO_AUTH_ERROR);

        Message msg = messageMapper.getById(request.getLastDeliveredMsgId());
        ThrowUtils.throwIf(msg == null || msg.getStatus() == null || msg.getStatus() != 0, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!request.getRoomId().equals(msg.getRoomId()), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!uid.equals(msg.getToUid()), ErrorCode.NO_AUTH_ERROR, "只能确认自己收到的消息");

        Long peerUid = uid.equals(teacherUid) ? studentUid : teacherUid;
        ThrowUtils.throwIf(peerUid == null || peerUid.equals(uid), ErrorCode.PARAMS_ERROR);

        ChatStreamDeliveryEvent deliveryEvent = new ChatStreamDeliveryEvent();
        deliveryEvent.setRoomId(request.getRoomId());
        deliveryEvent.setDeliverUid(uid);
        deliveryEvent.setLastDeliveredMsgId(request.getLastDeliveredMsgId());
        sseSessionManager.sendToUid(peerUid, "delivery", deliveryEvent);
    }

    private Long resolveUserId(int userType, Long refId) {
        if (refId == null) {
            return null;
        }
        ImUser user = imUserMapper.selectByUserTypeAndRefId(userType, refId);
        if (user == null) {
            return null;
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            return null;
        }
        return user.getId();
    }
}
