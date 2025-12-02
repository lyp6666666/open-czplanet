package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.dto.user.StudentExtInfo;
import com.ai.tutor.appointment.model.entity.StudentProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StudentProfileMapper {

    int insert(StudentProfile studentProfile);

    StudentProfile selectByUserId(Long userId);

    int updateStudentProfile(@Param("studentExtInfo") StudentExtInfo studentExtInfo, @Param("userId") Long userId);
}
