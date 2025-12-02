package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.dto.user.TeacherExtInfo;
import com.ai.tutor.appointment.model.entity.TeacherProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TeacherProfileMapper {

    int insert(TeacherProfile teacherProfile);

    TeacherProfile selectByUserId(Long userId);

    int updateTeacherProfile(@Param("teacherExtInfo") TeacherExtInfo teacherExtInfo, @Param("userId") Long userId);
}
