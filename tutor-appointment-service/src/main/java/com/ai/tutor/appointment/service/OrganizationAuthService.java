package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.dto.organization.OrgChangePasswordRequest;
import com.ai.tutor.appointment.model.dto.organization.OrgLoginRequest;
import com.ai.tutor.appointment.model.vo.OrgLoginVO;

public interface OrganizationAuthService {

    OrgLoginVO login(OrgLoginRequest request);

    void changePassword(Long orgUserId, OrgChangePasswordRequest request);
}
