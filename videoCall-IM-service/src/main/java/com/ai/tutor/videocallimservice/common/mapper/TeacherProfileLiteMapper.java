package com.ai.tutor.videocallimservice.common.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TeacherProfileLiteMapper {

    Long selectIdByUserId(@Param("userId") Long userId);

    Long selectUserIdById(@Param("id") Long id);
}
