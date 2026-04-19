package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.common.event.InviteBrokeragePaidEvent;
import com.ai.tutor.common.integration.InviteSystemBenefitInfo;
import com.ai.tutor.videocallimservice.chat.domain.entity.BrokerageOrder;
import com.ai.tutor.videocallimservice.chat.domain.entity.TutorApplication;
import com.ai.tutor.videocallimservice.chat.domain.enums.BrokerageOrderStatus;
import com.ai.tutor.videocallimservice.chat.mapper.BrokerageOrderMapper;
import com.ai.tutor.videocallimservice.chat.mapper.TutorApplicationMapper;
import com.ai.tutor.videocallimservice.integration.AppointmentInternalClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BrokerageOrderServiceInviteNotifyTest {

    @Test
    void onPaymentSuccessShouldNotifyInviteServiceWithTeacherAndStudent() {
        BrokerageOrderMapper brokerageOrderMapper = mock(BrokerageOrderMapper.class);
        TutorApplicationMapper tutorApplicationMapper = mock(TutorApplicationMapper.class);
        TutorApplicationService tutorApplicationService = mock(TutorApplicationService.class);
        AppointmentInternalClient appointmentInternalClient = mock(AppointmentInternalClient.class);
        ObjectProvider<AppointmentInternalClient> provider = mock(ObjectProvider.class);

        BrokerageOrder pending = BrokerageOrder.builder()
                .id(9001L)
                .applicationId(7001L)
                .payerUid(3001L)
                .amountFen(10000L)
                .status(BrokerageOrderStatus.PENDING.name())
                .build();
        BrokerageOrder paid = BrokerageOrder.builder()
                .id(9001L)
                .applicationId(7001L)
                .payerUid(3001L)
                .amountFen(10000L)
                .payMethod("WECHAT")
                .paidAt(LocalDateTime.of(2026, 4, 18, 10, 0))
                .status(BrokerageOrderStatus.PAID.name())
                .build();
        TutorApplication application = TutorApplication.builder()
                .id(7001L)
                .senderUid(3001L)
                .senderRole("TEACHER")
                .receiverUid(4001L)
                .receiverRole("STUDENT")
                .build();

        when(brokerageOrderMapper.selectById(9001L)).thenReturn(pending, paid);
        when(brokerageOrderMapper.markPaidWithMethod(any(), any(), any())).thenReturn(1);
        when(tutorApplicationMapper.selectById(7001L)).thenReturn(application);
        when(provider.getIfAvailable()).thenReturn(appointmentInternalClient);

        BrokerageOrderService service = new BrokerageOrderService();
        ReflectionTestUtils.setField(service, "brokerageOrderMapper", brokerageOrderMapper);
        ReflectionTestUtils.setField(service, "tutorApplicationMapper", tutorApplicationMapper);
        ReflectionTestUtils.setField(service, "tutorApplicationService", tutorApplicationService);
        ReflectionTestUtils.setField(service, "appointmentInternalClientProvider", provider);

        service.onPaymentSuccess(9001L, paid.getPaidAt(), "WECHAT");

        ArgumentCaptor<InviteBrokeragePaidEvent> captor = ArgumentCaptor.forClass(InviteBrokeragePaidEvent.class);
        verify(appointmentInternalClient).notifyInviteBrokeragePaid(captor.capture());
        assertThat(captor.getValue().getBrokerageOrderId()).isEqualTo(9001L);
        assertThat(captor.getValue().getTeacherUid()).isEqualTo(3001L);
        assertThat(captor.getValue().getStudentUid()).isEqualTo(4001L);
        assertThat(captor.getValue().getAmountFen()).isEqualTo(10000L);
        verify(tutorApplicationService).onBrokerageOrderPaid(7001L);
    }

    @Test
    void applySystemInvitePromotionShouldReturnHalfPriceWhenTeacherMatchedPromotion() {
        AppointmentInternalClient appointmentInternalClient = mock(AppointmentInternalClient.class);
        ObjectProvider<AppointmentInternalClient> provider = mock(ObjectProvider.class);
        InviteSystemBenefitInfo benefitInfo = new InviteSystemBenefitInfo();
        benefitInfo.setEnabled(true);
        benefitInfo.setSystemInvited(true);
        benefitInfo.setSystemInviteCode("CHUANGZHI");
        benefitInfo.setTutorInfoFeeDiscountRate(0.5D);
        when(provider.getIfAvailable()).thenReturn(appointmentInternalClient);
        when(appointmentInternalClient.getInviteSystemBenefit(3001L)).thenReturn(benefitInfo);

        BrokerageOrderService service = new BrokerageOrderService();
        ReflectionTestUtils.setField(service, "appointmentInternalClientProvider", provider);

        BrokerageOrderService.PromotionAmount amount = service.applySystemInvitePromotion(3001L, 10000L);

        assertThat(amount.amountFen()).isEqualTo(5000L);
        assertThat(amount.discountAmountFen()).isEqualTo(5000L);
        assertThat(amount.promotionType()).isEqualTo("SYSTEM_INVITE");
    }
}
