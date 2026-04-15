package com.ai.tutor.payment.service;

import com.ai.tutor.common.integration.BrokerageOrderFacade;
import com.ai.tutor.common.security.IdentitySignatureUtils;
import com.ai.tutor.payment.client.YungouosClient;
import com.ai.tutor.payment.config.PaymentProperties;
import com.ai.tutor.payment.integration.feign.ImBrokerageOrderFeignClient;
import com.ai.tutor.payment.enums.PaymentChannel;
import com.ai.tutor.payment.model.entity.PaymentOrder;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.enums.ErrorCode;
import com.yungouos.pay.util.PaySignUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class YungouosPaymentAppServiceNotifyTest {

    @Test
    void handleNotify_success() {
        PaymentProperties props = new PaymentProperties();
        PaymentProperties.Yungouos cfg = props.getYungouos();
        cfg.setAppKey("TEST_KEY");
        cfg.setNotifyUrl("https://example.com/payment/notify/yungouos");

        PaymentOrderService paymentOrderService = Mockito.mock(PaymentOrderService.class);
        BrokerageOrderFacade brokerageOrderFacade = Mockito.mock(BrokerageOrderFacade.class);
        YungouosClient yungouosClient = Mockito.mock(YungouosClient.class);
        ImBrokerageOrderFeignClient imBrokerageOrderFeignClient = Mockito.mock(ImBrokerageOrderFeignClient.class);
        IdentitySignatureUtils identitySignatureUtils = Mockito.mock(IdentitySignatureUtils.class);
        YungouosPaymentAppService svc = new YungouosPaymentAppService(
                props, paymentOrderService, brokerageOrderFacade, yungouosClient, imBrokerageOrderFeignClient, identitySignatureUtils);

        PaymentOrder order = new PaymentOrder();
        order.setOrderNo("ORDER_NO_1");
        order.setAmount(19900L);
        order.setChannel(PaymentChannel.WECHAT.getCode());
        order.setUserId(1L);
        order.setStatus("SUCCESS");
        order.setContextType("BROKERAGE_ORDER");
        order.setContextId(100L);

        when(paymentOrderService.getByOrderNo("ORDER_NO_1")).thenReturn(order).thenReturn(order);
        when(paymentOrderService.updateSuccessFromNotify(eq("ORDER_NO_1"), anyString(), any(), any(LocalDateTime.class), eq(1))).thenReturn(true);
        when(identitySignatureUtils.sign(anyLong(), anyInt(), anyLong(), anyString(), anyString())).thenReturn("SIGN");
        BaseResponse<Boolean> successResp = new BaseResponse<>();
        successResp.setCode(ErrorCode.SUCCESS.getCode());
        successResp.setData(Boolean.TRUE);
        when(imBrokerageOrderFeignClient.onPaymentSuccess(anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(successResp);

        MockHttpServletRequest request = new MockHttpServletRequest();
        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", "ORDER_NO_1");
        params.put("total_fee", "199.00");
        params.put("mch_id", "MCH_1");
        params.put("body", "中介费支付");
        params.put("pay_no", "PAY_NO_1");
        params.put("order_no", "Y_ORDER_1");
        params.put("pay_time", "2026-03-10 00:00:00");
        params.put("sign", PaySignUtil.createSign(new HashMap<>(params), "TEST_KEY"));

        for (Map.Entry<String, String> e : params.entrySet()) {
            request.addParameter(e.getKey(), e.getValue());
        }

        String resp = svc.handleNotify(request);
        assertEquals("SUCCESS", resp);
    }

    @Test
    void handleNotify_amountMismatch_fail() {
        PaymentProperties props = new PaymentProperties();
        PaymentProperties.Yungouos cfg = props.getYungouos();
        cfg.setAppKey("TEST_KEY");
        cfg.setNotifyUrl("https://example.com/payment/notify/yungouos");

        PaymentOrderService paymentOrderService = Mockito.mock(PaymentOrderService.class);
        BrokerageOrderFacade brokerageOrderFacade = Mockito.mock(BrokerageOrderFacade.class);
        YungouosClient yungouosClient = Mockito.mock(YungouosClient.class);
        ImBrokerageOrderFeignClient imBrokerageOrderFeignClient = Mockito.mock(ImBrokerageOrderFeignClient.class);
        IdentitySignatureUtils identitySignatureUtils = Mockito.mock(IdentitySignatureUtils.class);
        YungouosPaymentAppService svc = new YungouosPaymentAppService(
                props, paymentOrderService, brokerageOrderFacade, yungouosClient, imBrokerageOrderFeignClient, identitySignatureUtils);

        PaymentOrder order = new PaymentOrder();
        order.setOrderNo("ORDER_NO_2");
        order.setAmount(20000L);
        order.setChannel(PaymentChannel.ALIPAY.getCode());
        order.setUserId(1L);

        when(paymentOrderService.getByOrderNo("ORDER_NO_2")).thenReturn(order);
        when(paymentOrderService.recordNotifyReceipt(eq("ORDER_NO_2"), anyInt())).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", "ORDER_NO_2");
        params.put("total_fee", "199.00");
        params.put("mch_id", "MCH_2");
        params.put("body", "中介费支付");
        params.put("sign", PaySignUtil.createSign(new HashMap<>(params), "TEST_KEY"));
        for (Map.Entry<String, String> e : params.entrySet()) {
            request.addParameter(e.getKey(), e.getValue());
        }

        String resp = svc.handleNotify(request);
        assertEquals("FAIL", resp);
    }

    @Test
    void handleNotify_signInvalid_fail() {
        PaymentProperties props = new PaymentProperties();
        PaymentProperties.Yungouos cfg = props.getYungouos();
        cfg.setAppKey("TEST_KEY");
        cfg.setNotifyUrl("https://example.com/payment/notify/yungouos");

        PaymentOrderService paymentOrderService = Mockito.mock(PaymentOrderService.class);
        BrokerageOrderFacade brokerageOrderFacade = Mockito.mock(BrokerageOrderFacade.class);
        com.ai.tutor.payment.client.YungouosClient yungouosClient = Mockito.mock(com.ai.tutor.payment.client.YungouosClient.class);
        ImBrokerageOrderFeignClient imBrokerageOrderFeignClient = Mockito.mock(ImBrokerageOrderFeignClient.class);
        IdentitySignatureUtils identitySignatureUtils = Mockito.mock(IdentitySignatureUtils.class);
        YungouosPaymentAppService svc = new YungouosPaymentAppService(
                props, paymentOrderService, brokerageOrderFacade, yungouosClient, imBrokerageOrderFeignClient, identitySignatureUtils);

        PaymentOrder order = new PaymentOrder();
        order.setOrderNo("ORDER_NO_3");
        order.setAmount(100L);
        when(paymentOrderService.getByOrderNo("ORDER_NO_3")).thenReturn(order);
        when(paymentOrderService.recordNotifyReceipt(eq("ORDER_NO_3"), anyInt())).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("out_trade_no", "ORDER_NO_3");
        request.addParameter("total_fee", "1.00");
        request.addParameter("sign", "BAD_SIGN");

        String resp = svc.handleNotify(request);
        assertEquals("FAIL", resp);
    }
}
