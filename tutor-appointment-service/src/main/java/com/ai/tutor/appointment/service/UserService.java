package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.ai.tutor.appointment.model.vo.LoginUserVO;

public interface UserService {
    LoginUserVO userLoginOrRegister(String phone, String code, UserRoleEnum userRoleEnum);

}
