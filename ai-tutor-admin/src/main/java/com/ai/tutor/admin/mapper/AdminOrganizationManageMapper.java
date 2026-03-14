package com.ai.tutor.admin.mapper;

import com.ai.tutor.admin.model.vo.AdminOrganizationDetailVO;
import com.ai.tutor.admin.model.vo.AdminOrganizationRowVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AdminOrganizationManageMapper {

    @Select({
            "SELECT",
            "  u.id AS orgUserId,",
            "  op.org_name AS orgName,",
            "  oa.username AS username,",
            "  op.contact_phone AS contactPhone,",
            "  u.status AS userStatus,",
            "  oa.status AS accountStatus,",
            "  oa.must_change_password AS mustChangePassword,",
            "  oa.last_login_time AS lastLoginTime,",
            "  u.create_time AS createTime,",
            "  u.update_time AS updateTime",
            "FROM user u",
            "LEFT JOIN organization_profile op ON op.user_id = u.id",
            "LEFT JOIN organization_account oa ON oa.org_user_id = u.id",
            "WHERE u.user_type = 3",
            "  AND (",
            "    #{q} IS NULL OR #{q} = ''",
            "    OR op.org_name LIKE CONCAT('%', #{q}, '%')",
            "    OR oa.username LIKE CONCAT('%', #{q}, '%')",
            "    OR u.phone LIKE CONCAT('%', #{q}, '%')",
            "    OR op.contact_phone LIKE CONCAT('%', #{q}, '%')",
            "  )",
            "ORDER BY u.id DESC",
            "LIMIT #{offset}, #{limit}"
    })
    List<AdminOrganizationRowVO> page(@Param("q") String q,
                                     @Param("offset") long offset,
                                     @Param("limit") long limit);

    @Select({
            "SELECT COUNT(*)",
            "FROM user u",
            "LEFT JOIN organization_profile op ON op.user_id = u.id",
            "LEFT JOIN organization_account oa ON oa.org_user_id = u.id",
            "WHERE u.user_type = 3",
            "  AND (",
            "    #{q} IS NULL OR #{q} = ''",
            "    OR op.org_name LIKE CONCAT('%', #{q}, '%')",
            "    OR oa.username LIKE CONCAT('%', #{q}, '%')",
            "    OR u.phone LIKE CONCAT('%', #{q}, '%')",
            "    OR op.contact_phone LIKE CONCAT('%', #{q}, '%')",
            "  )"
    })
    long count(@Param("q") String q);

    @Select({
            "SELECT",
            "  u.id AS orgUserId,",
            "  op.org_name AS orgName,",
            "  oa.username AS username,",
            "  oa.status AS accountStatus,",
            "  oa.must_change_password AS mustChangePassword,",
            "  oa.last_login_time AS lastLoginTime,",
            "  u.status AS userStatus,",
            "  op.contact_name AS contactName,",
            "  op.contact_phone AS contactPhone,",
            "  op.address AS address,",
            "  op.intro AS intro,",
            "  op.license_no AS licenseNo,",
            "  op.split_platform_percent AS splitPlatformPercent,",
            "  op.split_org_percent AS splitOrgPercent,",
            "  u.create_time AS createTime,",
            "  u.update_time AS updateTime",
            "FROM user u",
            "LEFT JOIN organization_profile op ON op.user_id = u.id",
            "LEFT JOIN organization_account oa ON oa.org_user_id = u.id",
            "WHERE u.id = #{orgUserId} AND u.user_type = 3",
            "LIMIT 1"
    })
    AdminOrganizationDetailVO selectDetail(@Param("orgUserId") Long orgUserId);

    @Select("SELECT org_user_id FROM organization_account WHERE username = #{username} LIMIT 1")
    Long selectOrgUserIdByUsername(@Param("username") String username);

    @Update({
            "UPDATE user",
            "SET name = COALESCE(#{name}, name),",
            "    phone = COALESCE(#{phone}, phone),",
            "    update_time = NOW(3)",
            "WHERE id = #{orgUserId} AND user_type = 3"
    })
    int updateOrgUserBase(@Param("orgUserId") Long orgUserId,
                          @Param("name") String name,
                          @Param("phone") String phone);

    @Update({
            "UPDATE organization_profile",
            "SET org_name = COALESCE(#{orgName}, org_name),",
            "    intro = COALESCE(#{intro}, intro),",
            "    contact_name = COALESCE(#{contactName}, contact_name),",
            "    contact_phone = COALESCE(#{contactPhone}, contact_phone),",
            "    address = COALESCE(#{address}, address),",
            "    license_no = COALESCE(#{licenseNo}, license_no),",
            "    split_platform_percent = COALESCE(#{splitPlatformPercent}, split_platform_percent),",
            "    split_org_percent = COALESCE(#{splitOrgPercent}, split_org_percent),",
            "    update_time = NOW(3)",
            "WHERE user_id = #{orgUserId}"
    })
    int updateOrgProfile(@Param("orgUserId") Long orgUserId,
                         @Param("orgName") String orgName,
                         @Param("intro") String intro,
                         @Param("contactName") String contactName,
                         @Param("contactPhone") String contactPhone,
                         @Param("address") String address,
                         @Param("licenseNo") String licenseNo,
                         @Param("splitPlatformPercent") Integer splitPlatformPercent,
                         @Param("splitOrgPercent") Integer splitOrgPercent);

    @Update({
            "UPDATE organization_account",
            "SET username = COALESCE(#{username}, username),",
            "    status = COALESCE(#{status}, status),",
            "    update_time = NOW(3)",
            "WHERE org_user_id = #{orgUserId}"
    })
    int updateOrgAccount(@Param("orgUserId") Long orgUserId,
                         @Param("username") String username,
                         @Param("status") Integer status);

    @Update({
            "UPDATE organization_account",
            "SET password_hash = #{passwordHash},",
            "    must_change_password = #{mustChangePassword},",
            "    update_time = NOW(3)",
            "WHERE org_user_id = #{orgUserId}"
    })
    int resetOrgAccountPassword(@Param("orgUserId") Long orgUserId,
                                @Param("passwordHash") String passwordHash,
                                @Param("mustChangePassword") Integer mustChangePassword);

    @Update({
            "UPDATE organization_account",
            "SET must_change_password = #{mustChangePassword},",
            "    update_time = NOW(3)",
            "WHERE org_user_id = #{orgUserId}"
    })
    int updateOrgAccountMustChangePassword(@Param("orgUserId") Long orgUserId,
                                          @Param("mustChangePassword") Integer mustChangePassword);

    @Update({
            "UPDATE organization_account",
            "SET status = 0,",
            "    update_time = NOW(3)",
            "WHERE org_user_id = #{orgUserId}"
    })
    int disableOrgAccount(@Param("orgUserId") Long orgUserId);

    @Update({
            "UPDATE organization_profile",
            "SET status = 0,",
            "    update_time = NOW(3)",
            "WHERE user_id = #{orgUserId}"
    })
    int disableOrgProfile(@Param("orgUserId") Long orgUserId);

    @Update({
            "UPDATE student_profile",
            "SET status = 0,",
            "    update_time = NOW(3)",
            "WHERE user_id = #{orgUserId}"
    })
    int disableStudentProfile(@Param("orgUserId") Long orgUserId);

    @Update({
            "UPDATE user",
            "SET status = 1,",
            "    update_time = NOW(3)",
            "WHERE id = #{orgUserId} AND user_type = 3"
    })
    int blacklistOrgUser(@Param("orgUserId") Long orgUserId);
}

