package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.dto.appointment.CancelAppointmentRequest;
import com.ai.tutor.appointment.model.dto.schedule.CreateScheduleEventRequest;
import com.ai.tutor.appointment.model.dto.schedule.RespondScheduleEventRequest;
import com.ai.tutor.appointment.model.vo.schedule.ScheduleEventVO;
import com.ai.tutor.appointment.service.ScheduleService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程安排（日历）接口。
 *
 * <p>设计目标：对齐“课程安排”前端页面，提供日历查询、创建授课申请、接收/拒绝、取消等能力。</p>
 */
@RestController
@RequestMapping("/api/v1/schedule")
@Tag(name = "课程安排接口", description = "日历查询、创建授课申请、接收/拒绝、取消")
public class ScheduleController {

    @Resource
    private ScheduleService scheduleService;

    @GetMapping("/events")
    @Operation(summary = "查询日程（用于月/周/日视图）")
    public BaseResponse<List<ScheduleEventVO>> listEvents(@RequestParam("startAt") Long startAt,
                                                          @RequestParam("endAt") Long endAt,
                                                          @RequestParam(value = "includePending", required = false, defaultValue = "true") Boolean includePending) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(scheduleService.listEvents(uid, startAt, endAt, includePending != null && includePending));
    }

    @GetMapping("/courses/{courseId}/events")
    @Operation(summary = "查询长期课程下的课节列表")
    public BaseResponse<List<ScheduleEventVO>> listCourseEvents(@PathVariable("courseId") Long courseId) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(scheduleService.listCourseEvents(courseId, uid));
    }

    @PostMapping("/events")
    @Operation(summary = "创建授课申请（创建日程并发送聊天卡片）")
    public BaseResponse<ScheduleEventVO> create(@Valid @RequestBody CreateScheduleEventRequest request) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(scheduleService.createEvent(request, uid));
    }

    @PostMapping("/events/{eventId}/response")
    @Operation(summary = "响应授课申请（接收/拒绝）")
    public BaseResponse<ScheduleEventVO> respond(@PathVariable("eventId") Long eventId,
                                                 @Valid @RequestBody RespondScheduleEventRequest request) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(scheduleService.respond(eventId, request.getAction(), uid));
    }

    @PostMapping("/events/{eventId}/cancel")
    @Operation(summary = "取消申请/取消课程")
    public BaseResponse<ScheduleEventVO> cancel(@PathVariable("eventId") Long eventId,
                                                @RequestBody(required = false) CancelAppointmentRequest request) {
        Long uid = RequestHolder.get().getUid();
        String remark = request == null ? null : request.getRemark();
        return ResultUtils.success(scheduleService.cancel(eventId, uid, remark));
    }
}
