package com.ai.tutor.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AppointmentAcceptedEvent extends ApplicationEvent {

    private final Long appointmentId;
    private final Long parentId;
    private final Long tutorId;

    public AppointmentAcceptedEvent(Object source, Long appointmentId, Long parentId, Long tutorId) {
        super(source);
        this.appointmentId = appointmentId;
        this.parentId = parentId;
        this.tutorId = tutorId;
    }
}

