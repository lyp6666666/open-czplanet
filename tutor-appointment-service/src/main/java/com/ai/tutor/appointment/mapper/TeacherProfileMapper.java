package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.TeacherProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TeacherProfileMapper {

    int insert(TeacherProfile teacherProfile);

    TeacherProfile selectByUserId(Long userId);
}
