package com.ai.tutor.videocallimservice.chat.mapper;

import com.ai.tutor.videocallimservice.chat.domain.entity.StudentJobPostingLite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StudentJobPostingLiteMapper {
    StudentJobPostingLite selectById(@Param("id") Long id);

    int updateBizStatus(@Param("id") Long id, @Param("bizStatus") Integer bizStatus);
}
