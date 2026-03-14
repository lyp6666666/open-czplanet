package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.dto.appointment.CreateAppointmentRequest;
import com.ai.tutor.appointment.model.dto.appointment.RescheduleAppointmentRequest;
import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.entity.TutorAppointment;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;

public interface TutorAppointmentService {

    Long create(CreateAppointmentRequest request, Long uid);

    void accept(Long id, Long uid);

    void reschedule(Long id, RescheduleAppointmentRequest request, Long uid);

    void confirmReschedule(Long id, Long uid);

    void cancel(Long id, String remark, Long uid);

    void complete(Long id, Long uid);

    TutorAppointment detail(Long id, Long uid);

    CursorPageResponse<TutorAppointment> mine(Long uid, Integer status, CursorPageRequest request);
}
