package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.TeacherSettlement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TeacherSettlementMapper {

    int insertIgnore(TeacherSettlement settlement);

    TeacherSettlement selectByLessonPaymentOrderId(@Param("lessonPaymentOrderId") Long lessonPaymentOrderId);
}
