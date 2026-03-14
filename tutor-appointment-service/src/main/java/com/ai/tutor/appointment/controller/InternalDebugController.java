package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.service.SmsService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.ResultUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/debug")
@Profile({"qa", "test"})
@RequiredArgsConstructor
public class InternalDebugController {

    private final SmsService smsService;

    @GetMapping("/sms-code")
    public BaseResponse<String> smsCode(@RequestParam("phone") String phone, @RequestParam(value = "prefix", required = false) String prefix) {
        String p = StringUtils.hasText(prefix) ? prefix : "sms:code:";
        return ResultUtils.success(smsService.debugPeekCode(phone, p));
    }
}
