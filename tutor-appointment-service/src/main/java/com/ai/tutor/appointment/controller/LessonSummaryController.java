package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.dto.summary.UpsertLessonSummaryRequest;
import com.ai.tutor.appointment.model.entity.LessonSummary;
import com.ai.tutor.appointment.service.LessonSummaryService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lesson-summaries")
public class LessonSummaryController {

    @Resource
    private LessonSummaryService lessonSummaryService;

    @PostMapping
    public BaseResponse<LessonSummary> upsert(@RequestBody UpsertLessonSummaryRequest request) {
        return ResultUtils.success(lessonSummaryService.upsertReady(request, RequestHolder.get().getUid()));
    }

    @GetMapping("/lesson/{lessonId}")
    public BaseResponse<LessonSummary> getByLessonId(@PathVariable("lessonId") Long lessonId) {
        return ResultUtils.success(lessonSummaryService.getByLessonId(lessonId, RequestHolder.get().getUid()));
    }
}
