package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.LessonSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface LessonSummaryMapper {
    int insert(LessonSummary summary);

    int upsertReady(LessonSummary summary);

    LessonSummary selectByLessonId(@Param("lessonId") Long lessonId);

    LessonSummary selectLatestReadyForUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
