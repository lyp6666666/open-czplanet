package com.ai.tutor.videocallimservice.chat.mapper;

import com.ai.tutor.videocallimservice.chat.domain.entity.StudentJobPostingLite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StudentJobPostingLiteMapper {

    @Select("SELECT id, class_mode AS classMode, frequency_per_week AS frequencyPerWeek, budget_min AS budgetMin, budget_max AS budgetMax FROM student_job_posting WHERE id = #{id}")
    StudentJobPostingLite selectById(@Param("id") Long id);

    @Update("UPDATE student_job_posting SET biz_status = #{status} WHERE id = #{id}")
    int updateBizStatus(@Param("id") Long id, @Param("status") Integer status);
}
