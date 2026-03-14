package com.ai.tutor.appointment.model.dto.user;

import lombok.Data;

@Data
public class UpdatePhoneV2Request {

    private String newPhone;

    private String oldCode;

    private String newCode;
}
