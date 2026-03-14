package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.videocallimservice.chat.domain.entity.BrokerageOrder;
import com.ai.tutor.videocallimservice.chat.domain.enums.BrokerageOrderStatus;
import com.ai.tutor.videocallimservice.chat.mapper.BrokerageOrderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class BrokerageOrderServiceCancelTest {

    @Test
    void cancelShouldUpdateStatusWhenPending() {
        BrokerageOrderMapper mapper = mock(BrokerageOrderMapper.class);

        BrokerageOrder pending = new BrokerageOrder();
        pending.setId(1L);
        pending.setPayerUid(10L);
        pending.setStatus(BrokerageOrderStatus.PENDING.name());

        BrokerageOrder canceled = new BrokerageOrder();
        canceled.setId(1L);
        canceled.setPayerUid(10L);
        canceled.setStatus(BrokerageOrderStatus.CANCELED.name());

        when(mapper.selectById(1L)).thenReturn(pending, canceled);
        when(mapper.cancel(1L)).thenReturn(1);

        BrokerageOrderService service = new BrokerageOrderService();
        ReflectionTestUtils.setField(service, "brokerageOrderMapper", mapper);

        com.ai.tutor.videocallimservice.chat.domain.vo.response.BrokerageOrderVO vo = service.cancel(1L, 10L);
        assertThat(vo).isNotNull();
        assertThat(vo.getStatus()).isEqualTo(BrokerageOrderStatus.CANCELED.name());

        verify(mapper, times(2)).selectById(1L);
        verify(mapper, times(1)).cancel(1L);
    }

    @Test
    void cancelShouldBeIdempotentWhenAlreadyCanceled() {
        BrokerageOrderMapper mapper = mock(BrokerageOrderMapper.class);

        BrokerageOrder canceled = new BrokerageOrder();
        canceled.setId(1L);
        canceled.setPayerUid(10L);
        canceled.setStatus(BrokerageOrderStatus.CANCELED.name());

        when(mapper.selectById(1L)).thenReturn(canceled);

        BrokerageOrderService service = new BrokerageOrderService();
        ReflectionTestUtils.setField(service, "brokerageOrderMapper", mapper);

        com.ai.tutor.videocallimservice.chat.domain.vo.response.BrokerageOrderVO vo = service.cancel(1L, 10L);
        assertThat(vo).isNotNull();
        assertThat(vo.getStatus()).isEqualTo(BrokerageOrderStatus.CANCELED.name());

        verify(mapper, times(1)).selectById(1L);
        verify(mapper, never()).cancel(anyLong());
    }

    @Test
    void cancelShouldFailWhenPaid() {
        BrokerageOrderMapper mapper = mock(BrokerageOrderMapper.class);

        BrokerageOrder paid = new BrokerageOrder();
        paid.setId(1L);
        paid.setPayerUid(10L);
        paid.setStatus(BrokerageOrderStatus.PAID.name());

        when(mapper.selectById(1L)).thenReturn(paid);

        BrokerageOrderService service = new BrokerageOrderService();
        ReflectionTestUtils.setField(service, "brokerageOrderMapper", mapper);

        assertThrows(BusinessException.class, () -> service.cancel(1L, 10L));
        verify(mapper, times(1)).selectById(1L);
        verify(mapper, never()).cancel(anyLong());
    }
}
