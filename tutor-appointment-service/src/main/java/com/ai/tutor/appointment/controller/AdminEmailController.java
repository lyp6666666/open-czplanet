package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.vo.admin.EmailTaskDetailVO;
import com.ai.tutor.appointment.model.vo.admin.EmailTaskRowVO;
import com.ai.tutor.appointment.model.vo.admin.PageResult;
import com.ai.tutor.appointment.model.vo.email.InternalUserEmailsVO;
import com.ai.tutor.appointment.service.EmailAccountService;
import com.ai.tutor.appointment.service.EmailAdminService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.ResultUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/email")
public class AdminEmailController {

    @Resource
    private EmailAdminService emailAdminService;
    @Resource
    private EmailAccountService emailAccountService;

    @GetMapping("/tasks")
    public BaseResponse<PageResult<EmailTaskRowVO>> pageTasks(@RequestParam(value = "page", defaultValue = "1") int page,
                                                              @RequestParam(value = "size", defaultValue = "20") int size,
                                                              @RequestParam(value = "userId", required = false) Long userId,
                                                              @RequestParam(value = "email", required = false) String email,
                                                              @RequestParam(value = "templateCode", required = false) String templateCode,
                                                              @RequestParam(value = "bizType", required = false) String bizType,
                                                              @RequestParam(value = "status", required = false) String status) {
        return ResultUtils.success(emailAdminService.pageTasks(page, size, userId, email, templateCode, bizType, status));
    }

    @GetMapping("/tasks/{id}")
    public BaseResponse<EmailTaskDetailVO> getTaskDetail(@PathVariable("id") Long id) {
        return ResultUtils.success(emailAdminService.getTaskDetail(id));
    }

    @GetMapping("/users/{uid}/emails")
    public BaseResponse<InternalUserEmailsVO> getUserEmails(@PathVariable("uid") Long uid) {
        return ResultUtils.success(emailAccountService.getInternalUserEmails(uid));
    }

    @PostMapping("/tasks/{id}/retry")
    public BaseResponse<Boolean> retryTask(@PathVariable("id") Long id) {
        return ResultUtils.success(emailAdminService.retryTask(id));
    }
}
