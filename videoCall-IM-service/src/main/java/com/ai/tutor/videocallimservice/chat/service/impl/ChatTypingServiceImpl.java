package com.ai.tutor.videocallimservice.chat.service.impl;

import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatTypingReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatStreamTypingEvent;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.service.ChatTypingService;
import com.ai.tutor.videocallimservice.chat.service.stream.SseSessionManager;
import com.ai.tutor.videocallimservice.common.domain.entity.ImUser;
import com.ai.tutor.videocallimservice.common.mapper.ImUserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ChatTypingServiceImpl implements ChatTypingService {

    @Resource
    private RoomMapper roomMapper;

    @Resource
    private ImUserMapper imUserMapper;

    @Resource
    private SseSessionManager sseSessionManager;

    @Override
    public void reportTyping(ChatTypingReq request, Long uid) {
        ThrowUtils.throwIf(request == null || uid == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getRoomId() == null || request.getTyping() == null, ErrorCode.PARAMS_ERROR);

        Room room = roomMapper.selectById(request.getRoomId());
        ThrowUtils.throwIf(room == null || room.getStatus() == null || room.getStatus() != 1, ErrorCode.NOT_FOUND_ERROR);

        Long teacherUid = resolveUserId(1, room.getTeacherProfileId());
        Long studentUid = resolveUserId(2, room.getStudentProfileId());
        ThrowUtils.throwIf(teacherUid == null || studentUid == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(teacherUid) && !uid.equals(studentUid), ErrorCode.NO_AUTH_ERROR);

        Long peerUid = uid.equals(teacherUid) ? studentUid : teacherUid;
        ThrowUtils.throwIf(peerUid == null || peerUid.equals(uid), ErrorCode.PARAMS_ERROR);

        ChatStreamTypingEvent typingEvent = new ChatStreamTypingEvent();
        typingEvent.setRoomId(request.getRoomId());
        typingEvent.setTypingUid(uid);
        typingEvent.setTyping(request.getTyping());
        sseSessionManager.sendEphemeralToUid(peerUid, "typing", typingEvent);
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
