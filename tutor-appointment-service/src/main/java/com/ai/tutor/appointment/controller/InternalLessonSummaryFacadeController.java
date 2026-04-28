package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.dto.summary.UpsertLessonSummaryRequest;
import com.ai.tutor.appointment.model.entity.LessonSummary;
import com.ai.tutor.appointment.service.LessonSummaryService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.ResultUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/facade/lesson-summaries")
@RequiredArgsConstructor
public class InternalLessonSummaryFacadeController {

    private final LessonSummaryService lessonSummaryService;

    @PostMapping("/ready")
    public BaseResponse<LessonSummary> upsertReady(@RequestBody UpsertLessonSummaryRequest request) {
        return ResultUtils.success(lessonSummaryService.upsertReadyInternal(request));
    }
}
