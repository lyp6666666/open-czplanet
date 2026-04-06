package com.ai.tutor.admin.service;

import com.ai.tutor.admin.integration.feign.PaymentRefundFeignClient;
import com.ai.tutor.admin.mapper.AdminMessageMapper;
import com.ai.tutor.admin.mapper.AdminRefundMapper;
import com.ai.tutor.admin.mapper.AdminRefundRequestMapper;
import com.ai.tutor.admin.model.dto.PaymentRefundResponse;
import com.ai.tutor.admin.model.entity.RefundRequestRecord;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.admin.service.impl.AdminRefundRequestServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminRefundRequestServiceImplTest {

    @Mock
    private AdminRefundRequestMapper adminRefundRequestMapper;
    @Mock
    private AdminRefundMapper adminRefundMapper;
    @Mock
    private AdminMessageMapper adminMessageMapper;
    @Mock
    private PaymentRefundFeignClient paymentRefundFeignClient;

    @InjectMocks
    private AdminRefundRequestServiceImpl service;

    @Test
    void approveShouldCallPaymentRefundAndUpdateStatus() {
        RefundRequestRecord record = new RefundRequestRecord();
        record.setId(10L);
        record.setStatus("PENDING");
        record.setBrokerageOrderId(99L);
        record.setRefundAmountFen(6000L);
        record.setReason("试课不通过");
        record.setCourseId(1L);
        when(adminRefundRequestMapper.selectById(10L)).thenReturn(record);

        PaymentRefundResponse pr = new PaymentRefundResponse();
        pr.setRefundNo("R1");
        pr.setStatus("SUCCESS");
        when(paymentRefundFeignClient.refund(any())).thenReturn(new BaseResponse<>(0, pr, "ok"));

        when(adminRefundRequestMapper.approve(eq(10L), eq(100L), any(), any(LocalDateTime.class))).thenReturn(1);
        when(adminRefundRequestMapper.markOrderRefunded(99L, 6000L)).thenReturn(1);
        when(adminRefundRequestMapper.markCourseRefundedById(1L)).thenReturn(1);

        service.approve(10L, 100L, "ok");

        verify(paymentRefundFeignClient).refund(any());
        verify(adminRefundRequestMapper).approve(eq(10L), eq(100L), any(), any(LocalDateTime.class));
        verify(adminRefundRequestMapper).markOrderRefunded(99L, 6000L);
        verify(adminRefundRequestMapper).markCourseRefundedById(1L);
    }
}

