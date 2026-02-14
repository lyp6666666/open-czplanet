package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.dto.appointment.CancelAppointmentRequest;
import com.ai.tutor.appointment.model.dto.appointment.CreateAppointmentRequest;
import com.ai.tutor.appointment.model.dto.appointment.RescheduleAppointmentRequest;
import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.entity.TutorAppointment;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.service.TutorAppointmentService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/appointment")
@Tag(name = "预约接口", description = "邀约创建、确认、改期、取消、列表与详情")
public class TutorAppointmentController {

    @Resource
    private TutorAppointmentService tutorAppointmentService;

    @PostMapping
    @Operation(summary = "创建邀约/预约")
    public BaseResponse<Long> create(@Valid @RequestBody CreateAppointmentRequest request) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(tutorAppointmentService.create(request, uid));
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "接受邀约")
    public BaseResponse<String> accept(@PathVariable("id") Long id) {
        tutorAppointmentService.accept(id, RequestHolder.get().getUid());
        return ResultUtils.success("OK");
    }

    @PostMapping("/{id}/reschedule")
    @Operation(summary = "发起改期")
    public BaseResponse<String> reschedule(@PathVariable("id") Long id, @Valid @RequestBody RescheduleAppointmentRequest request) {
        tutorAppointmentService.reschedule(id, request, RequestHolder.get().getUid());
        return ResultUtils.success("OK");
    }

    @PostMapping("/{id}/confirmReschedule")
    @Operation(summary = "确认改期")
    public BaseResponse<String> confirmReschedule(@PathVariable("id") Long id) {
        tutorAppointmentService.confirmReschedule(id, RequestHolder.get().getUid());
        return ResultUtils.success("OK");
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消预约")
    public BaseResponse<String> cancel(@PathVariable("id") Long id, @RequestBody(required = false) CancelAppointmentRequest request) {
        String remark = request == null ? null : request.getRemark();
        tutorAppointmentService.cancel(id, remark, RequestHolder.get().getUid());
        return ResultUtils.success("OK");
    }

    @GetMapping("/{id}")
    @Operation(summary = "预约详情")
    public BaseResponse<TutorAppointment> detail(@PathVariable("id") Long id) {
        return ResultUtils.success(tutorAppointmentService.detail(id, RequestHolder.get().getUid()));
    }

    @GetMapping("/mine")
    @Operation(summary = "我的预约列表（游标分页）")
    public BaseResponse<CursorPageResponse<TutorAppointment>> mine(@RequestParam(value = "status", required = false) Integer status,
                                                                   @Valid CursorPageRequest request) {
        return ResultUtils.success(tutorAppointmentService.mine(RequestHolder.get().getUid(), status, request));
    }
}

