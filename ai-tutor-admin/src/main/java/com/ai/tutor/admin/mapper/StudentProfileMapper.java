package com.ai.tutor.admin.mapper;

import com.ai.tutor.admin.model.dto.user.StudentExtInfo;
import com.ai.tutor.admin.model.entity.StudentProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StudentProfileMapper {

    int insert(StudentProfile studentProfile);

    StudentProfile selectByUserId(Long userId);

    int updateStudentProfile(@Param("studentExtInfo") StudentExtInfo studentExtInfo, @Param("userId") Long userId);
}
