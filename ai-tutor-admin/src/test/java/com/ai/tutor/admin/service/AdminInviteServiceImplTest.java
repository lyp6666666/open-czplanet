package com.ai.tutor.admin.service;

import com.ai.tutor.admin.model.dto.AdminInviteSystemConfigRequest;
import com.ai.tutor.admin.model.vo.AdminInviteSystemConfigVO;
import com.ai.tutor.admin.mapper.AdminInviteMapper;
import com.ai.tutor.admin.model.vo.AdminInviteSettlementVO;
import com.ai.tutor.admin.service.impl.AdminInviteServiceImpl;
import com.ai.tutor.admin.model.vo.PageResult;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInviteServiceImplTest {

    @Mock
    private AdminInviteMapper adminInviteMapper;
    @Mock
    private TransactionTemplate transactionTemplate;

    private AdminInviteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminInviteServiceImpl();
        ReflectionTestUtils.setField(service, "adminInviteMapper", adminInviteMapper);
        ReflectionTestUtils.setField(service, "transactionTemplate", transactionTemplate);
        lenient().when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        });
    }

    @Test
    void listSettlementsShouldNormalizePagingAndBlankFilters() {
        when(adminInviteMapper.listSettlements(0L, 200, 1001L, null, null)).thenReturn(List.of());
        when(adminInviteMapper.countSettlements(1001L, null, null)).thenReturn(0L);

        PageResult<AdminInviteSettlementVO> page = service.listSettlements(0, 500, 1001L, "  ", "");

        assertThat(page.getCurrent()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(200);
        assertThat(page.getTotal()).isZero();
    }

    @Test
    void markSettlementPaidShouldUpdateSettlementAndRelatedRewardsInOneTransaction() {
        AdminInviteSettlementVO settlement = new AdminInviteSettlementVO();
        settlement.setId(99L);
        settlement.setUserId(1001L);
        settlement.setSettlementMonth("2026-03");
        when(adminInviteMapper.selectSettlementById(99L)).thenReturn(settlement);
        when(adminInviteMapper.markSettlementPaid(eq(99L), any(LocalDateTime.class))).thenReturn(1);

        service.markSettlementPaid(99L);

        verify(adminInviteMapper).selectSettlementById(99L);
        verify(adminInviteMapper).markSettlementPaid(eq(99L), any(LocalDateTime.class));
        /*
         * 企业规范：财务确认结算单已打款后，必须同步推进返利明细状态，避免用户端和管理端状态口径不一致。
         */
        verify(adminInviteMapper).markRewardsPaid(1001L, "2026-03");
    }

    @Test
    void markSettlementPaidShouldRejectMissingSettlement() {
        when(adminInviteMapper.selectSettlementById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.markSettlementPaid(99L))
                .isInstanceOf(BusinessException.class);

        verify(adminInviteMapper).selectSettlementById(99L);
        verifyNoMoreInteractions(adminInviteMapper);
    }

    @Test
    void markSettlementPaidShouldRejectIllegalStatusTransition() {
        AdminInviteSettlementVO settlement = new AdminInviteSettlementVO();
        settlement.setId(99L);
        settlement.setUserId(1001L);
        settlement.setSettlementMonth("2026-03");
        when(adminInviteMapper.selectSettlementById(99L)).thenReturn(settlement);
        when(adminInviteMapper.markSettlementPaid(eq(99L), any(LocalDateTime.class))).thenReturn(0);

        assertThatThrownBy(() -> service.markSettlementPaid(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前结算单状态不可标记已打款");
    }

    @Test
    void markSettlementFailedShouldUseDefaultReasonWhenBlank() {
        when(adminInviteMapper.markSettlementFailed(99L, "财务打款失败")).thenReturn(1);

        service.markSettlementFailed(99L, "  ");

        verify(adminInviteMapper).markSettlementFailed(99L, "财务打款失败");
    }

    @Test
    void markSettlementFailedShouldRejectIllegalStatusTransition() {
        when(adminInviteMapper.markSettlementFailed(99L, "微信号异常")).thenReturn(0);

        assertThatThrownBy(() -> service.markSettlementFailed(99L, "微信号异常"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前结算单状态不可标记失败");
    }

    @Test
    void systemConfigShouldInsertDefaultWhenMissing() {
        when(adminInviteMapper.selectSystemConfig()).thenReturn(null, AdminInviteSystemConfigVO.builder()
                .enabled(true)
                .systemInviteCode("CHUANGZHI")
                .systemInviteLink("http://localhost:5173/auth/student?inviteCode=CHUANGZHI")
                .tutorInfoFeeDiscountRate(0.5D)
                .studentRewardRate(0.13D)
                .promoTitle("创智推广专属福利")
                .promoDesc("desc")
                .build());

        AdminInviteSystemConfigVO config = service.systemConfig();

        assertThat(config.getSystemInviteCode()).isEqualTo("CHUANGZHI");
        verify(adminInviteMapper).insertSystemConfig(any(AdminInviteSystemConfigVO.class));
    }

    @Test
    void saveSystemConfigShouldNormalizeAndPersist() {
        AdminInviteSystemConfigRequest request = new AdminInviteSystemConfigRequest();
        request.setEnabled(true);
        request.setSystemInviteCode("huoyue");
        request.setSystemInviteLink("http://localhost:5173/auth/student?inviteCode=CHUANGZHI");
        request.setTutorInfoFeeDiscountRate(0.5D);
        request.setStudentRewardRate(0.13D);
        request.setPromoTitle("创智推广专属福利");
        request.setPromoDesc("desc");
        when(adminInviteMapper.selectSystemConfig()).thenReturn(
                AdminInviteSystemConfigVO.builder().enabled(true).systemInviteCode("OLD").build(),
                AdminInviteSystemConfigVO.builder()
                        .enabled(true)
                        .systemInviteCode("CHUANGZHI")
                        .systemInviteLink("http://localhost:5173/auth/student?inviteCode=CHUANGZHI")
                        .tutorInfoFeeDiscountRate(0.5D)
                        .studentRewardRate(0.13D)
                        .promoTitle("创智推广专属福利")
                        .promoDesc("desc")
                        .build()
        );
        when(adminInviteMapper.updateSystemConfig(any(AdminInviteSystemConfigVO.class))).thenReturn(1);

        AdminInviteSystemConfigVO saved = service.saveSystemConfig(request);

        assertThat(saved.getSystemInviteCode()).isEqualTo("CHUANGZHI");
        verify(adminInviteMapper).updateSystemConfig(any(AdminInviteSystemConfigVO.class));
    }
}
