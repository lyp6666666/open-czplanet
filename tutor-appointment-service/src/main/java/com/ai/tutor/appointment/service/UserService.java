package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.ai.tutor.appointment.model.dto.user.UpdatePhoneRequest;
import com.ai.tutor.appointment.model.dto.user.UserUpdateRequest;
import com.ai.tutor.appointment.model.vo.LoginUserVO;
import jakarta.servlet.http.HttpServletRequest;

public interface UserService {
    LoginUserVO userLoginOrRegister(String phone, String code, UserRoleEnum userRoleEnum);

    void updateUserInfo(UserUpdateRequest requestDto, HttpServletRequest request);

    void updateUserPhone(UpdatePhoneRequest requestDto, HttpServletRequest request);

}
