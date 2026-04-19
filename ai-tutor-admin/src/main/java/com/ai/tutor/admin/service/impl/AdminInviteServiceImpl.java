package com.ai.tutor.admin.service.impl;

import com.ai.tutor.admin.model.dto.AdminInviteSystemConfigRequest;
import com.ai.tutor.admin.mapper.AdminInviteMapper;
import com.ai.tutor.admin.model.vo.AdminInviteRelationVO;
import com.ai.tutor.admin.model.vo.AdminInviteRewardVO;
import com.ai.tutor.admin.model.vo.AdminInviteSettlementVO;
import com.ai.tutor.admin.model.vo.AdminInviteSystemConfigVO;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.service.AdminInviteService;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端邀请返利运营服务。
 */
@Service
public class AdminInviteServiceImpl implements AdminInviteService {

    @Resource
    private AdminInviteMapper adminInviteMapper;
    @Resource
    private TransactionTemplate transactionTemplate;

    private static final String DEFAULT_SYSTEM_INVITE_CODE = "CHUANGZHI";
    private static final String DEFAULT_SYSTEM_INVITE_LINK = "http://localhost:5173/auth/student?inviteCode=CHUANGZHI";
    private static final String DEFAULT_SYSTEM_PROMO_TITLE = "创智推广专属福利";
    private static final String DEFAULT_SYSTEM_PROMO_DESC = "使用创智推广码注册后，教师信息费享受推广期减半，学生可按教师实付信息费获得返现。";

    @Override
    public PageResult<AdminInviteRelationVO> listRelations(int page, int size, Long inviterUid, Long inviteeUid, String status) {
        PageArgs args = pageArgs(page, size);
        String normalizedStatus = trimToNull(status);
        List<AdminInviteRelationVO> records = adminInviteMapper.listRelations(args.offset, args.size, inviterUid, inviteeUid, normalizedStatus);
        long total = adminInviteMapper.countRelations(inviterUid, inviteeUid, normalizedStatus);
        return pageResult(records, total, args);
    }

    @Override
    public PageResult<AdminInviteRewardVO> listRewards(int page, int size, Long inviterUid, Long inviteeUid, String status, String scene, String settlementMonth) {
        PageArgs args = pageArgs(page, size);
        String normalizedStatus = trimToNull(status);
        String normalizedScene = trimToNull(scene);
        String normalizedMonth = trimToNull(settlementMonth);
        List<AdminInviteRewardVO> records = adminInviteMapper.listRewards(args.offset, args.size, inviterUid, inviteeUid, normalizedStatus, normalizedScene, normalizedMonth);
        long total = adminInviteMapper.countRewards(inviterUid, inviteeUid, normalizedStatus, normalizedScene, normalizedMonth);
        return pageResult(records, total, args);
    }

    @Override
    public PageResult<AdminInviteSettlementVO> listSettlements(int page, int size, Long userId, String status, String settlementMonth) {
        PageArgs args = pageArgs(page, size);
        String normalizedStatus = trimToNull(status);
        String normalizedMonth = trimToNull(settlementMonth);
        List<AdminInviteSettlementVO> records = adminInviteMapper.listSettlements(args.offset, args.size, userId, normalizedStatus, normalizedMonth);
        long total = adminInviteMapper.countSettlements(userId, normalizedStatus, normalizedMonth);
        return pageResult(records, total, args);
    }

    @Override
    public void markSettlementPaid(Long settlementId) {
        if (settlementId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Boolean ok = transactionTemplate.execute(status -> {
            AdminInviteSettlementVO settlement = adminInviteMapper.selectSettlementById(settlementId);
            if (settlement == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
            }
            int updated = adminInviteMapper.markSettlementPaid(settlementId, LocalDateTime.now());
            if (updated <= 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前结算单状态不可标记已打款");
            }
            /*
             * 财务确认已打款后，同步推进用户端返利明细状态，确保用户页面“已结算”口径和后台结算单一致。
             */
            adminInviteMapper.markRewardsPaid(settlement.getUserId(), settlement.getSettlementMonth());
            return true;
        });
        if (!Boolean.TRUE.equals(ok)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }

    @Override
    public void markSettlementFailed(Long settlementId, String reason) {
        if (settlementId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String failReason = StringUtils.hasText(reason) ? reason.trim() : "财务打款失败";
        int updated = adminInviteMapper.markSettlementFailed(settlementId, failReason);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前结算单状态不可标记失败");
        }
    }

    @Override
    public AdminInviteSystemConfigVO systemConfig() {
        AdminInviteSystemConfigVO config = adminInviteMapper.selectSystemConfig();
        if (config == null) {
            config = defaultSystemConfig();
            adminInviteMapper.insertSystemConfig(config);
        }
        return normalizeSystemConfig(config);
    }

    @Override
    public AdminInviteSystemConfigVO saveSystemConfig(AdminInviteSystemConfigRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        AdminInviteSystemConfigVO config = AdminInviteSystemConfigVO.builder()
                .enabled(Boolean.TRUE.equals(request.getEnabled()))
                .systemInviteCode(normalizeCode(request.getSystemInviteCode()))
                .systemInviteLink(trimToNull(request.getSystemInviteLink()))
                .tutorInfoFeeDiscountRate(request.getTutorInfoFeeDiscountRate())
                .studentRewardRate(request.getStudentRewardRate())
                .promoTitle(trimToNull(request.getPromoTitle()))
                .promoDesc(trimToNull(request.getPromoDesc()))
                .build();
        config = normalizeSystemConfig(config);
        if (adminInviteMapper.selectSystemConfig() == null) {
            adminInviteMapper.insertSystemConfig(config);
        } else {
            int updated = adminInviteMapper.updateSystemConfig(config);
            if (updated <= 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "系统邀请码配置保存失败");
            }
        }
        return systemConfig();
    }

    private static PageArgs pageArgs(int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 200);
        return new PageArgs(p, s, (long) (p - 1) * s);
    }

    private static <T> PageResult<T> pageResult(List<T> records, long total, PageArgs args) {
        return PageResult.<T>builder()
                .records(records)
                .total(total)
                .size(args.size)
                .current(args.page)
                .build();
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static AdminInviteSystemConfigVO defaultSystemConfig() {
        return AdminInviteSystemConfigVO.builder()
                .enabled(true)
                .systemInviteCode(DEFAULT_SYSTEM_INVITE_CODE)
                .systemInviteLink(DEFAULT_SYSTEM_INVITE_LINK)
                .tutorInfoFeeDiscountRate(0.5D)
                .studentRewardRate(0.13D)
                .promoTitle(DEFAULT_SYSTEM_PROMO_TITLE)
                .promoDesc(DEFAULT_SYSTEM_PROMO_DESC)
                .build();
    }

    private static AdminInviteSystemConfigVO normalizeSystemConfig(AdminInviteSystemConfigVO config) {
        String code = normalizeCode(config.getSystemInviteCode());
        if (!StringUtils.hasText(code)) {
            code = DEFAULT_SYSTEM_INVITE_CODE;
        }
        double discountRate = config.getTutorInfoFeeDiscountRate() == null ? 0.5D : config.getTutorInfoFeeDiscountRate();
        double studentRewardRate = config.getStudentRewardRate() == null ? 0.13D : config.getStudentRewardRate();
        if (discountRate <= 0D || discountRate > 1D || studentRewardRate < 0D || studentRewardRate > 1D) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "系统邀请码比例配置不合法");
        }
        config.setSystemInviteCode(code);
        if (!StringUtils.hasText(config.getSystemInviteLink())) {
            config.setSystemInviteLink("http://localhost:5173/auth/student?inviteCode=" + code);
        }
        config.setTutorInfoFeeDiscountRate(discountRate);
        config.setStudentRewardRate(studentRewardRate);
        if (!StringUtils.hasText(config.getPromoTitle())) {
            config.setPromoTitle(DEFAULT_SYSTEM_PROMO_TITLE);
        }
        if (!StringUtils.hasText(config.getPromoDesc())) {
            config.setPromoDesc(DEFAULT_SYSTEM_PROMO_DESC);
        }
        config.setEnabled(Boolean.TRUE.equals(config.getEnabled()));
        return config;
    }

    private static String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toUpperCase();
    }

    private static final class PageArgs {
        private final int page;
        private final int size;
        private final long offset;

        private PageArgs(int page, int size, long offset) {
            this.page = page;
            this.size = size;
            this.offset = offset;
        }
    }
}
