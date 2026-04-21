package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.dto.email.SendEmailCodeRequest;
import com.ai.tutor.appointment.model.dto.email.VerifyEmailRequest;
import com.ai.tutor.appointment.model.vo.email.EmailCodeVO;
import com.ai.tutor.appointment.model.vo.email.EmailReminderHintVO;
import com.ai.tutor.appointment.model.vo.email.UserEmailItemVO;
import com.ai.tutor.appointment.model.vo.email.UserEmailStatusVO;
import com.ai.tutor.appointment.service.EmailAccountService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/email")
public class UserEmailController {

    @Resource
    private EmailAccountService emailAccountService;

    @GetMapping
    public BaseResponse<UserEmailStatusVO> status(HttpServletRequest request) {
        return ResultUtils.success(emailAccountService.getStatus(requireUid(request)));
    }

    @PostMapping("/code")
    public BaseResponse<EmailCodeVO> sendCode(@Valid @RequestBody SendEmailCodeRequest body, HttpServletRequest request) {
        return ResultUtils.success(emailAccountService.sendCode(requireUid(request), body, request.getRemoteAddr()));
    }

    @PostMapping("/verify")
    public BaseResponse<UserEmailItemVO> verify(@Valid @RequestBody VerifyEmailRequest body, HttpServletRequest request) {
        return ResultUtils.success(emailAccountService.verify(requireUid(request), body));
    }

    @DeleteMapping("/summary")
    public BaseResponse<Boolean> deleteSummary(HttpServletRequest request) {
        return ResultUtils.success(emailAccountService.deleteSummaryEmail(requireUid(request)));
    }

    @GetMapping("/reminder-hints")
    public BaseResponse<EmailReminderHintVO> reminderHints(@RequestParam(value = "scene", required = false) String scene,
                                                           HttpServletRequest request) {
        return ResultUtils.success(emailAccountService.getReminderHint(requireUid(request), scene));
    }

    private Long requireUid(HttpServletRequest request) {
        String uidStr = (String) request.getAttribute(com.ai.tutor.utils.RequestHolder.ATTRIBUTE_UID);
        ThrowUtils.throwIf(uidStr == null, ErrorCode.NOT_LOGIN_ERROR);
        return Long.parseLong(uidStr);
    }
}
