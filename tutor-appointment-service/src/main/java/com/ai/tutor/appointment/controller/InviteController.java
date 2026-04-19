package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.dto.invite.SaveInviteReceiverAccountRequest;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.model.vo.invite.InviteOverviewVO;
import com.ai.tutor.appointment.model.vo.invite.InviteReceiverAccountVO;
import com.ai.tutor.appointment.model.vo.invite.InviteRecordVO;
import com.ai.tutor.appointment.model.vo.invite.InviteRewardRecordVO;
import com.ai.tutor.appointment.model.vo.invite.InviteRulesVO;
import com.ai.tutor.appointment.model.vo.invite.InviteSettlementVO;
import com.ai.tutor.appointment.service.InviteService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.utils.ThrowUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 邀请有礼接口。
 */
@RestController
@RequestMapping("/invite")
@Tag(name = "邀请有礼接口", description = "提供邀请码总览、邀请记录、返利记录、结算记录与收款信息维护能力")
public class InviteController {

    @Resource
    private InviteService inviteService;

    @GetMapping("/overview")
    @Operation(summary = "获取邀请总览")
    public BaseResponse<InviteOverviewVO> overview(HttpServletRequest request) {
        return ResultUtils.success(inviteService.overview(currentUid(request)));
    }

    @GetMapping("/records")
    @Operation(summary = "获取邀请记录分页")
    public BaseResponse<CursorPageResponse<InviteRecordVO>> records(@Valid CursorPageRequest request,
                                                                    @RequestParam(value = "status", required = false) String status,
                                                                    HttpServletRequest httpServletRequest) {
        return ResultUtils.success(inviteService.records(currentUid(httpServletRequest), request, status));
    }

    @GetMapping("/rewards")
    @Operation(summary = "获取返利明细分页")
    public BaseResponse<CursorPageResponse<InviteRewardRecordVO>> rewards(@Valid CursorPageRequest request,
                                                                          @RequestParam(value = "status", required = false) String status,
                                                                          @RequestParam(value = "scene", required = false) String scene,
                                                                          HttpServletRequest httpServletRequest) {
        return ResultUtils.success(inviteService.rewards(currentUid(httpServletRequest), request, status, scene));
    }

    @GetMapping("/settlements")
    @Operation(summary = "获取结算记录分页")
    public BaseResponse<CursorPageResponse<InviteSettlementVO>> settlements(@Valid CursorPageRequest request,
                                                                            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(inviteService.settlements(currentUid(httpServletRequest), request));
    }

    @GetMapping("/receiver-account")
    @Operation(summary = "获取收款信息")
    public BaseResponse<InviteReceiverAccountVO> receiverAccount(HttpServletRequest request) {
        return ResultUtils.success(inviteService.getReceiverAccount(currentUid(request)));
    }

    @PostMapping("/receiver-account")
    @Operation(summary = "保存收款信息")
    public BaseResponse<InviteReceiverAccountVO> saveReceiverAccount(@RequestBody SaveInviteReceiverAccountRequest request,
                                                                     HttpServletRequest httpServletRequest) {
        return ResultUtils.success(inviteService.saveReceiverAccount(currentUid(httpServletRequest), request));
    }

    @GetMapping("/rules")
    @Operation(summary = "获取邀请规则")
    public BaseResponse<InviteRulesVO> rules() {
        return ResultUtils.success(inviteService.rules());
    }

    private Long currentUid(HttpServletRequest request) {
        Object uid = request == null ? null : request.getAttribute("uid");
        ThrowUtils.throwIf(uid == null, ErrorCode.NOT_LOGIN_ERROR);
        return Long.parseLong(String.valueOf(uid));
    }
}
