package com.ai.tutor.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminDashboardMapper {

    @Select("SELECT COUNT(*) FROM user")
    Long countTotalUsers();

    @Select("SELECT COUNT(*) FROM teacher_profile WHERE status = 1")
    Long countActiveTeachers();

    @Select("SELECT COUNT(*) FROM student_job_posting WHERE status = 0")
    Long countPendingJobs();

    @Select("SELECT COUNT(*) FROM teacher_profile WHERE realname_verify_status = 1 OR edu_verify_status = 1")
    Long countPendingVerifications();

    @Select("SELECT COUNT(*) FROM brokerage_order WHERE status = 'DISPUTE'")
    Long countPendingRefunds();
}
