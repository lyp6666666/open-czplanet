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
import org.springframework.jdbc.core.JdbcTemplate;
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

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public void ackRead(ChatReadAckReq request, Long uid) {
        ThrowUtils.throwIf(request == null || uid == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getRoomId() == null || request.getLastReadMsgId() == null, ErrorCode.PARAMS_ERROR);

        Room room = roomMapper.selectById(request.getRoomId());
        ThrowUtils.throwIf(room == null || room.getStatus() == null || room.getStatus() != 1, ErrorCode.NOT_FOUND_ERROR);

        Long teacherUid = resolveUserId(1, room.getTeacherProfileId());
        Long studentUid = resolveUserId(2, room.getStudentProfileId());
        ThrowUtils.throwIf(teacherUid == null || studentUid == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(teacherUid) && !uid.equals(studentUid), ErrorCode.NO_AUTH_ERROR);

        Message msg = messageMapper.getById(request.getLastReadMsgId());
        ThrowUtils.throwIf(msg == null || msg.getStatus() == null || msg.getStatus() != 0, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!request.getRoomId().equals(msg.getRoomId()), ErrorCode.PARAMS_ERROR);

        tryInitRoomReadStateTable();
        try {
            roomReadStateMapper.upsertReadState(request.getRoomId(), uid, request.getLastReadMsgId());
        } catch (Exception ignored) {
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

    private void tryInitRoomReadStateTable() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS `room_read_state` ("
                            + " `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '已读状态id',"
                            + " `room_id` bigint(20) NOT NULL COMMENT '会话id',"
                            + " `uid` bigint(20) NOT NULL COMMENT '用户id',"
                            + " `last_read_msg_id` bigint(20) DEFAULT NULL COMMENT '最后已读消息id',"
                            + " `last_read_time` datetime(3) DEFAULT NULL COMMENT '最后已读时间',"
                            + " `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
                            + " `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),"
                            + " PRIMARY KEY (`id`),"
                            + " UNIQUE KEY `uniq_room_uid` (`room_id`, `uid`),"
                            + " KEY `idx_uid` (`uid`),"
                            + " KEY `idx_room_id` (`room_id`)"
                            + " ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话已读状态表'"
            );
        } catch (Exception ignored) {
        }
    }
}
