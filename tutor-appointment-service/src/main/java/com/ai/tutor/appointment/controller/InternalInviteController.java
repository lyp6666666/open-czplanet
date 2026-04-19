package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.service.InviteService;
import com.ai.tutor.appointment.model.vo.invite.InviteSystemBenefitVO;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.event.InviteBrokeragePaidEvent;
import com.ai.tutor.common.integration.InviteSystemBenefitInfo;
import com.ai.tutor.utils.ResultUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 邀请返利内部接口。
 *
 * <p>仅供平台内部服务调用，不直接暴露给前端。网关/服务间签名负责鉴权，业务层保持幂等。</p>
 */
@RestController
@RequestMapping("/internal/facade/invite")
@RequiredArgsConstructor
public class InternalInviteController {

    private final InviteService inviteService;

    @PostMapping("/brokerage-paid")
    public BaseResponse<Boolean> onBrokeragePaid(@RequestBody InviteBrokeragePaidEvent event) {
        inviteService.handleBrokerageOrderPaid(event);
        return ResultUtils.success(true);
    }

    @GetMapping("/system-benefit/{uid}")
    public BaseResponse<InviteSystemBenefitInfo> systemBenefit(@PathVariable("uid") Long uid) {
        InviteSystemBenefitVO vo = inviteService.systemBenefit(uid);
        InviteSystemBenefitInfo info = new InviteSystemBenefitInfo();
        info.setEnabled(vo.getEnabled());
        info.setSystemInvited(vo.getSystemInvited());
        info.setSystemInviteCode(vo.getSystemInviteCode());
        info.setTutorInfoFeeDiscountRate(vo.getTutorInfoFeeDiscountRate());
        info.setStudentRewardRate(vo.getStudentRewardRate());
        return ResultUtils.success(info);
    }
}
