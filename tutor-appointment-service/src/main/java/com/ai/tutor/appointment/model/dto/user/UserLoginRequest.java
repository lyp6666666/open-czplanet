package com.ai.tutor.appointment.model.dto.user;


import com.ai.tutor.appointment.enums.UserRoleEnum;
import lombok.Data;

@Data
public class UserLoginRequest {

    private String phone;
    private String code;
    private UserRoleEnum userRoleEnum;
}
