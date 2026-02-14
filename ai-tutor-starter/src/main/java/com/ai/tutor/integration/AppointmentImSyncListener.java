package com.ai.tutor.integration;

import com.ai.tutor.common.event.AppointmentAcceptedEvent;
import com.ai.tutor.common.integration.ImFacade;
import jakarta.annotation.Resource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentImSyncListener {

    @Resource
    private ImFacade imFacade;

    @EventListener
    public void onAppointmentAccepted(AppointmentAcceptedEvent event) {
        if (event.getParentId() == null || event.getTutorId() == null) {
            return;
        }
        imFacade.getOrCreateRoomWithUser(event.getTutorId(), event.getParentId());
    }
}
