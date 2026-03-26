package com.ai.tutor.admin.mapper;

import com.ai.tutor.admin.model.vo.AdminOrganizationDetailVO;
import com.ai.tutor.admin.model.vo.AdminOrganizationRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AdminOrganizationManageMapper {

    @Select("SELECT "
            + "u.id AS orgUserId, "
            + "p.org_name AS orgName, "
            + "a.username AS username, "
            + "a.status AS accountStatus, "
            + "a.must_change_password AS mustChangePassword, "
            + "a.last_login_time AS lastLoginTime, "
            + "u.status AS userStatus, "
            + "p.contact_phone AS contactPhone, "
            + "u.create_time AS createTime, "
            + "u.update_time AS updateTime "
            + "FROM user u "
            + "LEFT JOIN organization_profile p ON p.user_id = u.id "
            + "LEFT JOIN organization_account a ON a.org_user_id = u.id "
            + "WHERE u.user_type = 3 "
            + "AND (#{q} IS NULL OR #{q} = '' OR p.org_name LIKE CONCAT('%', #{q}, '%') OR a.username LIKE CONCAT('%', #{q}, '%') OR u.phone LIKE CONCAT('%', #{q}, '%')) "
            + "ORDER BY u.create_time DESC "
            + "LIMIT #{offset}, #{size}")
    List<AdminOrganizationRowVO> page(@Param("q") String q, @Param("offset") long offset, @Param("size") int size);

    @Select("SELECT COUNT(*) "
            + "FROM user u "
            + "LEFT JOIN organization_profile p ON p.user_id = u.id "
            + "LEFT JOIN organization_account a ON a.org_user_id = u.id "
            + "WHERE u.user_type = 3 "
            + "AND (#{q} IS NULL OR #{q} = '' OR p.org_name LIKE CONCAT('%', #{q}, '%') OR a.username LIKE CONCAT('%', #{q}, '%') OR u.phone LIKE CONCAT('%', #{q}, '%'))")
    long count(@Param("q") String q);

    @Select("SELECT "
            + "u.id AS orgUserId, "
            + "p.org_name AS orgName, "
            + "a.username AS username, "
            + "a.status AS accountStatus, "
            + "a.must_change_password AS mustChangePassword, "
            + "a.last_login_time AS lastLoginTime, "
            + "u.status AS userStatus, "
            + "p.contact_name AS contactName, "
            + "p.contact_phone AS contactPhone, "
            + "p.address AS address, "
            + "p.intro AS intro, "
            + "p.license_no AS licenseNo, "
            + "p.split_platform_percent AS splitPlatformPercent, "
            + "p.split_org_percent AS splitOrgPercent, "
            + "p.create_time AS createTime, "
            + "p.update_time AS updateTime "
            + "FROM user u "
            + "LEFT JOIN organization_profile p ON p.user_id = u.id "
            + "LEFT JOIN organization_account a ON a.org_user_id = u.id "
            + "WHERE u.id = #{orgUserId} AND u.user_type = 3 "
            + "LIMIT 1")
    AdminOrganizationDetailVO selectDetail(@Param("orgUserId") Long orgUserId);

    @Select("SELECT org_user_id FROM organization_account WHERE username = #{username} LIMIT 1")
    Long selectOrgUserIdByUsername(@Param("username") String username);

    @Update("UPDATE user SET "
            + "name = COALESCE(#{orgName}, name), "
            + "phone = COALESCE(#{contactPhone}, phone), "
            + "update_time = NOW() "
            + "WHERE id = #{orgUserId}")
    int updateOrgUserBase(@Param("orgUserId") Long orgUserId, @Param("orgName") String orgName, @Param("contactPhone") String contactPhone);

    @Update("UPDATE organization_profile SET "
            + "org_name = COALESCE(#{orgName}, org_name), "
            + "intro = COALESCE(#{intro}, intro), "
            + "contact_name = COALESCE(#{contactName}, contact_name), "
            + "contact_phone = COALESCE(#{contactPhone}, contact_phone), "
            + "address = COALESCE(#{address}, address), "
            + "license_no = COALESCE(#{licenseNo}, license_no), "
            + "split_platform_percent = COALESCE(#{splitPlatformPercent}, split_platform_percent), "
            + "split_org_percent = COALESCE(#{splitOrgPercent}, split_org_percent), "
            + "update_time = NOW() "
            + "WHERE user_id = #{orgUserId}")
    int updateOrgProfile(@Param("orgUserId") Long orgUserId,
                         @Param("orgName") String orgName,
                         @Param("intro") String intro,
                         @Param("contactName") String contactName,
                         @Param("contactPhone") String contactPhone,
                         @Param("address") String address,
                         @Param("licenseNo") String licenseNo,
                         @Param("splitPlatformPercent") Integer splitPlatformPercent,
                         @Param("splitOrgPercent") Integer splitOrgPercent);

    @Update("UPDATE organization_account SET "
            + "username = COALESCE(#{username}, username), "
            + "status = COALESCE(#{accountStatus}, status), "
            + "update_time = NOW() "
            + "WHERE org_user_id = #{orgUserId}")
    int updateOrgAccount(@Param("orgUserId") Long orgUserId, @Param("username") String username, @Param("accountStatus") Integer accountStatus);

    @Update("UPDATE organization_account SET "
            + "password_hash = #{hash}, "
            + "must_change_password = #{mustChangePassword}, "
            + "update_time = NOW() "
            + "WHERE org_user_id = #{orgUserId}")
    int resetOrgAccountPassword(@Param("orgUserId") Long orgUserId, @Param("hash") String hash, @Param("mustChangePassword") Integer mustChangePassword);

    @Update("UPDATE organization_account SET status = 0, update_time = NOW() WHERE org_user_id = #{orgUserId}")
    int disableOrgAccount(@Param("orgUserId") Long orgUserId);

    @Update("UPDATE organization_profile SET status = 0, update_time = NOW() WHERE user_id = #{orgUserId}")
    int disableOrgProfile(@Param("orgUserId") Long orgUserId);

    @Update("UPDATE student_profile SET status = 0, update_time = NOW() WHERE user_id = #{orgUserId}")
    int disableStudentProfile(@Param("orgUserId") Long orgUserId);

    @Update("UPDATE user SET status = 1, update_time = NOW() WHERE id = #{orgUserId}")
    int blacklistOrgUser(@Param("orgUserId") Long orgUserId);
}
