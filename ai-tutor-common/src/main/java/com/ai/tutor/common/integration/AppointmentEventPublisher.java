package com.ai.tutor.common.integration;

/**
 * 预约领域事件发布器。
 *
 * <p>在单体阶段可以用进程内事件实现；后续拆微服务可替换为 MQ/事件总线实现（RocketMQ/Kafka 等）。</p>
 */
public interface AppointmentEventPublisher {

    /**
     * 发布“预约被确认”事件。
     *
     * @param appointmentId 预约 id
     * @param parentId      家长 uid
     * @param tutorId       老师 uid
     */
    void publishAccepted(Long appointmentId, Long parentId, Long tutorId);
}

