package com.ai.tutor.appointment.model.dto.home;

import lombok.Data;

/**
 * 热门老师聚合查询的结果行（从 teacher_job_posting 聚合得到）。
 *
 * cursorKey 用于游标翻页：
 * - 取该老师最新一条服务贴的 id（越大越新）；
 * - 下一页请求携带 cursor=上一页最后一条的 cursorKey。
 */
@Data
public class HomeHotTutorAggRow {
    private Long tutorId;
    private Long cursorKey;
}

