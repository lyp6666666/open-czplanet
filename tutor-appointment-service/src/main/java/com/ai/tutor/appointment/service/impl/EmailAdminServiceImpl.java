package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.mapper.EmailNotificationTaskMapper;
import com.ai.tutor.appointment.mapper.EmailSendLogMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.entity.EmailNotificationTask;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.model.vo.admin.EmailTaskDetailVO;
import com.ai.tutor.appointment.model.vo.admin.EmailTaskRowVO;
import com.ai.tutor.appointment.model.vo.admin.PageResult;
import com.ai.tutor.appointment.service.EmailAdminService;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class EmailAdminServiceImpl implements EmailAdminService {

    @Resource
    private EmailNotificationTaskMapper taskMapper;
    @Resource
    private EmailSendLogMapper sendLogMapper;
    @Resource
    private UserMapper userMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PageResult<EmailTaskRowVO> pageTasks(int page, int size, Long userId, String email, String templateCode, String bizType, String status) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(1, Math.min(size, 100));
        int offset = (safePage - 1) * safeSize;
        List<EmailNotificationTask> tasks = taskMapper.pageAdminTasks(userId, normalize(email), normalizeUpper(templateCode), normalizeUpper(bizType), normalizeUpper(status), offset, safeSize);
        long total = taskMapper.countAdminTasks(userId, normalize(email), normalizeUpper(templateCode), normalizeUpper(bizType), normalizeUpper(status));
        return PageResult.<EmailTaskRowVO>builder()
                .records(tasks.stream().map(this::toRow).toList())
                .total(total)
                .page(safePage)
                .size(safeSize)
                .build();
    }

    @Override
    public EmailTaskDetailVO getTaskDetail(Long taskId) {
        EmailNotificationTask task = requireTask(taskId);
        return EmailTaskDetailVO.builder()
                .task(toRow(task))
                .payload(parsePayload(task.getPayloadJson()))
                .logs(sendLogMapper.listByTaskId(taskId))
                .build();
    }

    @Override
    public boolean retryTask(Long taskId) {
        EmailNotificationTask task = requireTask(taskId);
        if ("SENT".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR.getCode(), "发送成功任务无需重试");
        }
        int updated = taskMapper.retryNow(taskId, LocalDateTime.now());
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR.getCode(), "当前任务状态不允许重试");
        }
        return true;
    }

    private EmailNotificationTask requireTask(Long taskId) {
        EmailNotificationTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return task;
    }

    private EmailTaskRowVO toRow(EmailNotificationTask task) {
        User user = task.getReceiverUid() == null ? null : userMapper.selectById(task.getReceiverUid());
        return EmailTaskRowVO.builder()
                .id(task.getId())
                .taskKey(task.getTaskKey())
                .templateCode(task.getTemplateCode())
                .bizType(task.getBizType())
                .bizId(task.getBizId())
                .receiverUid(task.getReceiverUid())
                .receiverName(user == null ? null : user.getName())
                .receiverRole(task.getReceiverRole())
                .emailType(task.getEmailType())
                .email(task.getEmail())
                .subject(task.getSubject())
                .status(task.getStatus())
                .retryCount(task.getRetryCount())
                .maxRetryCount(task.getMaxRetryCount())
                .lastError(task.getLastError())
                .scheduledAt(task.getScheduledAt())
                .sentAt(task.getSentAt())
                .createTime(task.getCreateTime())
                .updateTime(task.getUpdateTime())
                .build();
    }

    private Map<String, Object> parsePayload(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeUpper(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase();
    }
}
