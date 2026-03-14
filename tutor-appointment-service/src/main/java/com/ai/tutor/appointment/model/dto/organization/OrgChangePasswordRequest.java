package com.ai.tutor.appointment.model.dto.organization;

import lombok.Data;

@Data
public class OrgChangePasswordRequest {

    private String oldPassword;

    private String newPassword;
}
