package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.OrganizationProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrganizationProfileMapper {

    OrganizationProfile selectByUserId(@Param("userId") Long userId);

    int insert(OrganizationProfile profile);
}
