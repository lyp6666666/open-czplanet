package com.ai.tutor.admin.mapper;

import com.ai.tutor.admin.model.entity.TeacherProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AdminVerificationMapper {

    @Select("SELECT * FROM teacher_profile WHERE realname_verify_status = 1 OR edu_verify_status = 1 ORDER BY update_time DESC LIMIT #{offset}, #{size}")
    List<TeacherProfile> listPendingVerifications(@Param("offset") long offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM teacher_profile WHERE realname_verify_status = 1 OR edu_verify_status = 1")
    long countPendingVerifications();

    @Select("SELECT * FROM teacher_profile WHERE user_id = #{userId} LIMIT 1")
    TeacherProfile selectByUserId(@Param("userId") Long userId);

    @Update("UPDATE teacher_profile SET realname_verify_status = 2, realname_verify_time = NOW(), realname_reject_reason = NULL, update_time = NOW() WHERE user_id = #{userId}")
    int approveRealname(@Param("userId") Long userId);

    @Update("UPDATE teacher_profile SET realname_verify_status = 3, realname_reject_reason = #{reason}, update_time = NOW() WHERE user_id = #{userId}")
    int rejectRealname(@Param("userId") Long userId, @Param("reason") String reason);

    @Update("UPDATE teacher_profile SET edu_verify_status = 2, edu_verify_time = NOW(), edu_reject_reason = NULL, update_time = NOW() WHERE user_id = #{userId}")
    int approveEdu(@Param("userId") Long userId);

    @Update("UPDATE teacher_profile SET edu_verify_status = 3, edu_reject_reason = #{reason}, update_time = NOW() WHERE user_id = #{userId}")
    int rejectEdu(@Param("userId") Long userId, @Param("reason") String reason);
}
