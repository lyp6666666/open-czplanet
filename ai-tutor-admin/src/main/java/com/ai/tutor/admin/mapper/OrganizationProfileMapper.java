package com.ai.tutor.admin.mapper;

import com.ai.tutor.admin.model.entity.OrganizationProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrganizationProfileMapper {

    OrganizationProfile selectByUserId(@Param("userId") Long userId);

    int insert(OrganizationProfile profile);
}
