package com.ai.tutor.payment.service;

import com.ai.tutor.common.integration.BrokerageOrderFacade;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.security.IdentitySignatureUtils;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.payment.client.YungouosClient;
import com.ai.tutor.payment.config.PaymentProperties;
import com.ai.tutor.payment.controller.dto.PaymentOrderStatusResponse;
import com.ai.tutor.payment.integration.feign.AppointmentLessonPaymentFeignClient;
import com.ai.tutor.payment.integration.feign.ImBrokerageOrderFeignClient;
import com.ai.tutor.payment.model.entity.PaymentOrder;
import com.yungouos.pay.entity.PayOrder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;

public class YungouosPaymentAppServiceOrderStatusTest {

    @Test
    void getOrderStatus_noAuth_throws() {
        PaymentProperties props = new PaymentProperties();
        PaymentOrderService paymentOrderService = Mockito.mock(PaymentOrderService.class);
        BrokerageOrderFacade brokerageOrderFacade = Mockito.mock(BrokerageOrderFacade.class);
        YungouosClient yungouosClient = Mockito.mock(YungouosClient.class);
        ImBrokerageOrderFeignClient imBrokerageOrderFeignClient = Mockito.mock(ImBrokerageOrderFeignClient.class);
        AppointmentLessonPaymentFeignClient appointmentLessonPaymentFeignClient = Mockito.mock(AppointmentLessonPaymentFeignClient.class);
        IdentitySignatureUtils identitySignatureUtils = Mockito.mock(IdentitySignatureUtils.class);
        YungouosPaymentAppService svc = new YungouosPaymentAppService(
                props, paymentOrderService, brokerageOrderFacade, yungouosClient, imBrokerageOrderFeignClient, appointmentLessonPaymentFeignClient, identitySignatureUtils);

        PaymentOrder order = new PaymentOrder();
        order.setOrderNo("O1");
        order.setUserId(2L);
        when(paymentOrderService.getByOrderNo("O1")).thenReturn(order);

        assertThrows(BusinessException.class, () -> svc.getOrderStatus("O1", 1L));
    }

    @Test
    void getOrderStatus_pending_syncsSuccessFromProvider() {
        PaymentProperties props = new PaymentProperties();
        PaymentProperties.Yungouos cfg = props.getYungouos();
        cfg.setAppKey("TEST_KEY");
        cfg.setWechatMchId("WX_MCH");

        PaymentOrderService paymentOrderService = Mockito.mock(PaymentOrderService.class);
        BrokerageOrderFacade brokerageOrderFacade = Mockito.mock(BrokerageOrderFacade.class);
        YungouosClient yungouosClient = Mockito.mock(YungouosClient.class);
        ImBrokerageOrderFeignClient imBrokerageOrderFeignClient = Mockito.mock(ImBrokerageOrderFeignClient.class);
        AppointmentLessonPaymentFeignClient appointmentLessonPaymentFeignClient = Mockito.mock(AppointmentLessonPaymentFeignClient.class);
        IdentitySignatureUtils identitySignatureUtils = Mockito.mock(IdentitySignatureUtils.class);
        YungouosPaymentAppService svc = new YungouosPaymentAppService(
                props, paymentOrderService, brokerageOrderFacade, yungouosClient, imBrokerageOrderFeignClient, appointmentLessonPaymentFeignClient, identitySignatureUtils);

        PaymentOrder pending = new PaymentOrder();
        pending.setOrderNo("O2");
        pending.setUserId(1L);
        pending.setStatus("PENDING");
        pending.setAmount(100L);
        pending.setChannel("WECHAT");
        pending.setContextType("BROKERAGE_ORDER");
        pending.setContextId(22L);

        PaymentOrder success = new PaymentOrder();
        success.setOrderNo("O2");
        success.setUserId(1L);
        success.setStatus("SUCCESS");
        success.setAmount(100L);
        success.setChannel("WECHAT");
        success.setContextType("BROKERAGE_ORDER");
        success.setContextId(22L);
        success.setTransactionId("PAYNO2");
        success.setProviderOrderNo("YGO2");

        PayOrder providerOrder = new PayOrder();
        providerOrder.setPayStatus(1);
        providerOrder.setMoney("1.00");
        providerOrder.setPayNo("PAYNO2");
        providerOrder.setOrderNo("YGO2");

        when(paymentOrderService.getByOrderNo("O2")).thenReturn(pending, success, success);
        when(yungouosClient.getOrderInfoByOutTradeNo("O2", "WX_MCH", "TEST_KEY")).thenReturn(providerOrder);
        when(paymentOrderService.updateSuccessFromProviderQuery("O2", "PAYNO2", "YGO2", null)).thenReturn(true);
        when(identitySignatureUtils.sign(anyLong(), anyInt(), anyLong(), any(), any())).thenReturn("SIGN");
        BaseResponse<Boolean> ok = new BaseResponse<>();
        ok.setCode(ErrorCode.SUCCESS.getCode());
        ok.setData(Boolean.TRUE);
        when(imBrokerageOrderFeignClient.onPaymentSuccess(any(), any(), any(), any(), any())).thenReturn(ok);

        PaymentOrderStatusResponse resp = svc.getOrderStatus("O2", 1L);
        assertEquals("SUCCESS", resp.getStatus());
    }
}
