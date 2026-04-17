package com.ai.tutor.admin.service;

import com.ai.tutor.admin.integration.feign.PaymentRefundFeignClient;
import com.ai.tutor.admin.mapper.AdminMessageMapper;
import com.ai.tutor.admin.mapper.AdminRefundMapper;
import com.ai.tutor.admin.mapper.AdminRefundRequestMapper;
import com.ai.tutor.admin.mapper.UserMapper;
import com.ai.tutor.admin.model.dto.PaymentRefundResponse;
import com.ai.tutor.admin.model.entity.BrokerageOrder;
import com.ai.tutor.admin.model.entity.Message;
import com.ai.tutor.admin.model.entity.RefundRequestRecord;
import com.ai.tutor.admin.model.entity.User;
import com.ai.tutor.admin.model.vo.RefundRequestDetailResponse;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.admin.service.impl.AdminRefundRequestServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
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
    @Mock
    private UserMapper userMapper;

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

    @Test
    void detailShouldResolveStudentAndTeacherParticipants() {
        RefundRequestRecord record = new RefundRequestRecord();
        record.setId(10L);
        record.setBrokerageOrderId(99L);
        record.setRoomId(777L);
        record.setApplicantUid(2L);
        record.setApplicantRole("STUDENT");
        when(adminRefundRequestMapper.selectById(10L)).thenReturn(record);

        BrokerageOrder order = new BrokerageOrder();
        order.setId(99L);
        order.setPayerUid(2L);
        when(adminRefundMapper.selectById(99L)).thenReturn(order);

        Message message = new Message();
        message.setId(1L);
        message.setRoomId(777L);
        message.setFromUid(2L);
        message.setToUid(1L);
        when(adminMessageMapper.listByRoomId(777L)).thenReturn(List.of(message));

        User teacher = User.builder().id(1L).name("陈老师").userType(1).build();
        User student = User.builder().id(2L).name("李同学").userType(2).build();
        when(userMapper.selectByIds(anyList())).thenReturn(List.of(student, teacher));

        RefundRequestDetailResponse response = service.detail(10L);

        assertNotNull(response.getStudentParticipant());
        assertNotNull(response.getTeacherParticipant());
        assertEquals(2L, response.getStudentParticipant().getUid());
        assertEquals("李同学", response.getStudentParticipant().getName());
        assertEquals(1L, response.getTeacherParticipant().getUid());
        assertEquals("陈老师", response.getTeacherParticipant().getName());
    }
}
