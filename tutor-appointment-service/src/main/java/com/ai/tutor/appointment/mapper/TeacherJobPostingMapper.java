package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.TeacherJobPosting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TeacherJobPostingMapper {

    int insert(TeacherJobPosting posting);

    int updateById(TeacherJobPosting posting);

    TeacherJobPosting selectById(@Param("id") Long id);

    List<TeacherJobPosting> listByTutorId(@Param("tutorId") Long tutorId,
                                         @Param("cursor") Long cursor,
                                         @Param("pageSize") Integer pageSize);

    List<TeacherJobPosting> listPublished(@Param("subjectId") Long subjectId,
                                         @Param("city") String city,
                                         @Param("mode") String mode,
                                         @Param("cursor") Long cursor,
                                         @Param("pageSize") Integer pageSize);
}

