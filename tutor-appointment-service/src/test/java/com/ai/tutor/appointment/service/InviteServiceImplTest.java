package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.config.InviteProperties;
import com.ai.tutor.appointment.mapper.InviteCodeMapper;
import com.ai.tutor.appointment.mapper.InviteReceiverAccountMapper;
import com.ai.tutor.appointment.mapper.InviteRelationMapper;
import com.ai.tutor.appointment.mapper.InviteRewardRecordMapper;
import com.ai.tutor.appointment.mapper.InviteSettlementOrderMapper;
import com.ai.tutor.appointment.mapper.InviteSystemConfigMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.dto.invite.SaveInviteReceiverAccountRequest;
import com.ai.tutor.appointment.model.entity.InviteCode;
import com.ai.tutor.appointment.model.entity.InviteReceiverAccount;
import com.ai.tutor.appointment.model.entity.InviteRelation;
import com.ai.tutor.appointment.model.entity.InviteRewardRecord;
import com.ai.tutor.appointment.model.entity.InviteSettlementOrder;
import com.ai.tutor.appointment.model.entity.InviteSystemConfig;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.model.vo.invite.InviteOverviewVO;
import com.ai.tutor.appointment.model.vo.invite.InviteReceiverAccountVO;
import com.ai.tutor.appointment.service.impl.InviteServiceImpl;
import com.ai.tutor.common.event.InviteBrokeragePaidEvent;
import com.ai.tutor.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InviteServiceImplTest {

    @Mock
    private InviteCodeMapper inviteCodeMapper;
    @Mock
    private InviteRelationMapper inviteRelationMapper;
    @Mock
    private InviteReceiverAccountMapper inviteReceiverAccountMapper;
    @Mock
    private InviteSettlementOrderMapper inviteSettlementOrderMapper;
    @Mock
    private InviteSystemConfigMapper inviteSystemConfigMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private TransactionTemplate transactionTemplate;

    private InviteServiceImpl inviteService;
    private FakeInviteRewardRecordMapper inviteRewardRecordMapper;

    @BeforeEach
    void setUp() {
        InviteProperties properties = new InviteProperties();
        inviteRewardRecordMapper = new FakeInviteRewardRecordMapper();
        inviteService = new InviteServiceImpl();
        ReflectionTestUtils.setField(inviteService, "inviteProperties", properties);
        ReflectionTestUtils.setField(inviteService, "inviteCodeMapper", inviteCodeMapper);
        ReflectionTestUtils.setField(inviteService, "inviteRelationMapper", inviteRelationMapper);
        ReflectionTestUtils.setField(inviteService, "inviteReceiverAccountMapper", inviteReceiverAccountMapper);
        ReflectionTestUtils.setField(inviteService, "inviteRewardRecordMapper", inviteRewardRecordMapper);
        ReflectionTestUtils.setField(inviteService, "inviteSettlementOrderMapper", inviteSettlementOrderMapper);
        ReflectionTestUtils.setField(inviteService, "inviteSystemConfigMapper", inviteSystemConfigMapper);
        ReflectionTestUtils.setField(inviteService, "userMapper", userMapper);
        ReflectionTestUtils.setField(inviteService, "transactionTemplate", transactionTemplate);
        lenient().when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> cb = inv.getArgument(0);
            return cb.doInTransaction(new SimpleTransactionStatus());
        });
    }

    @Test
    void ensureInviteCodeShouldInsertWhenMissing() {
        when(inviteCodeMapper.selectByUserId(1001L)).thenReturn(null);

        inviteService.ensureInviteCode(1001L);

        verify(inviteCodeMapper).insert(any(InviteCode.class));
    }

    @Test
    void ensureInviteCodeShouldRepairBlankInviteCodeRecord() {
        when(inviteCodeMapper.selectByUserId(1003L)).thenReturn(
                InviteCode.builder()
                        .userId(1003L)
                        .inviteCode(" ")
                        .status("ACTIVE")
                        .build(),
                InviteCode.builder()
                        .userId(1003L)
                        .inviteCode(" ")
                        .status("ACTIVE")
                        .build()
        );

        inviteService.ensureInviteCode(1003L);

        verify(inviteCodeMapper, times(1)).updateInviteCodeByUserId(eq(1003L), any(String.class), eq("ACTIVE"), any());
    }

    @Test
    void bindInviteCodeShouldRejectInvalidCode() {
        when(inviteRelationMapper.selectByInviteeUid(2001L)).thenReturn(null);
        when(inviteCodeMapper.selectByCode("BAD123")).thenReturn(null);

        assertThatThrownBy(() -> inviteService.bindInviteCodeIfNeeded(2001L, "bad123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邀请码无效");
    }

    @Test
    void bindInviteCodeShouldRejectSelfInvite() {
        when(inviteRelationMapper.selectByInviteeUid(2001L)).thenReturn(null);
        when(inviteCodeMapper.selectByCode("ABC123")).thenReturn(InviteCode.builder()
                .userId(2001L)
                .inviteCode("ABC123")
                .status("ACTIVE")
                .build());

        assertThatThrownBy(() -> inviteService.bindInviteCodeIfNeeded(2001L, "ABC123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能填写自己的邀请码");
    }

    @Test
    void overviewShouldReturnInviteCodeAndSummary() {
        when(inviteCodeMapper.selectByUserId(1001L)).thenReturn(InviteCode.builder()
                .userId(1001L)
                .inviteCode("ABC123")
                .status("ACTIVE")
                .build());
        when(inviteRelationMapper.countActiveByInviterUid(1001L)).thenReturn(2);
        inviteRewardRecordMapper.effectiveCount = 1;
        inviteRewardRecordMapper.totalRewardAmountFen = 1300L;
        inviteRewardRecordMapper.pendingAmountFen = 800L;
        when(inviteReceiverAccountMapper.selectByUserId(1001L)).thenReturn(InviteReceiverAccount.builder()
                .receiverName("张三")
                .wechatNo("wx_zhangsan")
                .phone("13800138000")
                .build());

        InviteOverviewVO vo = inviteService.overview(1001L);

        assertThat(vo.getMyInviteCode()).isEqualTo("ABC123");
        assertThat(vo.getTotalInviteCount()).isEqualTo(2);
        assertThat(vo.getReceiverConfigured()).isTrue();
    }

    @Test
    void overviewShouldRetryGeneratingInviteCodeWhenLegacyUserHasNoCodeRecord() {
        when(inviteCodeMapper.selectByUserId(1002L)).thenReturn(
                null,
                null,
                InviteCode.builder()
                        .userId(1002L)
                        .inviteCode("ZX9KQ2")
                        .status("ACTIVE")
                        .build()
        );
        when(inviteRelationMapper.countActiveByInviterUid(1002L)).thenReturn(0);
        when(inviteReceiverAccountMapper.selectByUserId(1002L)).thenReturn(null);

        InviteOverviewVO vo = inviteService.overview(1002L);

        assertThat(vo.getMyInviteCode()).isEqualTo("ZX9KQ2");
        verify(inviteCodeMapper, times(1)).insert(any(InviteCode.class));
    }

    @Test
    void saveReceiverAccountShouldInsertAndReturnConfigured() {
        SaveInviteReceiverAccountRequest req = new SaveInviteReceiverAccountRequest();
        req.setReceiverName("张三");
        req.setWechatNo("wx_zhangsan");
        req.setPhone("13800138000");
        req.setRemark("常用");
        when(inviteReceiverAccountMapper.selectByUserId(1001L)).thenReturn(null, InviteReceiverAccount.builder()
                .userId(1001L)
                .receiverName("张三")
                .wechatNo("wx_zhangsan")
                .phone("13800138000")
                .remark("常用")
                .build());

        InviteReceiverAccountVO vo = inviteService.saveReceiverAccount(1001L, req);

        assertThat(vo.getConfigured()).isTrue();
        assertThat(vo.getWechatNo()).isEqualTo("wx_zhangsan");
        verify(inviteReceiverAccountMapper).insert(any(InviteReceiverAccount.class));
    }

    @Test
    void bindInviteCodeShouldInsertRelationForNormalInviter() {
        when(inviteRelationMapper.selectByInviteeUid(2001L)).thenReturn(null);
        when(inviteCodeMapper.selectByCode("ABC123")).thenReturn(InviteCode.builder()
                .userId(1001L)
                .inviteCode("ABC123")
                .status("ACTIVE")
                .build());
        User inviter = new User();
        inviter.setId(1001L);
        inviter.setUserType(1);
        when(userMapper.selectById(1001L)).thenReturn(inviter);

        inviteService.bindInviteCodeIfNeeded(2001L, "abc123");

        verify(inviteRelationMapper).insert(any());
    }

    @Test
    void handleBrokerageOrderPaidShouldCreateTeacherAndStudentRewards() {
        when(inviteRelationMapper.selectByInviteeUid(3001L)).thenReturn(InviteRelation.builder()
                .inviterUid(1001L)
                .inviteeUid(3001L)
                .status("ACTIVE")
                .build());
        when(inviteRelationMapper.selectByInviteeUid(4001L)).thenReturn(InviteRelation.builder()
                .inviterUid(1002L)
                .inviteeUid(4001L)
                .status("ACTIVE")
                .build());
        InviteBrokeragePaidEvent event = new InviteBrokeragePaidEvent();
        event.setBrokerageOrderId(9001L);
        event.setProposalId(8001L);
        event.setTeacherUid(3001L);
        event.setStudentUid(4001L);
        event.setPayerUid(3001L);
        event.setAmountFen(10000L);
        event.setPayMethod("WECHAT");
        event.setPaidAt(LocalDateTime.of(2026, 4, 18, 10, 0));

        inviteService.handleBrokerageOrderPaid(event);

        assertThat(inviteRewardRecordMapper.insertedRecords).hasSize(2);
        assertThat(inviteRewardRecordMapper.insertedRecords)
                .extracting(InviteRewardRecord::getRewardScene)
                .containsExactly("INVITED_TUTOR_DEAL", "INVITED_STUDENT_PAID");
        assertThat(inviteRewardRecordMapper.insertedRecords)
                .extracting(InviteRewardRecord::getRewardAmountFen)
                .containsExactly(1300L, 1300L);
    }

    @Test
    void bindInviteCodeShouldCreateSystemPromotionRelationWhenUsingSystemInviteCode() {
        when(inviteRelationMapper.selectByInviteeUid(2001L)).thenReturn(null, null);
        when(inviteSystemConfigMapper.selectSingleton()).thenReturn(InviteSystemConfig.builder()
                .enabled(1)
                .systemInviteCode("CHUANGZHI")
                .systemInviteLink("http://localhost/auth/student?inviteCode=CHUANGZHI")
                .tutorInfoFeeDiscountRate(0.5D)
                .studentRewardRate(0.13D)
                .promoTitle("创智推广专属福利")
                .promoDesc("desc")
                .build());

        inviteService.bindInviteCodeIfNeeded(2001L, "chuangzhi");

        verify(inviteRelationMapper).insert(any(InviteRelation.class));
    }

    @Test
    void handleBrokerageOrderPaidShouldCreateSystemStudentCashbackWhenSystemInvited() {
        when(inviteSystemConfigMapper.selectSingleton()).thenReturn(InviteSystemConfig.builder()
                .enabled(1)
                .systemInviteCode("CHUANGZHI")
                .systemInviteLink("http://localhost/auth/student?inviteCode=CHUANGZHI")
                .tutorInfoFeeDiscountRate(0.5D)
                .studentRewardRate(0.13D)
                .promoTitle("创智推广专属福利")
                .promoDesc("desc")
                .build());
        when(inviteRelationMapper.selectByInviteeUid(3001L)).thenReturn(null);
        when(inviteRelationMapper.selectByInviteeUid(4001L)).thenReturn(InviteRelation.builder()
                .inviterUid(0L)
                .inviteeUid(4001L)
                .bindSource("SYSTEM_PROMOTION")
                .status("ACTIVE")
                .build());
        InviteBrokeragePaidEvent event = new InviteBrokeragePaidEvent();
        event.setBrokerageOrderId(9002L);
        event.setTeacherUid(3001L);
        event.setStudentUid(4001L);
        event.setPayerUid(3001L);
        event.setAmountFen(10000L);
        event.setPayMethod("WECHAT");

        inviteService.handleBrokerageOrderPaid(event);

        assertThat(inviteRewardRecordMapper.insertedRecords)
                .extracting(InviteRewardRecord::getRewardScene)
                .contains("SYSTEM_STUDENT_CASHBACK");
        assertThat(inviteRewardRecordMapper.insertedRecords)
                .filteredOn(it -> "SYSTEM_STUDENT_CASHBACK".equals(it.getRewardScene()))
                .extracting(InviteRewardRecord::getInviterUid)
                .containsExactly(4001L);
    }

    @Test
    void generateMonthlySettlementsShouldCreateOrderAndMarkRewards() {
        inviteRewardRecordMapper.settlementCandidateInviters = List.of(1001L);
        inviteRewardRecordMapper.settleableAmountFen = 2600L;
        when(inviteSettlementOrderMapper.selectByUserIdAndMonth(1001L, "2026-03")).thenReturn(null);
        when(inviteReceiverAccountMapper.selectByUserId(1001L)).thenReturn(InviteReceiverAccount.builder()
                .userId(1001L)
                .receiverName("张三")
                .wechatNo("wx_zhangsan")
                .phone("13800138000")
                .build());

        int count = inviteService.generateMonthlySettlements(LocalDate.of(2026, 4, 10));

        assertThat(count).isEqualTo(1);
        verify(inviteSettlementOrderMapper, times(1)).insert(any(InviteSettlementOrder.class));
        assertThat(inviteRewardRecordMapper.markSettlementPendingCount).isEqualTo(1);
    }

    private static final class FakeInviteRewardRecordMapper implements InviteRewardRecordMapper {

        private Integer effectiveCount = 0;
        private Long totalRewardAmountFen = 0L;
        private Long pendingAmountFen = 0L;
        private Long settleableAmountFen = 0L;
        private List<Long> settlementCandidateInviters = List.of();
        private int markSettlementPendingCount = 0;
        private final List<InviteRewardRecord> insertedRecords = new ArrayList<>();

        @Override
        public List<InviteRewardRecord> pageByInviterUid(Long inviterUid, Long cursor, Integer pageSize, String status, String scene) {
            return List.of();
        }

        @Override
        public Long sumRewardAmountByInviterUid(Long inviterUid) {
            return totalRewardAmountFen;
        }

        @Override
        public Long sumPendingAmountByInviterUid(Long inviterUid) {
            return pendingAmountFen;
        }

        @Override
        public Integer countEffectiveByInviterUid(Long inviterUid) {
            return effectiveCount;
        }

        @Override
        public Integer countEffectiveByInviterUidAndInviteeUid(Long inviterUid, Long inviteeUid) {
            return 0;
        }

        @Override
        public List<Long> listSettlementCandidateInviters(LocalDateTime cutoff, Integer limit) {
            return settlementCandidateInviters;
        }

        @Override
        public Long sumSettleableAmountByInviterUid(Long inviterUid, LocalDateTime cutoff) {
            return settleableAmountFen;
        }

        @Override
        public int markSettlementPending(Long inviterUid, String settlementMonth, LocalDateTime cutoff) {
            markSettlementPendingCount++;
            return 1;
        }

        @Override
        public int insert(InviteRewardRecord record) {
            insertedRecords.add(record);
            return 1;
        }
    }
}
