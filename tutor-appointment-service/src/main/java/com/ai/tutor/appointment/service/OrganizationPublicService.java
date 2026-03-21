package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.entity.OrganizationProfile;

public interface OrganizationPublicService {

    OrganizationProfile getByOrgUserId(Long orgUserId);
}
