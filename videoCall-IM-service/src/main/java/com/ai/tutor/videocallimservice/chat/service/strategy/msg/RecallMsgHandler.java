package com.ai.tutor.videocallimservice.chat.service.strategy.msg;

import cn.hutool.json.JSONUtil;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.enums.MessageTypeEnum;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.RecallMsgReq;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
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
public class RecallMsgHandler extends AbstractMsgHandler<RecallMsgReq> {

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
    protected MessageTypeEnum getMsgTypeEnum() {
        return MessageTypeEnum.RECALL;
    }

    @Override
    protected void checkMsg(RecallMsgReq body, Long roomId, Long uid) {
        ThrowUtils.throwIf(roomId == null || uid == null || body.getTargetMsgId() == null, ErrorCode.PARAMS_ERROR);

        Room room = roomMapper.selectById(roomId);
        ThrowUtils.throwIf(room == null || room.getStatus() == null || room.getStatus() != 1, ErrorCode.NOT_FOUND_ERROR);

        Long teacherUid = resolveUserId(1, room.getTeacherProfileId());
        Long studentUid = resolveUserId(2, room.getStudentProfileId());
        ThrowUtils.throwIf(teacherUid == null || studentUid == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(teacherUid) && !uid.equals(studentUid), ErrorCode.NO_AUTH_ERROR);

        Message target = messageMapper.getById(body.getTargetMsgId());
        ThrowUtils.throwIf(target == null || target.getStatus() == null || target.getStatus() != 0, ErrorCode.NOT_FOUND_ERROR, "目标消息不存在");
        ThrowUtils.throwIf(!roomId.equals(target.getRoomId()), ErrorCode.PARAMS_ERROR, "目标消息不属于当前会话");
        ThrowUtils.throwIf(!uid.equals(target.getFromUid()), ErrorCode.NO_AUTH_ERROR, "只能撤回自己发送的消息");
        ThrowUtils.throwIf(target.getType() == null, ErrorCode.PARAMS_ERROR);
        boolean recallableType = MessageTypeEnum.TEXT.getType().equals(target.getType()) || MessageTypeEnum.IMG.getType().equals(target.getType());
        ThrowUtils.throwIf(!recallableType, ErrorCode.OPERATION_ERROR, "当前仅支持撤回文本或图片消息");
        ThrowUtils.throwIf(MessageTypeEnum.RECALL.getType().equals(target.getType()), ErrorCode.OPERATION_ERROR, "该消息已撤回");
        ThrowUtils.throwIf(MessageTypeEnum.SYSTEM.getType().equals(target.getType()), ErrorCode.OPERATION_ERROR, "系统消息不支持撤回");
        Integer recallCount = messageMapper.countRecallByTarget(roomId, uid, body.getTargetMsgId());
        ThrowUtils.throwIf(recallCount != null && recallCount > 0, ErrorCode.OPERATION_ERROR, "该消息已撤回");
    }

    @Override
    protected void saveMsg(Message message, RecallMsgReq body) {
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
        message.setContent("[消息已撤回]");
        // 撤回事件保留目标消息 id，前端据此把原消息替换成占位提示，而不是新增一条普通文本。
        message.setReplyMsgId(body.getTargetMsgId());
        message.setExtra(JSONUtil.toJsonStr(body));
    }

    @Override
    public Object showMsg(Message msg) {
        Map<String, Object> out = new HashMap<>();
        out.put("type", "recall");
        out.put("targetMsgId", msg == null ? null : msg.getReplyMsgId());
        out.put("operatorUid", msg == null ? null : msg.getFromUid());
        return out;
    }

    @Override
    public Object showReplyMsg(Message msg) {
        return showMsg(msg);
    }

    @Override
    public String showContactMsg(Message msg) {
        return "[消息已撤回]";
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
