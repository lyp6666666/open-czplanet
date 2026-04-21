package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.dto.schedule.InternalTrialEventRequest;
import com.ai.tutor.appointment.service.ScheduleService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.ResultUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/facade/schedule")
@RequiredArgsConstructor
public class InternalScheduleFacadeController {

    private final ScheduleService scheduleService;

    @PostMapping("/trial-events")
    public BaseResponse<Long> createAcceptedTrialEvent(@RequestBody InternalTrialEventRequest request) {
        return ResultUtils.success(scheduleService.createAcceptedTrialEvent(request));
    }
}
