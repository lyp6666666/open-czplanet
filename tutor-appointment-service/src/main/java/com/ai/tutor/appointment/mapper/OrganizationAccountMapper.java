package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.OrganizationAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrganizationAccountMapper {

    OrganizationAccount selectByUsername(@Param("username") String username);

    OrganizationAccount selectByOrgUserId(@Param("orgUserId") Long orgUserId);

    int insert(OrganizationAccount account);

    int updatePassword(@Param("orgUserId") Long orgUserId,
                       @Param("passwordHash") String passwordHash,
                       @Param("mustChangePassword") Integer mustChangePassword);

    int updateLastLogin(@Param("orgUserId") Long orgUserId);
}
