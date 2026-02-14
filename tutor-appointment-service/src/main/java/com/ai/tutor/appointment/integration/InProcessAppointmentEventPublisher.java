package com.ai.tutor.appointment.integration;

import com.ai.tutor.common.event.AppointmentAcceptedEvent;
import com.ai.tutor.common.integration.AppointmentEventPublisher;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 预约事件的进程内实现。
 *
 * <p>单体阶段用于快速闭环：通过 Spring ApplicationEvent 在同一进程内分发事件。</p>
 * <p>后续拆微服务时，可以保留 {@link AppointmentEventPublisher} 接口不变，
 * 将实现替换为 MQ/事件总线发布即可。</p>
 */
@Component
public class InProcessAppointmentEventPublisher implements AppointmentEventPublisher {

    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publishAccepted(Long appointmentId, Long parentId, Long tutorId) {
        applicationEventPublisher.publishEvent(new AppointmentAcceptedEvent(this, appointmentId, parentId, tutorId));
    }
}

