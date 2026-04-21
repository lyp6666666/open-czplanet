package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.EmailNotificationTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EmailNotificationTaskMapper {
    int insertIgnore(EmailNotificationTask task);

    EmailNotificationTask selectById(@Param("id") Long id);

    EmailNotificationTask selectByTaskKey(@Param("taskKey") String taskKey);

    List<EmailNotificationTask> listDuePending(@Param("now") LocalDateTime now, @Param("limit") Integer limit);

    int updateStatusIfCurrent(@Param("id") Long id,
                              @Param("fromStatus") String fromStatus,
                              @Param("toStatus") String toStatus);

    int markSent(@Param("id") Long id, @Param("sentAt") LocalDateTime sentAt, @Param("subject") String subject);

    int markCanceled(@Param("id") Long id, @Param("reason") String reason);

    int markFailedOrRetry(@Param("id") Long id,
                          @Param("status") String status,
                          @Param("lastError") String lastError,
                          @Param("nextScheduledAt") LocalDateTime nextScheduledAt,
                          @Param("retryCount") Integer retryCount);

    int cancelPendingByBiz(@Param("bizType") String bizType, @Param("bizId") Long bizId, @Param("reason") String reason);

    int countSentSince(@Param("receiverUid") Long receiverUid,
                       @Param("templateCode") String templateCode,
                       @Param("bizType") String bizType,
                       @Param("bizId") Long bizId,
                       @Param("since") LocalDateTime since);

    int countSentByTaskKeyPrefix(@Param("taskKeyPrefix") String taskKeyPrefix);
}
