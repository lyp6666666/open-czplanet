package com.ai.tutor.videocallimservice.chat.service.strategy.msg;

import cn.hutool.json.JSONUtil;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.enums.MessageTypeEnum;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ImgMsgReq;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
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
public class ImgMsgHandler extends AbstractMsgHandler<ImgMsgReq> {

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
        return MessageTypeEnum.IMG;
    }

    @Override
    protected void checkMsg(ImgMsgReq body, Long roomId, Long uid) {
        ThrowUtils.throwIf(roomId == null || uid == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(body.getSize() == null || body.getSize() <= 0, ErrorCode.PARAMS_ERROR, "图片大小不合法");

        Room room = roomMapper.selectById(roomId);
        ThrowUtils.throwIf(room == null || room.getStatus() == null || room.getStatus() != 1, ErrorCode.NOT_FOUND_ERROR);

        Long teacherUid = resolveUserId(1, room.getTeacherProfileId());
        Long studentUid = resolveUserId(2, room.getStudentProfileId());
        ThrowUtils.throwIf(teacherUid == null || studentUid == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(teacherUid) && !uid.equals(studentUid), ErrorCode.NO_AUTH_ERROR);
    }

    @Override
    protected void saveMsg(Message message, ImgMsgReq body) {
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
        message.setContent("[图片]");
        message.setReplyMsgId(body.getReplyMsgId());
        // 图片消息正文走 extra，content 只保留给列表预览与搜索占位。
        message.setExtra(JSONUtil.toJsonStr(body));
    }

    @Override
    public Object showMsg(Message msg) {
        if (msg == null || msg.getExtra() == null || msg.getExtra().isBlank()) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("type", "image");
            fallback.put("url", "");
            fallback.put("objectKey", null);
            fallback.put("contentType", null);
            fallback.put("size", 0L);
            fallback.put("width", null);
            fallback.put("height", null);
            return fallback;
        }

        ImgMsgReq body = JSONUtil.toBean(msg.getExtra(), ImgMsgReq.class);
        Map<String, Object> out = new HashMap<>();
        out.put("type", "image");
        out.put("url", body.getUrl());
        out.put("objectKey", body.getObjectKey());
        out.put("contentType", body.getContentType());
        out.put("size", body.getSize());
        out.put("width", body.getWidth());
        out.put("height", body.getHeight());
        return out;
    }

    @Override
    public Object showReplyMsg(Message msg) {
        return showMsg(msg);
    }

    @Override
    public String showContactMsg(Message msg) {
        return "[图片]";
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
