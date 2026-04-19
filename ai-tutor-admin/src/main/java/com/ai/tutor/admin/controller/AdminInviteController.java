package com.ai.tutor.admin.controller;

import com.ai.tutor.admin.model.dto.AdminInviteSystemConfigRequest;
import com.ai.tutor.admin.model.dto.AdminInviteSettlementStatusRequest;
import com.ai.tutor.admin.model.vo.AdminInviteRelationVO;
import com.ai.tutor.admin.model.vo.AdminInviteRewardVO;
import com.ai.tutor.admin.model.vo.AdminInviteSettlementVO;
import com.ai.tutor.admin.model.vo.AdminInviteSystemConfigVO;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.service.AdminInviteService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端邀请返利运营接口。
 */
@RestController
@RequestMapping("/api/admin/invite")
@Tag(name = "Admin Invite", description = "邀请有礼运营与财务结算")
public class AdminInviteController {

    @Resource
    private AdminInviteService adminInviteService;

    @GetMapping("/relations")
    @Operation(summary = "邀请关系列表")
    public BaseResponse<PageResult<AdminInviteRelationVO>> relations(@RequestParam(value = "page", defaultValue = "1") int page,
                                                                     @RequestParam(value = "size", defaultValue = "10") int size,
                                                                     @RequestParam(value = "inviterUid", required = false) Long inviterUid,
                                                                     @RequestParam(value = "inviteeUid", required = false) Long inviteeUid,
                                                                     @RequestParam(value = "status", required = false) String status) {
        return ResultUtils.success(adminInviteService.listRelations(page, size, inviterUid, inviteeUid, status));
    }

    @GetMapping("/rewards")
    @Operation(summary = "邀请返利明细列表")
    public BaseResponse<PageResult<AdminInviteRewardVO>> rewards(@RequestParam(value = "page", defaultValue = "1") int page,
                                                                 @RequestParam(value = "size", defaultValue = "10") int size,
                                                                 @RequestParam(value = "inviterUid", required = false) Long inviterUid,
                                                                 @RequestParam(value = "inviteeUid", required = false) Long inviteeUid,
                                                                 @RequestParam(value = "status", required = false) String status,
                                                                 @RequestParam(value = "scene", required = false) String scene,
                                                                 @RequestParam(value = "settlementMonth", required = false) String settlementMonth) {
        return ResultUtils.success(adminInviteService.listRewards(page, size, inviterUid, inviteeUid, status, scene, settlementMonth));
    }

    @GetMapping("/settlements")
    @Operation(summary = "邀请返利结算单列表")
    public BaseResponse<PageResult<AdminInviteSettlementVO>> settlements(@RequestParam(value = "page", defaultValue = "1") int page,
                                                                        @RequestParam(value = "size", defaultValue = "10") int size,
                                                                        @RequestParam(value = "userId", required = false) Long userId,
                                                                        @RequestParam(value = "status", required = false) String status,
                                                                        @RequestParam(value = "settlementMonth", required = false) String settlementMonth) {
        return ResultUtils.success(adminInviteService.listSettlements(page, size, userId, status, settlementMonth));
    }

    @PostMapping("/settlements/{id}/paid")
    @Operation(summary = "标记结算单已打款")
    public BaseResponse<Boolean> markSettlementPaid(@PathVariable("id") Long id) {
        adminInviteService.markSettlementPaid(id);
        return ResultUtils.success(true);
    }

    @PostMapping("/settlements/{id}/failed")
    @Operation(summary = "标记结算单打款失败")
    public BaseResponse<Boolean> markSettlementFailed(@PathVariable("id") Long id,
                                                      @RequestBody(required = false) AdminInviteSettlementStatusRequest request) {
        adminInviteService.markSettlementFailed(id, request == null ? null : request.getReason());
        return ResultUtils.success(true);
    }

    @GetMapping("/system-config")
    @Operation(summary = "查询系统邀请码配置")
    public BaseResponse<AdminInviteSystemConfigVO> systemConfig() {
        return ResultUtils.success(adminInviteService.systemConfig());
    }

    @PostMapping("/system-config")
    @Operation(summary = "保存系统邀请码配置")
    public BaseResponse<AdminInviteSystemConfigVO> saveSystemConfig(@RequestBody AdminInviteSystemConfigRequest request) {
        return ResultUtils.success(adminInviteService.saveSystemConfig(request));
    }
}
