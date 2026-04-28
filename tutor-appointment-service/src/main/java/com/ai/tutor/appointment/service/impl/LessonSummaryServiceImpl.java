package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.mapper.LessonSummaryMapper;
import com.ai.tutor.appointment.mapper.TutorAppointmentMapper;
import com.ai.tutor.appointment.model.dto.summary.UpsertLessonSummaryRequest;
import com.ai.tutor.appointment.model.entity.LessonSummary;
import com.ai.tutor.appointment.model.entity.TutorAppointment;
import com.ai.tutor.appointment.service.EmailNotificationService;
import com.ai.tutor.appointment.service.LessonSummaryService;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LessonSummaryServiceImpl implements LessonSummaryService {

    @Resource
    private TutorAppointmentMapper tutorAppointmentMapper;
    @Resource
    private LessonSummaryMapper lessonSummaryMapper;
    @Resource
    private EmailNotificationService emailNotificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LessonSummary upsertReady(UpsertLessonSummaryRequest request, Long uid) {
        ThrowUtils.throwIf(request == null || request.getLessonId() == null, ErrorCode.PARAMS_ERROR);
        TutorAppointment lesson = requireParticipant(request.getLessonId(), uid);
        return upsertReadyForLesson(request, lesson, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LessonSummary upsertReadyInternal(UpsertLessonSummaryRequest request) {
        ThrowUtils.throwIf(request == null || request.getLessonId() == null, ErrorCode.PARAMS_ERROR);
        TutorAppointment lesson = tutorAppointmentMapper.selectById(request.getLessonId());
        ThrowUtils.throwIf(lesson == null, ErrorCode.NOT_FOUND_ERROR);
        return upsertReadyForLesson(request, lesson, false);
    }

    private LessonSummary upsertReadyForLesson(UpsertLessonSummaryRequest request, TutorAppointment lesson, boolean createEmailTasks) {
        ThrowUtils.throwIf(request.getSummaryContent() == null || request.getSummaryContent().isBlank(), ErrorCode.PARAMS_ERROR, "总结内容不能为空");
        LessonSummary existing = lessonSummaryMapper.selectByLessonId(lesson.getId());
        if (isSameReadySummary(existing, request)) {
            return existing;
        }
        LessonSummary summary = LessonSummary.builder()
                .lessonId(lesson.getId())
                .courseId(lesson.getCourseId())
                .teacherUid(lesson.getTutorId())
                .studentUid(lesson.getParentId())
                .title(request.getTitle() == null || request.getTitle().isBlank() ? safeTitle(lesson) : request.getTitle())
                .summaryStatus("READY")
                .summaryBrief(request.getSummaryBrief())
                .summaryContent(request.getSummaryContent())
                .homework(request.getHomework())
                .readyAt(LocalDateTime.now())
                .build();
        lessonSummaryMapper.upsertReady(summary);
        LessonSummary saved = lessonSummaryMapper.selectByLessonId(lesson.getId());
        if (createEmailTasks) {
            emailNotificationService.createLessonSummaryTasks(saved == null ? summary : saved);
        }
        return saved == null ? summary : saved;
    }

    @Override
    public LessonSummary getByLessonId(Long lessonId, Long uid) {
        requireParticipant(lessonId, uid);
        return lessonSummaryMapper.selectByLessonId(lessonId);
    }

    private TutorAppointment requireParticipant(Long lessonId, Long uid) {
        ThrowUtils.throwIf(lessonId == null || uid == null, ErrorCode.PARAMS_ERROR);
        TutorAppointment lesson = tutorAppointmentMapper.selectById(lessonId);
        ThrowUtils.throwIf(lesson == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(lesson.getParentId()) && !uid.equals(lesson.getTutorId()), ErrorCode.NO_AUTH_ERROR);
        return lesson;
    }

    private String safeTitle(TutorAppointment lesson) {
        return lesson.getTitle() == null || lesson.getTitle().isBlank() ? "课后总结" : lesson.getTitle();
    }

    private boolean isSameReadySummary(LessonSummary existing, UpsertLessonSummaryRequest request) {
        if (existing == null || !"READY".equalsIgnoreCase(existing.getSummaryStatus())) {
            return false;
        }
        return same(existing.getTitle(), request.getTitle())
                && same(existing.getSummaryBrief(), request.getSummaryBrief())
                && same(existing.getSummaryContent(), request.getSummaryContent())
                && same(existing.getHomework(), request.getHomework());
    }

    private boolean same(String left, String right) {
        String normalizedLeft = left == null ? "" : left.trim();
        String normalizedRight = right == null ? "" : right.trim();
        return normalizedLeft.equals(normalizedRight);
    }
}
