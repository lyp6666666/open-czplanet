package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.common.BaseResponse;
import com.ai.tutor.appointment.enums.ErrorCode;
import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.ai.tutor.appointment.model.dto.user.UserLoginRequest;
import com.ai.tutor.appointment.model.vo.LoginUserVO;
import com.ai.tutor.appointment.service.UserService;
import com.ai.tutor.appointment.service.impl.SmsServiceImpl;
import com.ai.tutor.appointment.utils.ResultUtils;
import com.ai.tutor.appointment.utils.ThrowUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Tag(name = "用户模块接口", description = "提供用户登录、注册、验证码发送等相关接口")
public class UserController {

    @Resource
    private SmsServiceImpl smsServiceImpl;
    @Resource
    private UserService userService;

    /**
     *  获取验证码
     * @param phone
     * @return
     */
    @PostMapping("/sendCode")
    @Operation(summary = "发送验证码", description = "根据手机号发送验证码（模拟）")
    public BaseResponse<String> sendCode(String phone) {
        ThrowUtils.throwIf(phone == null || phone.isEmpty(), ErrorCode.PARAMS_ERROR);
        // 生成并发送验证码
        smsServiceImpl.sendCode(phone);
        return ResultUtils.success("验证码发送成功(模拟)");
    }

    /**
     * 用户手脚登录注册功能
     * @param userLoginRequest
     * @return
     */
    @PostMapping("/loginOrRegister")
    @Operation(summary = "手机号验证码登录或注册",
            description = "使用手机号+验证码登录，若用户不存在则自动注册。userRoleEnum 指定注册角色（TEACHER/ STUDENT）")
    public BaseResponse<LoginUserVO> loginOrRegister(@RequestBody UserLoginRequest userLoginRequest) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String phone = userLoginRequest.getPhone();
        String code = userLoginRequest.getCode();
        UserRoleEnum userRoleEnum = userLoginRequest.getUserRoleEnum();
        LoginUserVO loginUserVO = userService.userLoginOrRegister(phone, code, userRoleEnum);
        return ResultUtils.success(loginUserVO);
    }
}
