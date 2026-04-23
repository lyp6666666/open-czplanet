package com.ai.tutor.payment.service;

import com.ai.tutor.common.metrics.BizKpiMetrics;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.payment.client.YungouosClient;
import com.ai.tutor.payment.config.PaymentProperties;
import com.ai.tutor.payment.controller.dto.InternalRefundRequest;
import com.ai.tutor.payment.controller.dto.InternalRefundResponse;
import com.ai.tutor.payment.enums.PaymentStatus;
import com.ai.tutor.payment.model.entity.PaymentOrder;
import com.ai.tutor.payment.model.entity.PaymentRefund;
import com.yungouos.pay.entity.RefundOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRefundAppServiceTest {

    @Mock
    private PaymentProperties paymentProperties;
    @Mock
    private PaymentOrderService paymentOrderService;
    @Mock
    private PaymentRefundService paymentRefundService;
    @Mock
    private YungouosClient yungouosClient;
    @Mock
    private BizKpiMetrics bizKpiMetrics;

    @InjectMocks
    private PaymentRefundAppService appService;

    @Test
    void shouldReturnExistingRefundByRequestId() {
        when(paymentProperties.getEnabled()).thenReturn(true);
        PaymentRefund existing = new PaymentRefund();
        existing.setRefundNo("R1");
        existing.setStatus("SUCCESS");
        when(paymentRefundService.getByRequestId(10L)).thenReturn(existing);

        InternalRefundRequest req = new InternalRefundRequest();
        req.setRequestId(10L);
        req.setRefundAmountFen(100L);
        req.setReason("ok");
        req.setPaymentOrderNo("P1");

        InternalRefundResponse resp = appService.refund(req);

        assertThat(resp.getRefundNo()).isEqualTo("R1");
        assertThat(resp.getStatus()).isEqualTo("SUCCESS");
        verifyNoInteractions(paymentOrderService, yungouosClient);
    }

    @Test
    void shouldCreateRefundAndCallWechatRefund() {
        when(paymentProperties.getEnabled()).thenReturn(true);
        PaymentRefund stored = new PaymentRefund();
        stored.setRefundNo("REFUND_NO_1");
        stored.setStatus("SUCCESS");
        when(paymentRefundService.getByRequestId(99L)).thenReturn(null, stored);

        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setOrderNo("ORDER_NO_1");
        paymentOrder.setAmount(10000L);
        paymentOrder.setStatus(PaymentStatus.SUCCESS.getCode());
        paymentOrder.setChannel("WECHAT");
        when(paymentOrderService.getByOrderNo("ORDER_NO_1")).thenReturn(paymentOrder);

        PaymentProperties.Yungouos yungouos = new PaymentProperties.Yungouos();
        yungouos.setBaseUrl("mock://yungouos");
        yungouos.setAppKey("TEST_KEY");
        yungouos.setWechatMchId("MOCK_MCH");
        when(paymentProperties.getYungouos()).thenReturn(yungouos);

        RefundOrder refundOrder = new RefundOrder();
        refundOrder.setRefundNo("PROVIDER_REFUND_NO");
        refundOrder.setRefundStatus(1);
        when(yungouosClient.wechatRefund(eq("ORDER_NO_1"), eq("MOCK_MCH"), anyString(), anyString(), anyString(), isNull(), eq("TEST_KEY")))
                .thenReturn(refundOrder);

        ArgumentCaptor<PaymentRefund> refundCaptor = ArgumentCaptor.forClass(PaymentRefund.class);
        doReturn(true).when(paymentRefundService).save(refundCaptor.capture());
        doReturn(true).when(paymentRefundService).updateById(any(PaymentRefund.class));

        InternalRefundRequest req = new InternalRefundRequest();
        req.setRequestId(99L);
        req.setPaymentOrderNo("ORDER_NO_1");
        req.setRefundAmountFen(6000L);
        req.setReason("试课不通过");

        InternalRefundResponse resp = appService.refund(req);

        assertThat(resp.getStatus()).isEqualTo("SUCCESS");
        verify(yungouosClient).wechatRefund(eq("ORDER_NO_1"), eq("MOCK_MCH"), eq("60.00"), anyString(), eq("试课不通过"), isNull(), eq("TEST_KEY"));
        verify(bizKpiMetrics).incRefundRequest("brokerage_order");
        verify(bizKpiMetrics).incRefund();
        verify(bizKpiMetrics).addRefundAmountFen(6000L);

        PaymentRefund created = refundCaptor.getValue();
        assertThat(created.getPaymentOrderNo()).isEqualTo("ORDER_NO_1");
        assertThat(created.getRefundAmountFen()).isEqualTo(6000L);
        assertThat(created.getRequestId()).isEqualTo(99L);
    }

    @Test
    void shouldRejectRefundAmountGreaterThanPaid() {
        when(paymentProperties.getEnabled()).thenReturn(true);
        when(paymentRefundService.getByRequestId(1L)).thenReturn(null);
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setOrderNo("O1");
        paymentOrder.setAmount(100L);
        paymentOrder.setStatus(PaymentStatus.SUCCESS.getCode());
        paymentOrder.setChannel("WECHAT");
        when(paymentOrderService.getByOrderNo("O1")).thenReturn(paymentOrder);

        InternalRefundRequest req = new InternalRefundRequest();
        req.setRequestId(1L);
        req.setPaymentOrderNo("O1");
        req.setRefundAmountFen(200L);
        req.setReason("bad");

        assertThatThrownBy(() -> appService.refund(req)).isInstanceOf(BusinessException.class);
    }
}
