package com.ai.tutor.payment.service;

import com.ai.tutor.common.integration.BrokerageOrderFacade;
import com.ai.tutor.common.integration.BrokerageOrderPayInfo;
import com.ai.tutor.payment.client.YungouosClient;
import com.ai.tutor.payment.config.PaymentProperties;
import com.ai.tutor.payment.controller.dto.PrepayRequest;
import com.ai.tutor.payment.controller.dto.PrepayResponse;
import com.ai.tutor.payment.model.entity.PaymentOrder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class YungouosPaymentAppServicePrepayTest {

    @Test
    void prepay_wechat_returnsQrCodeUrl() {
        PaymentProperties props = new PaymentProperties();
        PaymentProperties.Yungouos cfg = props.getYungouos();
        cfg.setAppId("TEST_APP");
        cfg.setAppKey("TEST_KEY");
        cfg.setWechatMchId("WX_MCH");
        cfg.setNotifyUrl("https://example.com/payment/notify/yungouos");
        cfg.setNativePayType("2");

        PaymentOrderService paymentOrderService = Mockito.mock(PaymentOrderService.class);
        BrokerageOrderFacade brokerageOrderFacade = Mockito.mock(BrokerageOrderFacade.class);
        YungouosClient yungouosClient = Mockito.mock(YungouosClient.class);

        YungouosPaymentAppService svc = new YungouosPaymentAppService(props, paymentOrderService, brokerageOrderFacade, yungouosClient);

        BrokerageOrderPayInfo payInfo = new BrokerageOrderPayInfo();
        payInfo.setOrderId(10L);
        payInfo.setPayerUid(1L);
        payInfo.setAmountFen(19900L);
        when(brokerageOrderFacade.getPayableOrder(10L, 1L)).thenReturn(payInfo);

        PaymentOrder created = new PaymentOrder();
        created.setOrderNo("O1");
        created.setUserId(1L);
        created.setAmount(19900L);
        created.setChannel("WECHAT");
        created.setContextType("BROKERAGE_ORDER");
        created.setContextId(10L);
        created.setBody("中介费支付");
        created.setExpireTime(LocalDateTime.now().plusMinutes(5));
        when(paymentOrderService.createOrReusePending(anyString(), anyLong(), anyLong(), anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(created);

        when(yungouosClient.wechatNativePay(eq("O1"), anyString(), eq("WX_MCH"), anyString(), eq("2"), any(), any(), any(), any(), eq("TEST_KEY")))
                .thenReturn("http://img.example.com/q.png");

        when(paymentOrderService.updatePayData(eq("O1"), anyString(), any(LocalDateTime.class))).thenReturn(true);

        PaymentOrder updated = new PaymentOrder();
        updated.setOrderNo("O1");
        updated.setAmount(19900L);
        updated.setChannel("WECHAT");
        updated.setExpireTime(LocalDateTime.now().plusMinutes(5));
        updated.setPayData("{\"type\":\"2\",\"data\":\"http://img.example.com/q.png\",\"channel\":\"WECHAT\",\"provider\":\"YUNGOUOS\"}");
        when(paymentOrderService.getByOrderNo("O1")).thenReturn(updated);

        PrepayRequest req = new PrepayRequest();
        req.setContextType("BROKERAGE_ORDER");
        req.setContextId(10L);
        req.setChannel("WECHAT");

        PrepayResponse resp = svc.prepay(req, 1L, "127.0.0.1");
        assertNotNull(resp);
        assertEquals("O1", resp.getOrderNo());
        assertEquals(19900L, resp.getAmountFen());
        assertEquals("WECHAT", resp.getChannel());
        assertEquals("http://img.example.com/q.png", resp.getQrCodeUrl());
    }
}
