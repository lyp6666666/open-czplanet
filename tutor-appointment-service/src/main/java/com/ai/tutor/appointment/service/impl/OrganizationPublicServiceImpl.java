package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.mapper.OrganizationProfileMapper;
import com.ai.tutor.appointment.model.entity.OrganizationProfile;
import com.ai.tutor.appointment.service.OrganizationPublicService;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class OrganizationPublicServiceImpl implements OrganizationPublicService {

    @Resource
    private OrganizationProfileMapper organizationProfileMapper;

    @Override
    public OrganizationProfile getByOrgUserId(Long orgUserId) {
        ThrowUtils.throwIf(orgUserId == null, ErrorCode.PARAMS_ERROR);
        OrganizationProfile profile = organizationProfileMapper.selectByUserId(orgUserId);
        ThrowUtils.throwIf(profile == null, ErrorCode.NOT_FOUND_ERROR);
        return profile;
    }
}
