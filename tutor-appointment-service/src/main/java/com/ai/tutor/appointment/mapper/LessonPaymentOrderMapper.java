package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.LessonPaymentOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface LessonPaymentOrderMapper {

    int insert(LessonPaymentOrder order);

    LessonPaymentOrder selectById(@Param("id") Long id);

    LessonPaymentOrder selectByLessonId(@Param("lessonId") Long lessonId);

    LessonPaymentOrder selectUnpaidByCourseId(@Param("courseId") Long courseId);

    LessonPaymentOrder selectFirstUnpaidBeforeLesson(@Param("courseId") Long courseId, @Param("lessonId") Long lessonId);

    int markPaying(@Param("id") Long id);

    int markPaid(@Param("id") Long id,
                 @Param("paymentOrderNo") String paymentOrderNo,
                 @Param("paidAt") LocalDateTime paidAt);
}
