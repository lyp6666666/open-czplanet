package com.ai.tutor.payment.integration;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.integration.BrokerageOrderPayInfo;
import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.payment.integration.feign.ImBrokerageOrderFeignClient;
import com.ai.tutor.utils.RequestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HttpBrokerageOrderFacadeTest {

    @Mock
    private ImBrokerageOrderFeignClient client;

    @InjectMocks
    private HttpBrokerageOrderFacade facade;

    @AfterEach
    void tearDown() {
        RequestHolder.remove();
    }

    @Test
    void shouldCallFeignClientForPayableOrder() {
        bindUid(206L);
        BrokerageOrderPayInfo info = new BrokerageOrderPayInfo();
        info.setOrderId(1L);
        info.setPayerUid(206L);
        info.setAmountFen(19900L);
        info.setStatus("PENDING");
        when(client.getPayableOrder(1L, 206L)).thenReturn(new BaseResponse<>(0, info, "ok"));

        BrokerageOrderPayInfo result = facade.getPayableOrder(1L, 206L);

        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getPayerUid()).isEqualTo(206L);
        verify(client).getPayableOrder(1L, 206L);
    }

    @Test
    void shouldThrowWhenUidDoesNotMatchRequestContext() {
        bindUid(300L);

        assertThatThrownBy(() -> facade.getPayableOrder(1L, 206L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldThrowWhenFeignReturnsBusinessError() {
        bindUid(206L);
        when(client.getPayableOrder(1L, 206L)).thenReturn(new BaseResponse<>(50001, null, "order invalid"));

        assertThatThrownBy(() -> facade.getPayableOrder(1L, 206L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("order invalid");
    }

    private static void bindUid(Long uid) {
        RequestInfo info = new RequestInfo();
        info.setUid(uid);
        info.setRole(1);
        RequestHolder.set(info);
    }
}
