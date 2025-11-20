package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.StudentProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentProfileMapper {

    int insert(StudentProfile studentProfile);

    StudentProfile selectByUserId(Long userId);
}
