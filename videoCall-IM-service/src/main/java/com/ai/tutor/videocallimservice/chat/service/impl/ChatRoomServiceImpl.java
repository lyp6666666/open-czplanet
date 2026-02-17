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
import com.ai.tutor.videocallimservice.common.mapper.StudentProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.mapper.TeacherProfileLiteMapper;
import com.ai.tutor.utils.RequestHolder;
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

    @Resource
    private TeacherProfileLiteMapper teacherProfileLiteMapper;

    @Resource
    private StudentProfileLiteMapper studentProfileLiteMapper;

    @Override
    public Long getOrCreateRoomWithUser(Long targetUid, Long uid) {
        ThrowUtils.throwIf(uid == null || targetUid == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(uid.equals(targetUid), ErrorCode.PARAMS_ERROR);

        requireActiveUser(uid);
        requireActiveUser(targetUid);

        Integer role = RequestHolder.get() == null ? null : RequestHolder.get().getRole();
        ThrowUtils.throwIf(role == null, ErrorCode.PARAMS_ERROR);

        Long teacherProfileId;
        Long studentProfileId;
        if (Integer.valueOf(1).equals(role)) {
            teacherProfileId = teacherProfileLiteMapper.selectIdByUserId(uid);
            studentProfileId = studentProfileLiteMapper.selectIdByUserId(targetUid);
        } else if (Integer.valueOf(2).equals(role)) {
            teacherProfileId = teacherProfileLiteMapper.selectIdByUserId(targetUid);
            studentProfileId = studentProfileLiteMapper.selectIdByUserId(uid);
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
        requireActiveUser(uid);
        Integer role = RequestHolder.get() == null ? null : RequestHolder.get().getRole();
        ThrowUtils.throwIf(role == null, ErrorCode.PARAMS_ERROR);

        Integer pageSize = request.getPageSize();
        List<Room> rooms;
        if (Integer.valueOf(1).equals(role)) {
            Long teacherProfileId = teacherProfileLiteMapper.selectIdByUserId(uid);
            ThrowUtils.throwIf(teacherProfileId == null, ErrorCode.PARAMS_ERROR);
            rooms = roomMapper.listByTeacherProfileId(teacherProfileId, request.getCursor(), pageSize);
        } else if (Integer.valueOf(2).equals(role)) {
            Long studentProfileId = studentProfileLiteMapper.selectIdByUserId(uid);
            ThrowUtils.throwIf(studentProfileId == null, ErrorCode.PARAMS_ERROR);
            rooms = roomMapper.listByStudentProfileId(studentProfileId, request.getCursor(), pageSize);
        } else {
            return CursorPageResp.empty();
        }

        List<ChatRoomItemResp> list = new ArrayList<>();
        for (Room room : rooms) {
            Long otherUid = Integer.valueOf(1).equals(role)
                    ? studentProfileLiteMapper.selectUserIdById(room.getStudentProfileId())
                    : teacherProfileLiteMapper.selectUserIdById(room.getTeacherProfileId());

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

    private static Date toDate(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }
}
