package com.ai.tutor.appointment.model.dto.user;

import lombok.Data;

/**
 * @description:
 * @author: lhw
 * @date: 2025/12/3 17:36
 */
@Data
public class UpdatePhoneRequest {

    private String newPhone;

    private String code;
}

