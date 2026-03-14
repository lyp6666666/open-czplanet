package com.ai.tutor.payment.service;

import com.ai.tutor.common.integration.BrokerageOrderFacade;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.payment.client.YungouosClient;
import com.ai.tutor.payment.config.PaymentProperties;
import com.ai.tutor.payment.model.entity.PaymentOrder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class YungouosPaymentAppServiceOrderStatusTest {

    @Test
    void getOrderStatus_noAuth_throws() {
        PaymentProperties props = new PaymentProperties();
        PaymentOrderService paymentOrderService = Mockito.mock(PaymentOrderService.class);
        BrokerageOrderFacade brokerageOrderFacade = Mockito.mock(BrokerageOrderFacade.class);
        YungouosClient yungouosClient = Mockito.mock(YungouosClient.class);
        YungouosPaymentAppService svc = new YungouosPaymentAppService(props, paymentOrderService, brokerageOrderFacade, yungouosClient);

        PaymentOrder order = new PaymentOrder();
        order.setOrderNo("O1");
        order.setUserId(2L);
        when(paymentOrderService.getByOrderNo("O1")).thenReturn(order);

        assertThrows(BusinessException.class, () -> svc.getOrderStatus("O1", 1L));
    }
}

