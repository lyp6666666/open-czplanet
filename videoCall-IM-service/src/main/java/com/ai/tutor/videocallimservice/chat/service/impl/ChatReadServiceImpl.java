package com.ai.tutor.videocallimservice.chat.service.impl;

import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatReadAckReq;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomReadStateMapper;
import com.ai.tutor.videocallimservice.chat.service.ChatReadService;
import com.ai.tutor.videocallimservice.common.domain.entity.ImUser;
import com.ai.tutor.videocallimservice.common.mapper.ImUserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ChatReadServiceImpl implements ChatReadService {

    @Resource
    private RoomMapper roomMapper;

    @Resource
    private MessageMapper messageMapper;

    @Resource
    private RoomReadStateMapper roomReadStateMapper;

    @Resource
    private ImUserMapper imUserMapper;

    @Override
    public void ackRead(ChatReadAckReq request, Long uid) {
        ThrowUtils.throwIf(request == null || uid == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getRoomId() == null || request.getLastReadMsgId() == null, ErrorCode.PARAMS_ERROR);

        Room room = roomMapper.selectById(request.getRoomId());
        ThrowUtils.throwIf(room == null || room.getStatus() == null, ErrorCode.NOT_FOUND_ERROR);

        Long teacherUid = resolveUserId(1, room.getTeacherProfileId());
        Long studentUid = resolveUserId(2, room.getStudentProfileId());
        ThrowUtils.throwIf(teacherUid == null || studentUid == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(teacherUid) && !uid.equals(studentUid), ErrorCode.NO_AUTH_ERROR);

        Message msg = messageMapper.getById(request.getLastReadMsgId());
        ThrowUtils.throwIf(msg == null || msg.getStatus() == null || msg.getStatus() != 0, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!request.getRoomId().equals(msg.getRoomId()), ErrorCode.PARAMS_ERROR);

        try {
            roomReadStateMapper.upsertReadState(request.getRoomId(), uid, request.getLastReadMsgId());
        } catch (Exception e) {
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "已读上报失败");
        }
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
