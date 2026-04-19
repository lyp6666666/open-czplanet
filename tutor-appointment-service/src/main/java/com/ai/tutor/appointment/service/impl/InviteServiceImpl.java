package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.config.InviteProperties;
import com.ai.tutor.appointment.mapper.InviteCodeMapper;
import com.ai.tutor.appointment.mapper.InviteReceiverAccountMapper;
import com.ai.tutor.appointment.mapper.InviteRelationMapper;
import com.ai.tutor.appointment.mapper.InviteRewardRecordMapper;
import com.ai.tutor.appointment.mapper.InviteSettlementOrderMapper;
import com.ai.tutor.appointment.mapper.InviteSystemConfigMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.dto.invite.SaveInviteReceiverAccountRequest;
import com.ai.tutor.appointment.model.entity.InviteCode;
import com.ai.tutor.appointment.model.entity.InviteReceiverAccount;
import com.ai.tutor.appointment.model.entity.InviteRelation;
import com.ai.tutor.appointment.model.entity.InviteRewardRecord;
import com.ai.tutor.appointment.model.entity.InviteSettlementOrder;
import com.ai.tutor.appointment.model.entity.InviteSystemConfig;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.model.vo.invite.InviteOverviewVO;
import com.ai.tutor.appointment.model.vo.invite.InviteReceiverAccountVO;
import com.ai.tutor.appointment.model.vo.invite.InviteRecordVO;
import com.ai.tutor.appointment.model.vo.invite.InviteRewardRecordVO;
import com.ai.tutor.appointment.model.vo.invite.InviteRulesVO;
import com.ai.tutor.appointment.model.vo.invite.InviteSettlementVO;
import com.ai.tutor.appointment.model.vo.invite.InviteSystemBenefitVO;
import com.ai.tutor.appointment.model.vo.invite.InviteSystemConfigVO;
import com.ai.tutor.appointment.service.InviteService;
import com.ai.tutor.common.event.InviteBrokeragePaidEvent;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 邀请有礼服务实现。
 *
 * <p>当前实现优先完成三类核心能力：</p>
 * <ul>
 *     <li>用户注册后生成唯一邀请码</li>
 *     <li>注册阶段绑定邀请码，建立邀请关系</li>
 *     <li>邀请页所需的概览、记录、收款信息与规则查询</li>
 * </ul>
 *
 * <p>返利记录与结算记录先接真实表，后续可继续由支付/合作链路异步写入。</p>
 */
@Service
@Slf4j
public class InviteServiceImpl implements InviteService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String SETTLEMENT_STATUS_CREATED = "CREATED";
    private static final String BIND_SOURCE_REGISTER = "REGISTER";
    private static final String BIND_SOURCE_SYSTEM_PROMOTION = "SYSTEM_PROMOTION";
    private static final String REWARD_SCENE_INVITED_TUTOR_DEAL = "INVITED_TUTOR_DEAL";
    private static final String REWARD_SCENE_INVITED_STUDENT_PAID = "INVITED_STUDENT_PAID";
    private static final String REWARD_SCENE_SYSTEM_STUDENT_CASHBACK = "SYSTEM_STUDENT_CASHBACK";
    private static final String BIZ_ORDER_TYPE_BROKERAGE_ORDER = "BROKERAGE_ORDER";
    private static final Long SYSTEM_INVITER_UID = 0L;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    @Resource
    private InviteProperties inviteProperties;
    @Resource
    private InviteCodeMapper inviteCodeMapper;
    @Resource
    private InviteRelationMapper inviteRelationMapper;
    @Resource
    private InviteReceiverAccountMapper inviteReceiverAccountMapper;
    @Resource
    private InviteRewardRecordMapper inviteRewardRecordMapper;
    @Resource
    private InviteSettlementOrderMapper inviteSettlementOrderMapper;
    @Resource
    private InviteSystemConfigMapper inviteSystemConfigMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public void ensureInviteCode(Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        InviteCode existing = inviteCodeMapper.selectByUserId(userId);
        if (existing != null && !trim(existing.getInviteCode()).isEmpty()) {
            return;
        }
        transactionTemplate.execute(status -> {
            InviteCode latest = inviteCodeMapper.selectByUserId(userId);
            if (latest != null && !trim(latest.getInviteCode()).isEmpty()) {
                return true;
            }
            LocalDateTime now = LocalDateTime.now();
            for (int i = 0; i < 10; i++) {
                String generatedCode = generateCode(6);
                try {
                    if (latest == null) {
                        inviteCodeMapper.insert(InviteCode.builder()
                                .userId(userId)
                                .inviteCode(generatedCode)
                                .status(STATUS_ACTIVE)
                                .createTime(now)
                                .updateTime(now)
                                .build());
                    } else {
                        /*
                         * 企业规范：历史脏数据可能已经存在 invite_code 记录，但字段值为空。
                         * 此处直接修复该行数据，避免首屏长期展示“邀请码生成中”。
                         */
                        inviteCodeMapper.updateInviteCodeByUserId(userId, generatedCode, STATUS_ACTIVE, now);
                    }
                    return true;
                } catch (DuplicateKeyException ignored) {
                }
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邀请码生成失败，请稍后重试");
        });
    }

    @Override
    public void bindInviteCodeIfNeeded(Long inviteeUid, String inviteCode) {
        ThrowUtils.throwIf(inviteeUid == null, ErrorCode.PARAMS_ERROR);
        String normalizedInviteCode = normalizeInviteCode(inviteCode);
        if (normalizedInviteCode.isEmpty()) {
            return;
        }
        InviteRelation existingRelation = inviteRelationMapper.selectByInviteeUid(inviteeUid);
        if (existingRelation != null) {
            return;
        }
        InviteSystemConfig systemConfig = currentSystemConfig();
        if (isEnabledSystemCode(systemConfig, normalizedInviteCode)) {
            bindSystemInviteCode(inviteeUid, systemConfig);
            return;
        }
        InviteCode inviterCode = inviteCodeMapper.selectByCode(normalizedInviteCode);
        ThrowUtils.throwIf(inviterCode == null, ErrorCode.PARAMS_ERROR, "邀请码无效，请核对后重试");
        ThrowUtils.throwIf(inviterCode.getUserId() == null || inviterCode.getUserId().equals(inviteeUid), ErrorCode.PARAMS_ERROR, "不能填写自己的邀请码");

        User inviter = userMapper.selectById(inviterCode.getUserId());
        ThrowUtils.throwIf(inviter == null, ErrorCode.NOT_FOUND_ERROR, "邀请码所属用户不存在");
        ThrowUtils.throwIf(inviter.getUserType() != null && inviter.getUserType() == 3, ErrorCode.PARAMS_ERROR, "该邀请码暂不可用");

        LocalDateTime now = LocalDateTime.now();
        transactionTemplate.execute(status -> {
            InviteRelation latest = inviteRelationMapper.selectByInviteeUid(inviteeUid);
            if (latest != null) {
                return true;
            }
            try {
                inviteRelationMapper.insert(InviteRelation.builder()
                        .inviterUid(inviterCode.getUserId())
                        .inviteeUid(inviteeUid)
                        .inviteCode(normalizedInviteCode)
                        .bindSource(BIND_SOURCE_REGISTER)
                        .status(STATUS_ACTIVE)
                        .riskFlag(0)
                        .bindTime(now)
                        .createTime(now)
                        .updateTime(now)
                        .build());
                return true;
            } catch (DuplicateKeyException e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前账号已绑定邀请码，不能重复填写");
            }
        });
    }

    @Override
    public InviteOverviewVO overview(Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        ensureInviteCode(userId);
        InviteCode inviteCode = inviteCodeMapper.selectByUserId(userId);
        if (inviteCode == null || trim(inviteCode.getInviteCode()).isEmpty()) {
            /*
             * 企业规范：邀请页首屏展示必须尽量保证“老用户也有邀请码”。
             * 这里增加一次兜底补偿，覆盖历史账号未生成邀请码、远程库手工补数据不完整等场景。
             */
            log.warn("invite_code_missing_retry userId={}", userId);
            ensureInviteCode(userId);
            inviteCode = inviteCodeMapper.selectByUserId(userId);
        }
        InviteReceiverAccount receiverAccount = inviteReceiverAccountMapper.selectByUserId(userId);
        Integer totalInviteCount = safeInteger(inviteRelationMapper.countActiveByInviterUid(userId));
        Integer effectiveInviteCount = safeInteger(inviteRewardRecordMapper.countEffectiveByInviterUid(userId));
        Long totalRewardAmountFen = safeLong(inviteRewardRecordMapper.sumRewardAmountByInviterUid(userId));
        Long pendingSettlementAmountFen = safeLong(inviteRewardRecordMapper.sumPendingAmountByInviterUid(userId));
        return InviteOverviewVO.builder()
                .myInviteCode(inviteCode == null ? "" : inviteCode.getInviteCode())
                .totalInviteCount(totalInviteCount)
                .effectiveInviteCount(effectiveInviteCount)
                .totalRewardAmountFen(totalRewardAmountFen)
                .pendingSettlementAmountFen(pendingSettlementAmountFen)
                .estimatedCurrentMonthAmountFen(pendingSettlementAmountFen)
                .teacherRewardRate(inviteProperties.getTeacherRewardRate())
                .studentRewardRate(inviteProperties.getStudentRewardRate())
                .settlementDay(inviteProperties.getSettlementDay())
                .receiverConfigured(isReceiverConfigured(receiverAccount))
                .systemInviteConfig(toSystemConfigVO(currentSystemConfig()))
                .build();
    }

    @Override
    public CursorPageResponse<InviteRecordVO> records(Long userId, CursorPageRequest request, String status) {
        ThrowUtils.throwIf(userId == null || request == null, ErrorCode.PARAMS_ERROR);
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();
        List<InviteRelation> rows = inviteRelationMapper.pageByInviterUid(userId, request.getCursor(), pageSize, normalizeFilter(status));
        if (rows == null || rows.isEmpty()) {
            return new CursorPageResponse<>(null, true, List.of());
        }
        List<Long> inviteeIds = rows.stream().map(InviteRelation::getInviteeUid).toList();
        Map<Long, User> userMap = mapUsers(inviteeIds);
        List<InviteRecordVO> list = new ArrayList<>(rows.size());
        for (InviteRelation row : rows) {
            User invitee = userMap.get(row.getInviteeUid());
            String phoneMasked = maskPhone(invitee == null ? null : invitee.getPhone());
            // 企业级口径：邀请记录的“已产生返利”必须按邀请人与被邀请人精确匹配，避免分页取样导致误判。
            boolean hasReward = safeInteger(inviteRewardRecordMapper.countEffectiveByInviterUidAndInviteeUid(userId, row.getInviteeUid())) > 0;
            list.add(InviteRecordVO.builder()
                    .inviteeUid(row.getInviteeUid())
                    .inviteeDisplayName(resolveUserDisplayName(invitee))
                    .inviteePhoneMasked(phoneMasked)
                    .inviteeUserType(invitee == null ? null : invitee.getUserType())
                    .registeredAt(row.getBindTime() == null ? null : row.getBindTime().format(DATE_TIME_FORMATTER))
                    .status(row.getStatus())
                    .hasReward(hasReward)
                    .build());
        }
        Long nextCursor = rows.size() < pageSize ? null : rows.get(rows.size() - 1).getId();
        return new CursorPageResponse<>(nextCursor, rows.size() < pageSize, list);
    }

    @Override
    public CursorPageResponse<InviteRewardRecordVO> rewards(Long userId, CursorPageRequest request, String status, String scene) {
        ThrowUtils.throwIf(userId == null || request == null, ErrorCode.PARAMS_ERROR);
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();
        List<InviteRewardRecord> rows = inviteRewardRecordMapper.pageByInviterUid(
                userId,
                request.getCursor(),
                pageSize,
                normalizeFilter(status),
                normalizeFilter(scene)
        );
        if (rows == null || rows.isEmpty()) {
            return new CursorPageResponse<>(null, true, List.of());
        }
        Map<Long, User> inviteeMap = mapUsers(rows.stream().map(InviteRewardRecord::getInviteeUid).distinct().toList());
        List<InviteRewardRecordVO> list = new ArrayList<>(rows.size());
        for (InviteRewardRecord row : rows) {
            User invitee = inviteeMap.get(row.getInviteeUid());
            list.add(InviteRewardRecordVO.builder()
                    .id(row.getId())
                    .inviteeUid(row.getInviteeUid())
                    .inviteeDisplayName(resolveUserDisplayName(invitee))
                    .rewardScene(row.getRewardScene())
                    .bizOrderType(row.getBizOrderType())
                    .bizOrderId(row.getBizOrderId())
                    .baseAmountFen(row.getBaseAmountFen())
                    .rewardRate(row.getRewardRate())
                    .rewardAmountFen(row.getRewardAmountFen())
                    .status(row.getStatus())
                    .createdAt(row.getCreateTime() == null ? null : row.getCreateTime().format(DATE_TIME_FORMATTER))
                    .build());
        }
        Long nextCursor = rows.size() < pageSize ? null : rows.get(rows.size() - 1).getId();
        return new CursorPageResponse<>(nextCursor, rows.size() < pageSize, list);
    }

    @Override
    public CursorPageResponse<InviteSettlementVO> settlements(Long userId, CursorPageRequest request) {
        ThrowUtils.throwIf(userId == null || request == null, ErrorCode.PARAMS_ERROR);
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();
        List<InviteSettlementOrder> rows = inviteSettlementOrderMapper.pageByUserId(userId, request.getCursor(), pageSize);
        if (rows == null || rows.isEmpty()) {
            return new CursorPageResponse<>(null, true, List.of());
        }
        List<InviteSettlementVO> list = rows.stream().map(it -> InviteSettlementVO.builder()
                .id(it.getId())
                .settlementMonth(it.getSettlementMonth())
                .totalAmountFen(it.getTotalAmountFen())
                .status(it.getStatus())
                .payTime(it.getPayTime() == null ? null : it.getPayTime().format(DATE_TIME_FORMATTER))
                .failReason(it.getFailReason())
                .build()).toList();
        Long nextCursor = rows.size() < pageSize ? null : rows.get(rows.size() - 1).getId();
        return new CursorPageResponse<>(nextCursor, rows.size() < pageSize, list);
    }

    @Override
    public InviteReceiverAccountVO getReceiverAccount(Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        return toReceiverVO(inviteReceiverAccountMapper.selectByUserId(userId));
    }

    @Override
    public InviteReceiverAccountVO saveReceiverAccount(Long userId, SaveInviteReceiverAccountRequest request) {
        ThrowUtils.throwIf(userId == null || request == null, ErrorCode.PARAMS_ERROR);
        String receiverName = trim(request.getReceiverName());
        String wechatNo = trim(request.getWechatNo());
        String phone = trim(request.getPhone());
        ThrowUtils.throwIf(receiverName.isEmpty() || wechatNo.isEmpty() || phone.isEmpty(), ErrorCode.PARAMS_ERROR, "收款信息格式错误");
        ThrowUtils.throwIf(phone.length() != 11, ErrorCode.PARAMS_ERROR, "收款信息格式错误");

        InviteReceiverAccount existing = inviteReceiverAccountMapper.selectByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            inviteReceiverAccountMapper.insert(InviteReceiverAccount.builder()
                    .userId(userId)
                    .receiverName(receiverName)
                    .wechatNo(wechatNo)
                    .phone(phone)
                    .remark(trimToNull(request.getRemark()))
                    .status(STATUS_ACTIVE)
                    .createTime(now)
                    .updateTime(now)
                    .build());
        } else {
            existing.setReceiverName(receiverName);
            existing.setWechatNo(wechatNo);
            existing.setPhone(phone);
            existing.setRemark(trimToNull(request.getRemark()));
            existing.setStatus(STATUS_ACTIVE);
            inviteReceiverAccountMapper.updateByUserId(existing);
        }
        return toReceiverVO(inviteReceiverAccountMapper.selectByUserId(userId));
    }

    @Override
    public InviteRulesVO rules() {
        InviteSystemConfig systemConfig = currentSystemConfig();
        List<String> ruleTextList = List.of(
                String.format(Locale.ROOT, "邀请教师成单返利 %.0f%%", inviteProperties.getTeacherRewardRate() * 100),
                String.format(Locale.ROOT, "邀请学生有效支付返利 %.0f%%", inviteProperties.getStudentRewardRate() * 100),
                String.format(Locale.ROOT, "每月 %d 号统一结算", inviteProperties.getSettlementDay())
        );
        return InviteRulesVO.builder()
                .teacherRewardRate(inviteProperties.getTeacherRewardRate())
                .studentRewardRate(inviteProperties.getStudentRewardRate())
                .settlementDay(inviteProperties.getSettlementDay())
                .minSettlementAmountFen(inviteProperties.getMinSettlementAmountFen())
                .enabled(inviteProperties.isEnabled())
                .receiverHint(inviteProperties.getReceiverHint())
                .systemInviteConfig(toSystemConfigVO(systemConfig))
                .ruleTextList(ruleTextList)
                .build();
    }

    @Override
    public InviteSystemConfigVO systemConfig() {
        return toSystemConfigVO(currentSystemConfig());
    }

    @Override
    public InviteSystemConfigVO saveSystemConfig(InviteSystemConfigVO request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        String code = normalizeInviteCode(request.getSystemInviteCode());
        ThrowUtils.throwIf(code.isEmpty() || code.length() > 16, ErrorCode.PARAMS_ERROR, "系统邀请码格式不正确");
        double discountRate = normalizeDiscountRate(request.getTutorInfoFeeDiscountRate());
        double studentRewardRate = normalizeRewardRate(request.getStudentRewardRate());
        LocalDateTime now = LocalDateTime.now();
        InviteSystemConfig config = InviteSystemConfig.builder()
                .id(1L)
                .enabled(Boolean.TRUE.equals(request.getEnabled()) ? 1 : 0)
                .systemInviteCode(code)
                .systemInviteLink(normalizeSystemInviteLink(request.getSystemInviteLink(), code))
                .tutorInfoFeeDiscountRate(discountRate)
                .studentRewardRate(studentRewardRate)
                .promoTitle(trimOrDefault(request.getPromoTitle(), inviteProperties.getSystemPromoTitle()))
                .promoDesc(trimOrDefault(request.getPromoDesc(), inviteProperties.getSystemPromoDesc()))
                .createTime(now)
                .updateTime(now)
                .build();
        InviteSystemConfig existing = inviteSystemConfigMapper.selectSingleton();
        if (existing == null) {
            inviteSystemConfigMapper.insertSingleton(config);
        } else {
            inviteSystemConfigMapper.updateSingleton(config);
        }
        return toSystemConfigVO(currentSystemConfig());
    }

    @Override
    public InviteSystemBenefitVO systemBenefit(Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        InviteSystemConfig config = currentSystemConfig();
        InviteRelation relation = inviteRelationMapper.selectByInviteeUid(userId);
        boolean matched = isActiveSystemRelation(relation);
        return InviteSystemBenefitVO.builder()
                .enabled(isSystemConfigEnabled(config))
                .systemInvited(matched && isSystemConfigEnabled(config))
                .systemInviteCode(config.getSystemInviteCode())
                .tutorInfoFeeDiscountRate(normalizeDiscountRate(config.getTutorInfoFeeDiscountRate()))
                .studentRewardRate(normalizeRewardRate(config.getStudentRewardRate()))
                .build();
    }

    @Override
    public void handleBrokerageOrderPaid(InviteBrokeragePaidEvent event) {
        if (!inviteProperties.isEnabled()) {
            return;
        }
        ThrowUtils.throwIf(event == null || event.getBrokerageOrderId() == null, ErrorCode.PARAMS_ERROR);
        Long amountFen = event.getAmountFen();
        if (amountFen == null || amountFen <= 0L) {
            return;
        }

        /*
         * 企业返利口径：
         * 1. 教师被邀请且完成信息费成单，按教师返利比例记一笔；
         * 2. 学生被邀请且该单信息费完成支付，按学生返利比例记一笔；
         * 3. 唯一键 reward_scene + biz_order_type + biz_order_id 保证 MQ/内部接口重试不会重复入账。
         */
        createRewardIfInviteeMatched(
                event.getTeacherUid(),
                REWARD_SCENE_INVITED_TUTOR_DEAL,
                inviteProperties.getTeacherRewardRate(),
                amountFen,
                event
        );
        createRewardIfInviteeMatched(
                event.getStudentUid(),
                REWARD_SCENE_INVITED_STUDENT_PAID,
                inviteProperties.getStudentRewardRate(),
                amountFen,
                event
        );
        createSystemStudentCashbackIfMatched(event.getStudentUid(), amountFen, event);
    }

    @Override
    public int generateMonthlySettlements(LocalDate today) {
        LocalDate bizDate = today == null ? LocalDate.now() : today;
        if (!inviteProperties.isEnabled() || !inviteProperties.isSettlementJobEnabled()) {
            return 0;
        }
        if (bizDate.getDayOfMonth() != inviteProperties.getSettlementDay()) {
            return 0;
        }
        String settlementMonth = bizDate.minusMonths(1).format(MONTH_FORMATTER);
        LocalDateTime cutoff = bizDate.withDayOfMonth(1).atStartOfDay();
        List<Long> inviterIds = inviteRewardRecordMapper.listSettlementCandidateInviters(cutoff, 5000);
        if (inviterIds == null || inviterIds.isEmpty()) {
            return 0;
        }
        int createdCount = 0;
        for (Long inviterUid : inviterIds) {
            if (createSettlementForInviter(inviterUid, settlementMonth, cutoff)) {
                createdCount++;
            }
        }
        return createdCount;
    }

    private void createRewardIfInviteeMatched(Long inviteeUid,
                                              String rewardScene,
                                              double rewardRate,
                                              Long amountFen,
                                              InviteBrokeragePaidEvent event) {
        if (inviteeUid == null || rewardRate <= 0D) {
            return;
        }
        InviteRelation relation = inviteRelationMapper.selectByInviteeUid(inviteeUid);
        if (relation == null || !STATUS_ACTIVE.equals(relation.getStatus())) {
            return;
        }
        if (isActiveSystemRelation(relation)) {
            return;
        }
        Long inviterUid = relation.getInviterUid();
        if (inviterUid == null || inviterUid.equals(inviteeUid)) {
            return;
        }
        long rewardAmountFen = calculateRewardAmountFen(amountFen, rewardRate);
        if (rewardAmountFen <= 0L) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        InviteRewardRecord record = InviteRewardRecord.builder()
                .inviterUid(inviterUid)
                .inviteeUid(inviteeUid)
                .rewardScene(rewardScene)
                .bizOrderType(BIZ_ORDER_TYPE_BROKERAGE_ORDER)
                .bizOrderId(event.getBrokerageOrderId())
                .paymentOrderId(null)
                .baseAmountFen(amountFen)
                .rewardRate(rewardRate)
                .rewardAmountFen(rewardAmountFen)
                .status(STATUS_PENDING)
                .freezeReason(null)
                .settlementMonth(null)
                .configSnapshotJson(buildRewardSnapshotJson(event, rewardScene, rewardRate))
                .createTime(event.getPaidAt() == null ? now : event.getPaidAt())
                .updateTime(now)
                .build();
        try {
            inviteRewardRecordMapper.insert(record);
        } catch (DuplicateKeyException ignored) {
            // 支付回调、MQ 或内部接口重试均可能重复到达，唯一键命中时视为幂等成功。
        }
    }

    private void createSystemStudentCashbackIfMatched(Long studentUid, Long amountFen, InviteBrokeragePaidEvent event) {
        if (studentUid == null || amountFen == null || amountFen <= 0L) {
            return;
        }
        InviteSystemConfig config = currentSystemConfig();
        if (!isSystemConfigEnabled(config)) {
            return;
        }
        InviteRelation relation = inviteRelationMapper.selectByInviteeUid(studentUid);
        if (!isActiveSystemRelation(relation)) {
            return;
        }
        double rewardRate = normalizeRewardRate(config.getStudentRewardRate());
        long rewardAmountFen = calculateRewardAmountFen(amountFen, rewardRate);
        if (rewardAmountFen <= 0L) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        InviteRewardRecord record = InviteRewardRecord.builder()
                .inviterUid(studentUid)
                .inviteeUid(studentUid)
                .rewardScene(REWARD_SCENE_SYSTEM_STUDENT_CASHBACK)
                .bizOrderType(BIZ_ORDER_TYPE_BROKERAGE_ORDER)
                .bizOrderId(event.getBrokerageOrderId())
                .paymentOrderId(null)
                .baseAmountFen(amountFen)
                .rewardRate(rewardRate)
                .rewardAmountFen(rewardAmountFen)
                .status(STATUS_PENDING)
                .freezeReason(null)
                .settlementMonth(null)
                .configSnapshotJson(buildRewardSnapshotJson(event, REWARD_SCENE_SYSTEM_STUDENT_CASHBACK, rewardRate))
                .createTime(event.getPaidAt() == null ? now : event.getPaidAt())
                .updateTime(now)
                .build();
        try {
            inviteRewardRecordMapper.insert(record);
        } catch (DuplicateKeyException ignored) {
            // 系统邀请码学生返现同样依赖唯一键幂等，避免支付回调重试重复入账。
        }
    }

    private boolean createSettlementForInviter(Long inviterUid, String settlementMonth, LocalDateTime cutoff) {
        if (inviterUid == null) {
            return false;
        }
        InviteSettlementOrder existing = inviteSettlementOrderMapper.selectByUserIdAndMonth(inviterUid, settlementMonth);
        if (existing != null) {
            return false;
        }
        Long totalAmountFen = safeLong(inviteRewardRecordMapper.sumSettleableAmountByInviterUid(inviterUid, cutoff));
        if (totalAmountFen < inviteProperties.getMinSettlementAmountFen()) {
            return false;
        }
        InviteReceiverAccount receiverAccount = inviteReceiverAccountMapper.selectByUserId(inviterUid);
        if (!isReceiverConfigured(receiverAccount)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            InviteSettlementOrder latest = inviteSettlementOrderMapper.selectByUserIdAndMonth(inviterUid, settlementMonth);
            if (latest != null) {
                return false;
            }
            inviteSettlementOrderMapper.insert(InviteSettlementOrder.builder()
                    .userId(inviterUid)
                    .settlementMonth(settlementMonth)
                    .totalAmountFen(totalAmountFen)
                    .paidAmountFen(0L)
                    .status(SETTLEMENT_STATUS_CREATED)
                    .receiverSnapshotJson(buildReceiverSnapshotJson(receiverAccount))
                    .failReason(null)
                    .payTime(null)
                    .createTime(now)
                    .updateTime(now)
                    .build());
            inviteRewardRecordMapper.markSettlementPending(inviterUid, settlementMonth, cutoff);
            return true;
        }));
    }

    private void bindSystemInviteCode(Long inviteeUid, InviteSystemConfig systemConfig) {
        LocalDateTime now = LocalDateTime.now();
        transactionTemplate.execute(status -> {
            InviteRelation latest = inviteRelationMapper.selectByInviteeUid(inviteeUid);
            if (latest != null) {
                return true;
            }
            try {
                inviteRelationMapper.insert(InviteRelation.builder()
                        .inviterUid(SYSTEM_INVITER_UID)
                        .inviteeUid(inviteeUid)
                        .inviteCode(systemConfig.getSystemInviteCode())
                        .bindSource(BIND_SOURCE_SYSTEM_PROMOTION)
                        .status(STATUS_ACTIVE)
                        .riskFlag(0)
                        .remark("创智推广码推广期权益")
                        .bindTime(now)
                        .createTime(now)
                        .updateTime(now)
                        .build());
                return true;
            } catch (DuplicateKeyException e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前账号已绑定邀请码，不能重复填写");
            }
        });
    }

    private InviteSystemConfig currentSystemConfig() {
        InviteSystemConfig dbConfig = inviteSystemConfigMapper == null ? null : inviteSystemConfigMapper.selectSingleton();
        if (dbConfig != null) {
            normalizeSystemConfig(dbConfig);
            return dbConfig;
        }
        InviteSystemConfig fallback = InviteSystemConfig.builder()
                .id(1L)
                .enabled(1)
                .systemInviteCode(normalizeInviteCode(inviteProperties.getSystemInviteCode()))
                .systemInviteLink(inviteProperties.getSystemInviteLink())
                .tutorInfoFeeDiscountRate(inviteProperties.getSystemTutorInfoFeeDiscountRate())
                .studentRewardRate(inviteProperties.getSystemStudentRewardRate())
                .promoTitle(inviteProperties.getSystemPromoTitle())
                .promoDesc(inviteProperties.getSystemPromoDesc())
                .build();
        normalizeSystemConfig(fallback);
        return fallback;
    }

    private void normalizeSystemConfig(InviteSystemConfig config) {
        String code = normalizeInviteCode(config.getSystemInviteCode());
        if (code.isEmpty()) {
            code = normalizeInviteCode(inviteProperties.getSystemInviteCode());
        }
        config.setSystemInviteCode(code);
        config.setSystemInviteLink(normalizeSystemInviteLink(config.getSystemInviteLink(), code));
        config.setTutorInfoFeeDiscountRate(normalizeDiscountRate(config.getTutorInfoFeeDiscountRate()));
        config.setStudentRewardRate(normalizeRewardRate(config.getStudentRewardRate()));
        config.setPromoTitle(trimOrDefault(config.getPromoTitle(), inviteProperties.getSystemPromoTitle()));
        config.setPromoDesc(trimOrDefault(config.getPromoDesc(), inviteProperties.getSystemPromoDesc()));
    }

    private InviteSystemConfigVO toSystemConfigVO(InviteSystemConfig config) {
        InviteSystemConfig normalized = config == null ? currentSystemConfig() : config;
        normalizeSystemConfig(normalized);
        return InviteSystemConfigVO.builder()
                .enabled(isSystemConfigEnabled(normalized))
                .systemInviteCode(normalized.getSystemInviteCode())
                .systemInviteLink(normalized.getSystemInviteLink())
                .tutorInfoFeeDiscountRate(normalized.getTutorInfoFeeDiscountRate())
                .studentRewardRate(normalized.getStudentRewardRate())
                .promoTitle(normalized.getPromoTitle())
                .promoDesc(normalized.getPromoDesc())
                .build();
    }

    private boolean isEnabledSystemCode(InviteSystemConfig config, String normalizedInviteCode) {
        return isSystemConfigEnabled(config)
                && !trim(normalizedInviteCode).isEmpty()
                && normalizeInviteCode(config.getSystemInviteCode()).equals(normalizedInviteCode);
    }

    private boolean isSystemConfigEnabled(InviteSystemConfig config) {
        return config != null && Integer.valueOf(1).equals(config.getEnabled());
    }

    private boolean isActiveSystemRelation(InviteRelation relation) {
        return relation != null
                && STATUS_ACTIVE.equals(relation.getStatus())
                && SYSTEM_INVITER_UID.equals(relation.getInviterUid())
                && BIND_SOURCE_SYSTEM_PROMOTION.equals(relation.getBindSource());
    }

    private double normalizeDiscountRate(Double raw) {
        double v = raw == null ? inviteProperties.getSystemTutorInfoFeeDiscountRate() : raw;
        ThrowUtils.throwIf(v <= 0D || v > 1D, ErrorCode.PARAMS_ERROR, "系统邀请码教师优惠比例配置不合法");
        return v;
    }

    private double normalizeRewardRate(Double raw) {
        double v = raw == null ? inviteProperties.getSystemStudentRewardRate() : raw;
        ThrowUtils.throwIf(v < 0D || v > 1D, ErrorCode.PARAMS_ERROR, "系统邀请码学生返现比例配置不合法");
        return v;
    }

    private String normalizeSystemInviteLink(String raw, String code) {
        String link = trim(raw);
        if (!link.isEmpty()) {
            return link;
        }
        String origin = trim(inviteProperties.getWebOrigin());
        if (origin.isEmpty()) {
            return "/auth/student?inviteCode=" + code;
        }
        return origin.replaceAll("/+$", "") + "/auth/student?inviteCode=" + code;
    }

    private String trimOrDefault(String raw, String fallback) {
        String v = trim(raw);
        return v.isEmpty() ? trim(fallback) : v;
    }

    private long calculateRewardAmountFen(Long baseAmountFen, double rewardRate) {
        // 金额单位为分，返利向下取整，避免平台因为小数分产生超额打款。
        return BigDecimal.valueOf(baseAmountFen)
                .multiply(BigDecimal.valueOf(rewardRate))
                .setScale(0, RoundingMode.DOWN)
                .longValue();
    }

    private String buildRewardSnapshotJson(InviteBrokeragePaidEvent event, String rewardScene, double rewardRate) {
        return String.format(Locale.ROOT,
                "{\"scene\":\"%s\",\"rate\":%.4f,\"proposalId\":%s,\"applicationId\":%s,\"roomId\":%s,\"payerUid\":%s,\"payMethod\":\"%s\",\"source\":\"%s\"}",
                rewardScene,
                rewardRate,
                jsonNumber(event.getProposalId()),
                jsonNumber(event.getApplicationId()),
                jsonNumber(event.getRoomId()),
                jsonNumber(event.getPayerUid()),
                jsonText(event.getPayMethod()),
                jsonText(event.getSource()));
    }

    private String buildReceiverSnapshotJson(InviteReceiverAccount receiverAccount) {
        return String.format(Locale.ROOT,
                "{\"receiverName\":\"%s\",\"wechatNo\":\"%s\",\"phone\":\"%s\"}",
                jsonText(receiverAccount.getReceiverName()),
                jsonText(receiverAccount.getWechatNo()),
                jsonText(receiverAccount.getPhone()));
    }

    private String jsonNumber(Long value) {
        return value == null ? "null" : String.valueOf(value);
    }

    private String jsonText(String value) {
        return nullToEmpty(value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String generateCode(int len) {
        StringBuilder sb = new StringBuilder(len);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < len; i++) {
            sb.append(CODE_CHARS[random.nextInt(CODE_CHARS.length)]);
        }
        return sb.toString();
    }

    private String normalizeInviteCode(String inviteCode) {
        return trim(inviteCode).toUpperCase(Locale.ROOT);
    }

    private String normalizeFilter(String raw) {
        String value = trim(raw);
        return value.isEmpty() ? null : value;
    }

    private String trim(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private String trimToNull(String raw) {
        String value = trim(raw);
        return value.isEmpty() ? null : value;
    }

    private InviteReceiverAccountVO toReceiverVO(InviteReceiverAccount account) {
        return InviteReceiverAccountVO.builder()
                .receiverName(account == null ? "" : nullToEmpty(account.getReceiverName()))
                .wechatNo(account == null ? "" : nullToEmpty(account.getWechatNo()))
                .phone(account == null ? "" : nullToEmpty(account.getPhone()))
                .remark(account == null ? null : account.getRemark())
                .configured(isReceiverConfigured(account))
                .build();
    }

    private boolean isReceiverConfigured(InviteReceiverAccount account) {
        return account != null
                && !trim(account.getReceiverName()).isEmpty()
                && !trim(account.getWechatNo()).isEmpty()
                && !trim(account.getPhone()).isEmpty();
    }

    private String resolveUserDisplayName(User user) {
        if (user == null) {
            return "用户";
        }
        String name = trim(user.getName());
        if (!name.isEmpty()) {
            return name;
        }
        String phoneMasked = maskPhone(user.getPhone());
        if (!phoneMasked.isEmpty()) {
            return phoneMasked;
        }
        return "用户" + user.getId();
    }

    private String maskPhone(String phone) {
        String value = trim(phone);
        if (value.length() < 7) {
            return value;
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private Map<Long, User> mapUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<User> users = userMapper.selectByIds(userIds);
        Map<Long, User> userMap = new HashMap<>();
        if (users != null) {
            for (User user : users) {
                userMap.put(user.getId(), user);
            }
        }
        return userMap;
    }

    private Integer safeInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private Long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}
