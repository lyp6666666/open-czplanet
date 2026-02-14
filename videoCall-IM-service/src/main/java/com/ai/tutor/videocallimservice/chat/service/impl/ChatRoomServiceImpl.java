package com.ai.tutor.videocallimservice.chat.service.impl;

import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatRoomPageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatRoomItemResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CursorPageResp;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.service.ChatRoomService;
import com.ai.tutor.videocallimservice.common.domain.entity.ImUser;
import com.ai.tutor.videocallimservice.common.mapper.ImUserMapper;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ChatRoomServiceImpl implements ChatRoomService {

    @Resource
    private RoomMapper roomMapper;

    @Resource
    private MessageMapper messageMapper;

    @Resource
    private ImUserMapper imUserMapper;

    @Override
    public Long getOrCreateRoomWithUser(Long targetUid, Long uid) {
        ThrowUtils.throwIf(uid == null || targetUid == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(uid.equals(targetUid), ErrorCode.PARAMS_ERROR);

        ImUser self = requireActiveUser(uid);
        ImUser target = requireActiveUser(targetUid);

        Long teacherProfileId;
        Long studentProfileId;
        if (Integer.valueOf(1).equals(self.getUserType())) {
            ThrowUtils.throwIf(!Integer.valueOf(2).equals(target.getUserType()), ErrorCode.PARAMS_ERROR);
            teacherProfileId = self.getRefId();
            studentProfileId = target.getRefId();
        } else if (Integer.valueOf(2).equals(self.getUserType())) {
            ThrowUtils.throwIf(!Integer.valueOf(1).equals(target.getUserType()), ErrorCode.PARAMS_ERROR);
            teacherProfileId = target.getRefId();
            studentProfileId = self.getRefId();
        } else {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR);
            return null;
        }

        ThrowUtils.throwIf(teacherProfileId == null || studentProfileId == null, ErrorCode.PARAMS_ERROR);

        Room existing = roomMapper.selectByTeacherAndStudent(teacherProfileId, studentProfileId);
        if (existing != null) {
            return existing.getId();
        }

        LocalDateTime now = LocalDateTime.now();
        Room room = Room.builder()
                .teacherProfileId(teacherProfileId)
                .studentProfileId(studentProfileId)
                .activeTime(now)
                .lastMsgId(null)
                .status(1)
                .createTime(now)
                .updateTime(now)
                .build();
        try {
            int inserted = roomMapper.insert(room);
            ThrowUtils.throwIf(inserted <= 0 || room.getId() == null, ErrorCode.OPERATION_ERROR);
            return room.getId();
        } catch (DuplicateKeyException e) {
            Room latest = roomMapper.selectByTeacherAndStudent(teacherProfileId, studentProfileId);
            ThrowUtils.throwIf(latest == null, ErrorCode.OPERATION_ERROR);
            return latest.getId();
        }
    }

    @Override
    public CursorPageResp<ChatRoomItemResp> listRooms(ChatRoomPageReq request, Long uid) {
        ThrowUtils.throwIf(request == null || uid == null, ErrorCode.PARAMS_ERROR);
        ImUser self = requireActiveUser(uid);
        ThrowUtils.throwIf(self.getRefId() == null, ErrorCode.PARAMS_ERROR);

        Integer pageSize = request.getPageSize();
        List<Room> rooms;
        if (Integer.valueOf(1).equals(self.getUserType())) {
            rooms = roomMapper.listByTeacherProfileId(self.getRefId(), request.getCursor(), pageSize);
        } else if (Integer.valueOf(2).equals(self.getUserType())) {
            rooms = roomMapper.listByStudentProfileId(self.getRefId(), request.getCursor(), pageSize);
        } else {
            return CursorPageResp.empty();
        }

        List<ChatRoomItemResp> list = new ArrayList<>();
        for (Room room : rooms) {
            Long otherUid = Integer.valueOf(1).equals(self.getUserType())
                    ? resolveUserId(2, room.getStudentProfileId())
                    : resolveUserId(1, room.getTeacherProfileId());

            Message lastMsg = room.getLastMsgId() == null ? null : messageMapper.getById(room.getLastMsgId());
            Object lastBody = lastMsg == null ? null : lastMsg.getContent();

            list.add(ChatRoomItemResp.builder()
                    .roomId(room.getId())
                    .otherUid(otherUid)
                    .lastMsgId(room.getLastMsgId())
                    .lastMsgBody(lastBody)
                    .activeTime(toDate(room.getActiveTime()))
                    .build());
        }

        Long nextCursor = list.isEmpty() ? null : list.get(list.size() - 1).getRoomId();
        boolean isLast = rooms.size() < pageSize;
        return new CursorPageResp<>(nextCursor, isLast, list);
    }

    private ImUser requireActiveUser(Long uid) {
        ImUser user = imUserMapper.selectById(uid);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(user.getStatus() != null && user.getStatus() == 1, ErrorCode.NO_AUTH_ERROR);
        return user;
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

    private static Date toDate(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }
}
