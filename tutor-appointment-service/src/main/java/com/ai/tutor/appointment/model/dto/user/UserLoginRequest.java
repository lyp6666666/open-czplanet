package com.ai.tutor.appointment.model.dto.user;


import com.ai.tutor.appointment.enums.UserRoleEnum;
import lombok.Data;

@Data
public class UserLoginRequest {

    private String phone;
    private String code;
    private UserRoleEnum userRoleEnum;

    /**
     * 邀请码为注册阶段的选填字段。
     * 若当前手机号对应用户已存在，则后端忽略该字段，不重复绑定邀请关系。
     */
    private String inviteCode;
}
