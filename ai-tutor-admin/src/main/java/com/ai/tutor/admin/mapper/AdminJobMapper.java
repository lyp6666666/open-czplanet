package com.ai.tutor.admin.mapper;

import com.ai.tutor.appointment.model.entity.StudentJobPosting;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AdminJobMapper extends BaseMapper<StudentJobPosting> {

    @Select("SELECT * FROM student_job_posting WHERE status = 0 ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<StudentJobPosting> listPendingJobs(@Param("offset") long offset, @Param("limit") long limit);

    @Select("SELECT COUNT(*) FROM student_job_posting WHERE status = 0")
    long countPendingJobs();

    @Update("UPDATE student_job_posting SET status = 1, update_time = NOW() WHERE id = #{id}")
    int approveJob(@Param("id") Long id);

    @Update("UPDATE student_job_posting SET status = 2, reject_reason = #{reason}, update_time = NOW() WHERE id = #{id}")
    int rejectJob(@Param("id") Long id, @Param("reason") String reason);
}
