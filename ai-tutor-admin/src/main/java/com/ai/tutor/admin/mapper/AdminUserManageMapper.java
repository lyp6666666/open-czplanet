package com.ai.tutor.admin.mapper;

import com.ai.tutor.admin.model.vo.AdminUserRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AdminUserManageMapper {

    @Select("SELECT "
            + "u.id AS id, "
            + "u.name AS name, "
            + "u.phone AS phone, "
            + "u.avatar AS avatar, "
            + "u.sex AS sex, "
            + "u.status AS status, "
            + "u.active_status AS activeStatus, "
            + "u.user_type AS userType, "
            + "u.create_time AS createTime, "
            + "u.update_time AS updateTime, "
            + "tp.real_name AS teacherRealName, "
            + "tp.education AS teacherEducation, "
            + "tp.subject AS teacherSubject, "
            + "tp.city AS teacherCity, "
            + "tp.rate_per_hour AS teacherRatePerHour, "
            + "tp.realname_verify_status AS teacherRealnameVerifyStatus, "
            + "tp.edu_verify_status AS teacherEduVerifyStatus, "
            + "tp.home_star_teacher AS teacherHomeStarTeacher, "
            + "tp.status AS teacherProfileStatus "
            + "FROM user u "
            + "LEFT JOIN teacher_profile tp ON tp.user_id = u.id "
            + "WHERE u.user_type = 1 "
            + "AND (#{q} IS NULL OR #{q} = '' OR u.phone LIKE CONCAT('%', #{q}, '%') OR u.name LIKE CONCAT('%', #{q}, '%') OR tp.real_name LIKE CONCAT('%', #{q}, '%')) "
            + "ORDER BY u.create_time DESC "
            + "LIMIT #{offset}, #{size}")
    List<AdminUserRowVO> pageTeachers(@Param("q") String q, @Param("offset") long offset, @Param("size") int size);

    @Select("SELECT COUNT(*) "
            + "FROM user u "
            + "LEFT JOIN teacher_profile tp ON tp.user_id = u.id "
            + "WHERE u.user_type = 1 "
            + "AND (#{q} IS NULL OR #{q} = '' OR u.phone LIKE CONCAT('%', #{q}, '%') OR u.name LIKE CONCAT('%', #{q}, '%') OR tp.real_name LIKE CONCAT('%', #{q}, '%'))")
    long countTeachers(@Param("q") String q);

    @Select("SELECT "
            + "u.id AS id, "
            + "u.name AS name, "
            + "u.phone AS phone, "
            + "u.avatar AS avatar, "
            + "u.sex AS sex, "
            + "u.status AS status, "
            + "u.active_status AS activeStatus, "
            + "u.user_type AS userType, "
            + "u.create_time AS createTime, "
            + "u.update_time AS updateTime, "
            + "sp.real_name AS studentRealName, "
            + "sp.age AS studentAge, "
            + "sp.address AS studentAddress, "
            + "sp.demand_description AS studentDemandDescription, "
            + "sp.budget AS studentBudget, "
            + "sp.status AS studentProfileStatus "
            + "FROM user u "
            + "LEFT JOIN student_profile sp ON sp.user_id = u.id "
            + "WHERE u.user_type = 2 "
            + "AND (#{q} IS NULL OR #{q} = '' OR u.phone LIKE CONCAT('%', #{q}, '%') OR u.name LIKE CONCAT('%', #{q}, '%') OR sp.real_name LIKE CONCAT('%', #{q}, '%')) "
            + "ORDER BY u.create_time DESC "
            + "LIMIT #{offset}, #{size}")
    List<AdminUserRowVO> pageStudents(@Param("q") String q, @Param("offset") long offset, @Param("size") int size);

    @Select("SELECT COUNT(*) "
            + "FROM user u "
            + "LEFT JOIN student_profile sp ON sp.user_id = u.id "
            + "WHERE u.user_type = 2 "
            + "AND (#{q} IS NULL OR #{q} = '' OR u.phone LIKE CONCAT('%', #{q}, '%') OR u.name LIKE CONCAT('%', #{q}, '%') OR sp.real_name LIKE CONCAT('%', #{q}, '%'))")
    long countStudents(@Param("q") String q);
}
