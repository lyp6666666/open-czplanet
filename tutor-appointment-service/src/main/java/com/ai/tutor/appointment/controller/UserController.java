package com.ai.tutor.appointment.controller;


import com.ai.tutor.appointment.enums.RedisKeyPrefix;
import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.ai.tutor.appointment.model.dto.user.SendCodeRequest;
import com.ai.tutor.appointment.model.dto.user.UserLoginRequest;
import com.ai.tutor.appointment.model.dto.user.UserUpdateRequest;
import com.ai.tutor.appointment.model.vo.LoginUserVO;
import com.ai.tutor.appointment.service.UserService;
import com.ai.tutor.appointment.service.impl.SmsServiceImpl;
import com.ai.tutor.appointment.utils.ResultUtils;
import com.ai.tutor.appointment.utils.ThrowUtils;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.annotation.FrequencyControl;
import com.ai.tutor.enums.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Tag(name = "用户模块接口", description = "提供用户登录、注册、验证码发送等相关接口")
public class UserController {

    @Resource
    private SmsServiceImpl smsService;
    @Resource
    private UserService userService;

    /**
     *  获取验证码
     * @param sendCodeRequest
     * @return
     */
    @PostMapping("/sendcode")
    @Operation(summary = "发送验证码", description = "根据手机号发送验证码（模拟）")
    @FrequencyControl(
            prefixKey = "sms:send:",   // Redis key 前缀
            target = FrequencyControl.Target.EL,  // 通过 EL 取手机号
            spEl = "#phone",           // 取入参 phone 参数
            time = 1,                  // 时间范围（1 分钟）
            unit = TimeUnit.MINUTES,   // 时间单位：分钟
            count = 1                  // 限制 1 次
    )
    @FrequencyControl(
            prefixKey = "sms:send:ip:",
            target = FrequencyControl.Target.IP,
            time = 1,
            unit = TimeUnit.MINUTES,
            count = 5
    )
    public BaseResponse<String> sendCode(@RequestBody SendCodeRequest sendCodeRequest) {
        ThrowUtils.throwIf(sendCodeRequest.getPhone() == null || sendCodeRequest.getPhone().isEmpty(), ErrorCode.PARAMS_ERROR);
        // 生成并发送验证码
        smsService.sendCode(sendCodeRequest.getPhone(), RedisKeyPrefix.SMS_CODE.getPrefix());
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

    @PostMapping("/updateUserInfo")
    @Operation(summary = "更新用户信息", description = "更新用户信息")
    public BaseResponse<String> updateUserInfo(@RequestBody UserUpdateRequest requestDto, HttpServletRequest request) {

        userService.updateUserInfo(requestDto,request);
        return ResultUtils.success("更新成功");
    }


}
