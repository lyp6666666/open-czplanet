package com.ai.tutor.videocallimservice.chat.service.strategy.msg;

import cn.hutool.json.JSONUtil;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.enums.MessageTypeEnum;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.SystemMsgReq;
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

/**
 * 系统消息处理器。
 *
 * <p>用于业务系统投递结构化消息（例如授课申请卡片）。</p>
 */
@Component
public class SystemMsgHandler extends AbstractMsgHandler<SystemMsgReq> {

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
        return MessageTypeEnum.SYSTEM;
    }

    @Override
    protected void checkMsg(SystemMsgReq body, Long roomId, Long uid) {
        ThrowUtils.throwIf(roomId == null || uid == null, ErrorCode.PARAMS_ERROR);
        Room room = roomMapper.selectById(roomId);
        ThrowUtils.throwIf(room == null || room.getStatus() == null || room.getStatus() != 1, ErrorCode.NOT_FOUND_ERROR);

        Long teacherUid = resolveUserId(1, room.getTeacherProfileId());
        Long studentUid = resolveUserId(2, room.getStudentProfileId());
        ThrowUtils.throwIf(teacherUid == null || studentUid == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(teacherUid) && !uid.equals(studentUid), ErrorCode.NO_AUTH_ERROR);
    }

    @Override
    protected void saveMsg(Message message, SystemMsgReq body) {
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

        // content 用作会话列表预览；结构化内容放在 extra，便于后续演进与兼容。
        message.setContent(buildPreview(body));
        message.setExtra(JSONUtil.toJsonStr(body));
    }

    @Override
    public Object showMsg(Message msg) {
        // 优先使用 extra（结构化），content 仅作预览兜底。
        if (msg == null || msg.getExtra() == null || msg.getExtra().isBlank()) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("type", "system");
            fallback.put("content", msg == null ? "" : msg.getContent());
            return fallback;
        }

        SystemMsgReq body = JSONUtil.toBean(msg.getExtra(), SystemMsgReq.class);
        String bizType = body.getBizType() == null ? "" : body.getBizType().trim().toUpperCase();

        Map<String, Object> out = new HashMap<>();
        if ("LESSON_REQUEST".equals(bizType)) {
            out.put("type", "lesson_request");
            out.put("eventId", body.getEventId());
            out.put("title", body.getTitle());
            out.put("startAt", body.getStartAt());
            out.put("endAt", body.getEndAt());
            out.put("status", body.getStatus());
            out.put("creatorUserId", body.getCreatorUserId());
            return out;
        }
        if ("LESSON_STATUS".equals(bizType)) {
            out.put("type", "lesson_status");
            out.put("eventId", body.getEventId());
            out.put("title", body.getTitle());
            out.put("startAt", body.getStartAt());
            out.put("endAt", body.getEndAt());
            out.put("status", body.getStatus());
            out.put("actorUserId", body.getActorUserId());
            return out;
        }
        if ("COLLAB_PROPOSAL".equals(bizType)) {
            out.put("type", "collaboration_proposal");
            out.put("proposalId", body.getEventId());
            out.put("pricePerHour", body.getPricePerHour());
            out.put("classTime", body.getClassTime());
            out.put("frequencyPerWeek", body.getFrequencyPerWeek());
            out.put("status", body.getStatus());
            out.put("creatorUserId", body.getCreatorUserId());
            return out;
        }
        if ("COLLAB_PROPOSAL_STATUS".equals(bizType)) {
            out.put("type", "collaboration_status");
            out.put("proposalId", body.getEventId());
            out.put("status", body.getStatus());
            out.put("actorUserId", body.getActorUserId());
            return out;
        }
        if ("BROKERAGE_REQUIRED".equals(bizType)) {
            out.put("type", "brokerage_required");
            out.put("orderId", body.getEventId());
            out.put("proposalId", body.getProposalId());
            out.put("amountFen", body.getAmountFen());
            out.put("status", body.getStatus());
            out.put("payerUserId", body.getCreatorUserId());
            return out;
        }
        if ("CONTACT_UNLOCKED".equals(bizType)) {
            out.put("type", "contact_unlocked");
            out.put("proposalId", body.getProposalId() == null ? body.getEventId() : body.getProposalId());
            out.put("orderId", body.getOrderId());
            out.put("status", body.getStatus());
            return out;
        }
        if ("TUTOR_APPLICATION".equals(bizType)) {
            out.put("type", "tutor_application");
            out.put("applicationId", body.getEventId());
            out.put("content", body.getContent());
            out.put("status", body.getStatus());
            out.put("creatorUserId", body.getCreatorUserId());
            out.put("contextType", body.getContextType());
            out.put("contextId", body.getContextId());
            out.put("teachingMode", body.getTeachingMode());
            return out;
        }
        if ("TUTOR_APPLICATION_STATUS".equals(bizType)) {
            out.put("type", "tutor_application_status");
            out.put("applicationId", body.getEventId());
            out.put("status", body.getStatus());
            out.put("actorUserId", body.getActorUserId());
            return out;
        }
        if ("END_CHAT_REQUEST".equals(bizType)) {
            out.put("type", "end_chat_request");
            out.put("requestId", body.getEventId());
            out.put("status", body.getStatus());
            out.put("creatorUserId", body.getCreatorUserId() == null ? msg.getFromUid() : body.getCreatorUserId());
            return out;
        }
        if ("END_CHAT_STATUS".equals(bizType)) {
            out.put("type", "end_chat_status");
            out.put("requestId", body.getEventId());
            out.put("status", body.getStatus());
            out.put("actorUserId", body.getActorUserId() == null ? msg.getFromUid() : body.getActorUserId());
            return out;
        }
        if ("BROKERAGE_REFUND_REQUEST".equals(bizType)) {
            out.put("type", "brokerage_refund_request");
            out.put("requestId", body.getEventId());
            out.put("status", body.getStatus());
            out.put("creatorUserId", body.getCreatorUserId() == null ? msg.getFromUid() : body.getCreatorUserId());
            out.put("orderId", body.getOrderId());
            out.put("proposalId", body.getProposalId());
            return out;
        }
        if ("BROKERAGE_REFUND_STATUS".equals(bizType)) {
            out.put("type", "brokerage_refund_status");
            out.put("requestId", body.getEventId());
            out.put("status", body.getStatus());
            out.put("actorUserId", body.getActorUserId());
            out.put("orderId", body.getOrderId());
            out.put("proposalId", body.getProposalId());
            return out;
        }

        out.put("type", "system");
        out.put("bizType", body.getBizType());
        out.put("eventId", body.getEventId());
        out.put("content", msg.getContent());
        return out;
    }

    @Override
    public Object showReplyMsg(Message msg) {
        return showMsg(msg);
    }

    @Override
    public String showContactMsg(Message msg) {
        return msg == null ? "" : msg.getContent();
    }

    private static String buildPreview(SystemMsgReq body) {
        if (body == null) {
            return "系统消息";
        }
        String bizType = body.getBizType() == null ? "" : body.getBizType().trim().toUpperCase();
        String title = body.getTitle() == null || body.getTitle().isBlank() ? "课程" : body.getTitle();
        if ("LESSON_REQUEST".equals(bizType)) {
            return "授课申请：" + title;
        }
        if ("LESSON_STATUS".equals(bizType)) {
            String s = body.getStatus() == null ? "" : body.getStatus();
            return "课程状态：" + s + "（" + title + "）";
        }
        if ("COLLAB_PROPOSAL".equals(bizType)) {
            return "合作提案";
        }
        if ("COLLAB_PROPOSAL_STATUS".equals(bizType)) {
            String s = body.getStatus() == null ? "" : body.getStatus();
            return "合作提案：" + s;
        }
        if ("BROKERAGE_REQUIRED".equals(bizType)) {
            return "信息费支付";
        }
        if ("CONTACT_UNLOCKED".equals(bizType)) {
            return "聊天功能开启";
        }
        if ("TUTOR_APPLICATION".equals(bizType)) {
            return "家教申请";
        }
        if ("TUTOR_APPLICATION_STATUS".equals(bizType)) {
            String s = body.getStatus() == null ? "" : body.getStatus();
            return "家教申请：" + s;
        }
        if ("END_CHAT_REQUEST".equals(bizType)) {
            return "结束沟通确认";
        }
        if ("END_CHAT_STATUS".equals(bizType)) {
            String s = body.getStatus() == null ? "" : body.getStatus();
            return "结束沟通：" + s;
        }
        if ("BROKERAGE_REFUND_REQUEST".equals(bizType)) {
            return "退款申请";
        }
        if ("BROKERAGE_REFUND_STATUS".equals(bizType)) {
            String s = body.getStatus() == null ? "" : body.getStatus();
            return "退款状态：" + s;
        }
        return "系统消息：" + title;
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
