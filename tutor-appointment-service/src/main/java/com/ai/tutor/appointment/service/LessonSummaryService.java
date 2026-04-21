package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.dto.summary.UpsertLessonSummaryRequest;
import com.ai.tutor.appointment.model.entity.LessonSummary;

public interface LessonSummaryService {
    LessonSummary upsertReady(UpsertLessonSummaryRequest request, Long uid);

    LessonSummary getByLessonId(Long lessonId, Long uid);
}
