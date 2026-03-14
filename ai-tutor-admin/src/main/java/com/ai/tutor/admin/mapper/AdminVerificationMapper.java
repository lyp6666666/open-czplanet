package com.ai.tutor.admin.mapper;

import com.ai.tutor.appointment.model.entity.TeacherProfile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AdminVerificationMapper extends BaseMapper<TeacherProfile> {

    @Select("SELECT * FROM teacher_profile WHERE realname_verify_status = 1 OR edu_verify_status = 1 ORDER BY update_time ASC LIMIT #{offset}, #{limit}")
    List<TeacherProfile> listPendingVerifications(@Param("offset") long offset, @Param("limit") long limit);

    @Select("SELECT COUNT(*) FROM teacher_profile WHERE realname_verify_status = 1 OR edu_verify_status = 1")
    long countPendingVerifications();

    @Select("SELECT * FROM teacher_profile WHERE user_id = #{userId}")
    TeacherProfile selectByUserId(Long userId);

    @Update("UPDATE teacher_profile SET realname_verify_status = 2, realname_verify_time = NOW(), update_time = NOW() WHERE user_id = #{userId} AND realname_verify_status = 1")
    int approveRealname(Long userId);

    @Update("UPDATE teacher_profile SET realname_verify_status = 3, realname_verify_reject_reason = #{reason}, realname_verify_time = NOW(), update_time = NOW() WHERE user_id = #{userId} AND realname_verify_status = 1")
    int rejectRealname(@Param("userId") Long userId, @Param("reason") String reason);

    @Update("UPDATE teacher_profile SET edu_verify_status = 2, edu_verify_time = NOW(), update_time = NOW() WHERE user_id = #{userId} AND edu_verify_status = 1")
    int approveEdu(Long userId);

    @Update("UPDATE teacher_profile SET edu_verify_status = 3, edu_verify_reject_reason = #{reason}, edu_verify_time = NOW(), update_time = NOW() WHERE user_id = #{userId} AND edu_verify_status = 1")
    int rejectEdu(@Param("userId") Long userId, @Param("reason") String reason);
}
