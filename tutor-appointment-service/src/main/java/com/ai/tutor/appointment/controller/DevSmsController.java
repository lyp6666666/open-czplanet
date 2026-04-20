package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.config.OpsProperties;
import com.ai.tutor.appointment.enums.RedisKeyPrefix;
import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.ai.tutor.appointment.model.vo.LoginUserVO;
import com.ai.tutor.appointment.service.UserService;
import com.ai.tutor.appointment.service.impl.SmsServiceImpl;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/dev/sms")
public class DevSmsController {

    @Value("${dev.exposeSmsCode:false}")
    private boolean exposeSmsCode;

    @Resource
    private OpsProperties opsProperties;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private SmsServiceImpl smsService;

    @Resource
    private UserService userService;

    @GetMapping("/code")
    public BaseResponse<String> code(@RequestParam("phone") String phone,
                                    @RequestHeader(value = "X-Ops-Token", required = false) String token) {
        ThrowUtils.throwIf(!exposeSmsCode, ErrorCode.NO_AUTH_ERROR, "dev endpoint disabled");
        String expected = opsProperties == null ? null : opsProperties.getVerifyToken();
        ThrowUtils.throwIf(expected == null || expected.isBlank(), ErrorCode.NO_AUTH_ERROR, "ops token not configured");
        ThrowUtils.throwIf(token == null || !token.equals(expected), ErrorCode.NO_AUTH_ERROR, "ops token invalid");
        ThrowUtils.throwIf(phone == null || phone.trim().isEmpty(), ErrorCode.PARAMS_ERROR);
        String p = phone.trim();
        String key = RedisKeyPrefix.SMS_CODE.key(p);
        Object v = null;
        try {
            v = redisTemplate.opsForValue().get(key);
        } catch (Exception ignored) {
        }
        if (v != null) {
            return ResultUtils.success(String.valueOf(v));
        }
        String local = smsService.debugPeekCode(p, RedisKeyPrefix.SMS_CODE.getPrefix());
        ThrowUtils.throwIf(local == null, ErrorCode.NOT_FOUND_ERROR, "code not found");
        return ResultUtils.success(local);
    }

    @GetMapping("/login")
    public BaseResponse<LoginUserVO> login(@RequestParam("phone") String phone,
                                           @RequestParam("role") String role,
                                           @RequestHeader(value = "X-Ops-Token", required = false) String token) {
        ThrowUtils.throwIf(!exposeSmsCode, ErrorCode.NO_AUTH_ERROR, "dev endpoint disabled");
        String expected = opsProperties == null ? null : opsProperties.getVerifyToken();
        ThrowUtils.throwIf(expected == null || expected.isBlank(), ErrorCode.NO_AUTH_ERROR, "ops token not configured");
        ThrowUtils.throwIf(token == null || !token.equals(expected), ErrorCode.NO_AUTH_ERROR, "ops token invalid");
        ThrowUtils.throwIf(phone == null || phone.trim().isEmpty(), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(role == null || role.trim().isEmpty(), ErrorCode.PARAMS_ERROR);

        String normalizedPhone = phone.trim();
        UserRoleEnum userRoleEnum = UserRoleEnum.valueOf(role.trim().toUpperCase());
        String code = smsService.sendCode(normalizedPhone, RedisKeyPrefix.SMS_CODE.getPrefix());
        return ResultUtils.success(userService.userLoginOrRegister(normalizedPhone, code, userRoleEnum, null));
    }
}
