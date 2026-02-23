package com.ai.tutor.videocallimservice.chat.service.strategy.msg;

import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.enums.MessageTypeEnum;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.TextMsgReq;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.service.util.ImMessageMasking;
import com.ai.tutor.videocallimservice.chat.service.strategy.AbstractMsgHandler;
import com.ai.tutor.videocallimservice.common.domain.entity.ImUser;
import com.ai.tutor.videocallimservice.common.mapper.ImUserMapper;
import com.ai.tutor.videocallimservice.common.mapper.StudentProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.mapper.TeacherProfileLiteMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TextMsgHandler extends AbstractMsgHandler<TextMsgReq> {

    @Resource
    private RoomMapper roomMapper;

    @Resource
    private ImUserMapper imUserMapper;

    @Resource
    private TeacherProfileLiteMapper teacherProfileLiteMapper;

    @Resource
    private StudentProfileLiteMapper studentProfileLiteMapper;

    @Override
    protected MessageTypeEnum getMsgTypeEnum() {
        return MessageTypeEnum.TEXT;
    }

    @Override
    protected void checkMsg(TextMsgReq body, Long roomId, Long uid) {
        ThrowUtils.throwIf(roomId == null || uid == null, ErrorCode.PARAMS_ERROR);
        Room room = roomMapper.selectById(roomId);
        ThrowUtils.throwIf(room == null || room.getStatus() == null || room.getStatus() != 1, ErrorCode.NOT_FOUND_ERROR);

        Long teacherUid = resolveUserId(1, room.getTeacherProfileId());
        Long studentUid = resolveUserId(2, room.getStudentProfileId());
        ThrowUtils.throwIf(teacherUid == null || studentUid == null, ErrorCode.NOT_FOUND_ERROR);

        ThrowUtils.throwIf(!uid.equals(teacherUid) && !uid.equals(studentUid), ErrorCode.NO_AUTH_ERROR);
    }

    @Override
    protected void saveMsg(Message message, TextMsgReq body) {
        Room room = roomMapper.selectById(message.getRoomId());
        ThrowUtils.throwIf(room == null || room.getStatus() == null || room.getStatus() != 1, ErrorCode.NOT_FOUND_ERROR);

        Long teacherUid = resolveUserId(1, room.getTeacherProfileId());
        Long studentUid = resolveUserId(2, room.getStudentProfileId());
        ThrowUtils.throwIf(teacherUid == null || studentUid == null, ErrorCode.NOT_FOUND_ERROR);

        Long fromUid = message.getFromUid();
        ThrowUtils.throwIf(fromUid == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!fromUid.equals(teacherUid) && !fromUid.equals(studentUid), ErrorCode.NO_AUTH_ERROR);

        Long toUid = fromUid.equals(teacherUid) ? studentUid : teacherUid;
        message.setToUid(toUid);
        message.setContent(body.getContent());
        message.setIsMasked(ImMessageMasking.mask(body.getContent()).masked ? 1 : 0);
        message.setReplyMsgId(body.getReplyMsgId());
    }

    @Override
    public Object showMsg(Message msg) {
        ImMessageMasking.MaskResult r = ImMessageMasking.mask(msg == null ? "" : msg.getContent());
        boolean masked = r.masked || (msg != null && msg.getIsMasked() != null && msg.getIsMasked() == 1);
        Map<String, Object> body = new HashMap<>();
        body.put("type", "text");
        body.put("content", r.maskedText);
        body.put("masked", masked);
        return body;
    }

    @Override
    public Object showReplyMsg(Message msg) {
        return showMsg(msg);
    }

    @Override
    public String showContactMsg(Message msg) {
        return ImMessageMasking.mask(msg == null ? "" : msg.getContent()).maskedText;
    }

    private Long resolveUserId(int userType, Long refId) {
        if (refId == null) {
            return null;
        }
        Long userId;
        if (userType == 1) {
            userId = teacherProfileLiteMapper.selectUserIdById(refId);
        } else if (userType == 2) {
            userId = studentProfileLiteMapper.selectUserIdById(refId);
        } else {
            return null;
        }
        if (userId == null) {
            return null;
        }
        ImUser user = imUserMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            return null;
        }
        return user.getId();
    }
}
