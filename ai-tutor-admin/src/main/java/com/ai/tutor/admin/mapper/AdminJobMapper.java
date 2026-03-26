package com.ai.tutor.admin.mapper;

import com.ai.tutor.admin.model.entity.StudentJobPosting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AdminJobMapper {

    @Select("SELECT * FROM student_job_posting WHERE status = 0 ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<StudentJobPosting> listPendingJobs(@Param("offset") long offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM student_job_posting WHERE status = 0")
    long countPendingJobs();

    @Update("UPDATE student_job_posting SET status = 1, reject_reason = NULL, update_time = NOW() WHERE id = #{id} AND status = 0")
    int approveJob(@Param("id") Long id);

    @Update("UPDATE student_job_posting SET status = 2, reject_reason = #{reason}, update_time = NOW() WHERE id = #{id} AND status = 0")
    int rejectJob(@Param("id") Long id, @Param("reason") String reason);
}
