package com.ai.tutor.videocallimservice.integration;

import com.ai.tutor.common.integration.BrokerageOrderPayInfo;
import com.ai.tutor.common.event.PaymentSuccessEvent;
import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.videocallimservice.chat.service.BrokerageOrderService;
import com.ai.tutor.videocallimservice.chat.service.ChatRoomService;
import com.ai.tutor.videocallimservice.chat.service.ChatService;
import com.ai.tutor.videocallimservice.chat.service.CourseEnrollmentService;
import com.ai.tutor.videocallimservice.chat.service.TutorApplicationService;
import com.ai.tutor.videocallimservice.integration.controller.InternalFacadeController;
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
class InternalFacadeControllerTest {

    @Mock
    private ChatRoomService chatRoomService;
    @Mock
    private ChatService chatService;
    @Mock
    private BrokerageOrderService brokerageOrderService;
    @Mock
    private TutorApplicationService tutorApplicationService;
    @Mock
    private CourseEnrollmentService courseEnrollmentService;

    @InjectMocks
    private InternalFacadeController internalFacadeController;

    @AfterEach
    void tearDown() {
        RequestHolder.remove();
    }

    @Test
    void getPayableOrderShouldAllowInternalCallWithoutRequestContext() {
        BrokerageOrderPayInfo payInfo = new BrokerageOrderPayInfo();
        payInfo.setOrderId(666014L);
        payInfo.setPayerUid(667079L);
        when(brokerageOrderService.getPayableOrder(666014L, 667079L)).thenReturn(payInfo);

        BrokerageOrderPayInfo response = internalFacadeController.getPayableOrder(666014L, 667079L).getData();

        assertThat(response).isSameAs(payInfo);
        verify(brokerageOrderService).getPayableOrder(666014L, 667079L);
    }

    @Test
    void getPayableOrderShouldRejectMismatchedRequestContextUid() {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setUid(1L);
        RequestHolder.set(requestInfo);

        assertThatThrownBy(() -> internalFacadeController.getPayableOrder(666014L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.NO_AUTH_ERROR.getCode());
    }

    @Test
    void onPaymentSuccessShouldAllowInternalSignedIdentityUidZero() {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setUid(0L);
        requestInfo.setRole(0);
        RequestHolder.set(requestInfo);

        PaymentSuccessEvent event = new PaymentSuccessEvent();
        event.setContextType("BROKERAGE_ORDER");
        event.setContextId(666018L);

        Boolean result = internalFacadeController.onPaymentSuccess(event).getData();

        assertThat(result).isTrue();
        verify(brokerageOrderService).onPaymentSuccess(666018L, null, null);
    }
}
