package com.ai.tutor.videocallimservice.chat.service.impl;

import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.enums.MessageTypeEnum;
import com.ai.tutor.videocallimservice.chat.domain.enums.TutorApplicationChatAccessStatus;
import com.ai.tutor.videocallimservice.chat.domain.entity.TutorApplication;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatRoomPageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatRoomStartReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.TextMsgReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatRoomItemResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CursorPageResp;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.mapper.TutorApplicationMapper;
import com.ai.tutor.videocallimservice.chat.service.adapter.MessageAdapter;
import com.ai.tutor.videocallimservice.chat.service.ChatRoomService;
import com.ai.tutor.videocallimservice.chat.service.ChatService;
import com.ai.tutor.videocallimservice.common.domain.entity.ImUser;
import com.ai.tutor.videocallimservice.common.mapper.ImUserMapper;
import com.ai.tutor.videocallimservice.common.mapper.StudentProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.mapper.TeacherProfileLiteMapper;
import com.ai.tutor.utils.RequestHolder;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class ChatRoomServiceImpl implements ChatRoomService {

    @Resource
    private RoomMapper roomMapper;

    @Resource
    private MessageMapper messageMapper;

    @Resource
    private ImUserMapper imUserMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private TeacherProfileLiteMapper teacherProfileLiteMapper;

    @Resource
    private StudentProfileLiteMapper studentProfileLiteMapper;

    @Resource
    private ChatService chatService;

    @Resource
    private TutorApplicationMapper tutorApplicationMapper;

    @Value("${tutor-application.gating-enabled:true}")
    private boolean tutorApplicationGatingEnabled;

    @Value("${tutor-application.skip-payment-check:false}")
    private boolean skipPaymentCheck;

    @Override
    public Long getOrCreateRoomWithUser(Long targetUid, Long uid) {
        ThrowUtils.throwIf(uid == null || targetUid == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(uid.equals(targetUid), ErrorCode.PARAMS_ERROR, "不能与自己发起聊天");

        assertNotBlockedByApplication(uid, targetUid);
        requireActiveUser(uid);
        requireActiveUser(targetUid);

        Integer role = RequestHolder.get() == null ? null : RequestHolder.get().getRole();
        ThrowUtils.throwIf(role == null, ErrorCode.PARAMS_ERROR, "用户角色缺失");

        Long teacherProfileId;
        Long studentProfileId;
        if (Integer.valueOf(1).equals(role)) {
            teacherProfileId = teacherProfileLiteMapper.selectIdByUserId(uid);
            ThrowUtils.throwIf(teacherProfileId == null, ErrorCode.OPERATION_ERROR, "教师资料未初始化，请先完成教师端入驻");
            studentProfileId = studentProfileLiteMapper.selectIdByUserId(targetUid);
            ThrowUtils.throwIf(studentProfileId == null, ErrorCode.OPERATION_ERROR, "对方不是学生账号，无法建立会话");
        } else if (Integer.valueOf(2).equals(role) || Integer.valueOf(3).equals(role)) {
            teacherProfileId = teacherProfileLiteMapper.selectIdByUserId(targetUid);
            ThrowUtils.throwIf(teacherProfileId == null, ErrorCode.OPERATION_ERROR, "对方不是教师账号，无法建立会话");
            studentProfileId = studentProfileLiteMapper.selectIdByUserId(uid);
            ThrowUtils.throwIf(studentProfileId == null, ErrorCode.OPERATION_ERROR, "资料未初始化，请联系管理员");
        } else {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "用户角色不合法");
            return null;
        }

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
    @Transactional
    public Long startChat(ChatRoomStartReq request, Long uid) {
        ThrowUtils.throwIf(request == null || uid == null, ErrorCode.PARAMS_ERROR);
        Long roomId = getOrCreateRoomWithUser(request.getTargetUid(), uid);

        String greeting = request.getGreeting();
        if (greeting == null || greeting.trim().isEmpty()) {
            return roomId;
        }

        Integer role = RequestHolder.get() == null ? null : RequestHolder.get().getRole();
        if (!Integer.valueOf(1).equals(role)) {
            return roomId;
        }

        Room locked = roomMapper.selectByIdForUpdate(roomId);
        ThrowUtils.throwIf(locked == null || locked.getStatus() == null || locked.getStatus() != 1, ErrorCode.NOT_FOUND_ERROR);
        if (locked.getLastMsgId() != null) {
            return roomId;
        }

        ChatMessageReq msgReq = ChatMessageReq.builder()
                .roomId(roomId)
                .msgType(MessageTypeEnum.TEXT.getType())
                .body(TextMsgReq.builder().content(greeting.trim()).build())
                .build();
        chatService.sendMsg(msgReq, uid);
        return roomId;
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
        } else if (Integer.valueOf(2).equals(role) || Integer.valueOf(3).equals(role)) {
            Long studentProfileId = studentProfileLiteMapper.selectIdByUserId(uid);
            ThrowUtils.throwIf(studentProfileId == null, ErrorCode.PARAMS_ERROR);
            rooms = roomMapper.listByStudentProfileId(studentProfileId, request.getCursor(), pageSize);
        } else {
            return CursorPageResp.empty();
        }

        List<Long> roomIds = new ArrayList<>();
        for (Room room : rooms) {
            roomIds.add(room.getId());
        }
        Map<Long, Long> unreadMap = new HashMap<>();
        if (!roomIds.isEmpty()) {
            try {
                messageMapper.listUnreadCounts(roomIds, uid).forEach(it -> unreadMap.put(it.getRoomId(), it.getUnreadCount()));
            } catch (Exception ignored) {
            }
        }

        List<ChatRoomItemResp> list = new ArrayList<>();
        for (Room room : rooms) {
            Long otherUid = Integer.valueOf(1).equals(role)
                    ? studentProfileLiteMapper.selectUserIdById(room.getStudentProfileId())
                    : teacherProfileLiteMapper.selectUserIdById(room.getTeacherProfileId());

            Message lastMsg = room.getLastMsgId() == null ? null : messageMapper.getById(room.getLastMsgId());
            Object lastBody = lastMsg == null ? null : MessageAdapter.buildContactPreview(lastMsg);

            list.add(ChatRoomItemResp.builder()
                    .roomId(room.getId())
                    .otherUid(otherUid)
                    .lastMsgId(room.getLastMsgId())
                    .lastMsgBody(lastBody)
                    .unreadCount(unreadMap.getOrDefault(room.getId(), 0L))
                    .activeTime(toDate(room.getActiveTime()))
                    .build());
        }

        Long nextCursor = list.isEmpty() ? null : list.get(list.size() - 1).getRoomId();
        boolean isLast = rooms.size() < pageSize;
        return new CursorPageResp<>(nextCursor, isLast, list);
    }

    private ImUser requireActiveUser(Long uid) {
        ImUser user = imUserMapper.selectById(uid);
        if (user == null) {
            try {
                user = jdbcTemplate.queryForObject(
                        "SELECT id, user_type AS userType, ref_id AS refId, status FROM user WHERE id = ? LIMIT 1",
                        new Object[]{uid},
                        (rs, rowNum) -> {
                            ImUser row = new ImUser();
                            row.setId(rs.getLong("id"));
                            row.setUserType(rs.getObject("userType") == null ? null : rs.getInt("userType"));
                            row.setRefId(rs.getObject("refId") == null ? null : rs.getLong("refId"));
                            row.setStatus(rs.getObject("status") == null ? null : rs.getInt("status"));
                            return row;
                        }
                );
            } catch (EmptyResultDataAccessException ignored) {
            }
        }
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "user not found, uid=" + uid);
        ThrowUtils.throwIf(user.getStatus() != null && user.getStatus() == 1, ErrorCode.NO_AUTH_ERROR);
        return user;
    }

    private static Date toDate(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }

    private void assertNotBlockedByApplication(Long uid, Long targetUid) {
        if (!tutorApplicationGatingEnabled) {
            return;
        }
        if (skipPaymentCheck) {
            return;
        }
        TutorApplication application = tutorApplicationMapper.selectLatestAcceptedBetween(uid, targetUid);
        if (application == null) {
            return;
        }
        if (TutorApplicationChatAccessStatus.PAYMENT_REQUIRED.name().equals(application.getChatAccessStatus())) {
            return;
        }
    }
}
