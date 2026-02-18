package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.dto.schedule.CreateScheduleEventRequest;
import com.ai.tutor.appointment.model.vo.schedule.ScheduleEventVO;

import java.util.List;

/**
 * 课程安排（日历）服务。
 *
 * <p>V1 设计：以 1v1 约课为核心，将“授课申请”落到聊天中并在双方日历中沉淀。</p>
 */
public interface ScheduleService {

    /**
     * 查询当前用户在时间范围内的日程（用于月/周/日视图）。
     */
    List<ScheduleEventVO> listEvents(Long uid, Long startAtMs, Long endAtMs, boolean includePending);

    /**
     * 创建授课申请（创建日程 + 发送授课申请消息）。
     */
    ScheduleEventVO createEvent(CreateScheduleEventRequest request, Long uid);

    /**
     * 响应授课申请（接收/拒绝）。
     */
    ScheduleEventVO respond(Long eventId, String action, Long uid);

    /**
     * 取消申请/取消课程。
     */
    ScheduleEventVO cancel(Long eventId, Long uid, String remark);
}

