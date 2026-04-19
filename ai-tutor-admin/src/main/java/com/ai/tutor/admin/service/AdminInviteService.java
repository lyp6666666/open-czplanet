package com.ai.tutor.admin.service;

import com.ai.tutor.admin.model.dto.AdminInviteSystemConfigRequest;
import com.ai.tutor.admin.model.vo.AdminInviteRelationVO;
import com.ai.tutor.admin.model.vo.AdminInviteRewardVO;
import com.ai.tutor.admin.model.vo.AdminInviteSettlementVO;
import com.ai.tutor.admin.model.vo.AdminInviteSystemConfigVO;
import com.ai.tutor.admin.model.vo.PageResult;

public interface AdminInviteService {

    PageResult<AdminInviteRelationVO> listRelations(int page, int size, Long inviterUid, Long inviteeUid, String status);

    PageResult<AdminInviteRewardVO> listRewards(int page, int size, Long inviterUid, Long inviteeUid, String status, String scene, String settlementMonth);

    PageResult<AdminInviteSettlementVO> listSettlements(int page, int size, Long userId, String status, String settlementMonth);

    void markSettlementPaid(Long settlementId);

    void markSettlementFailed(Long settlementId, String reason);

    AdminInviteSystemConfigVO systemConfig();

    AdminInviteSystemConfigVO saveSystemConfig(AdminInviteSystemConfigRequest request);
}
