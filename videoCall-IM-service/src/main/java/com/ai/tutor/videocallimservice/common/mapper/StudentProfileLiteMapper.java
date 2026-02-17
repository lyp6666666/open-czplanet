package com.ai.tutor.videocallimservice.common.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StudentProfileLiteMapper {

    @Select("SELECT id FROM student_profile WHERE user_id = #{userId} LIMIT 1")
    Long selectIdByUserId(@Param("userId") Long userId);

    @Select("SELECT user_id FROM student_profile WHERE id = #{id} LIMIT 1")
    Long selectUserIdById(@Param("id") Long id);
}

