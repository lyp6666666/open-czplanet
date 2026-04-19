package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.dto.invite.SaveInviteReceiverAccountRequest;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.model.vo.invite.InviteOverviewVO;
import com.ai.tutor.appointment.model.vo.invite.InviteReceiverAccountVO;
import com.ai.tutor.appointment.model.vo.invite.InviteRecordVO;
import com.ai.tutor.appointment.model.vo.invite.InviteRewardRecordVO;
import com.ai.tutor.appointment.model.vo.invite.InviteRulesVO;
import com.ai.tutor.appointment.model.vo.invite.InviteSettlementVO;
import com.ai.tutor.appointment.model.vo.invite.InviteSystemBenefitVO;
import com.ai.tutor.appointment.model.vo.invite.InviteSystemConfigVO;
import com.ai.tutor.common.event.InviteBrokeragePaidEvent;

import java.time.LocalDate;

/**
 * 邀请有礼服务。
 */
public interface InviteService {

    void ensureInviteCode(Long userId);

    void bindInviteCodeIfNeeded(Long inviteeUid, String inviteCode);

    InviteOverviewVO overview(Long userId);

    CursorPageResponse<InviteRecordVO> records(Long userId, CursorPageRequest request, String status);

    CursorPageResponse<InviteRewardRecordVO> rewards(Long userId, CursorPageRequest request, String status, String scene);

    CursorPageResponse<InviteSettlementVO> settlements(Long userId, CursorPageRequest request);

    InviteReceiverAccountVO getReceiverAccount(Long userId);

    InviteReceiverAccountVO saveReceiverAccount(Long userId, SaveInviteReceiverAccountRequest request);

    InviteRulesVO rules();

    InviteSystemConfigVO systemConfig();

    InviteSystemConfigVO saveSystemConfig(InviteSystemConfigVO request);

    InviteSystemBenefitVO systemBenefit(Long userId);

    /**
     * 处理信息费支付成功事件，并按邀请关系幂等生成教师/学生返利记录。
     */
    void handleBrokerageOrderPaid(InviteBrokeragePaidEvent event);

    /**
     * 生成月度结算单。
     *
     * @param today 当前业务日期，便于定时任务与单元测试复用同一口径
     * @return 本次生成的结算单数量
     */
    int generateMonthlySettlements(LocalDate today);
}
