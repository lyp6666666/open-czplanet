package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.videocallimservice.chat.domain.entity.ApplicationBrokerageOrder;
import com.ai.tutor.videocallimservice.chat.domain.entity.BrokerageOrder;
import com.ai.tutor.videocallimservice.chat.domain.entity.StudentJobPostingLite;
import com.ai.tutor.videocallimservice.chat.domain.entity.TutorApplication;
import com.ai.tutor.videocallimservice.chat.domain.enums.BrokerageOrderStatus;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.CreateTutorApplicationReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.DecideTutorApplicationReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.SystemMsgReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatMessageResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.TutorApplicationVO;
import com.ai.tutor.videocallimservice.chat.mapper.ApplicationBrokerageOrderMapper;
import com.ai.tutor.videocallimservice.chat.mapper.BrokerageOrderMapper;
import com.ai.tutor.videocallimservice.chat.mapper.StudentJobPostingLiteMapper;
import com.ai.tutor.videocallimservice.chat.mapper.TutorApplicationMapper;
import com.ai.tutor.videocallimservice.chat.service.stream.SseSessionManager;
import com.ai.tutor.videocallimservice.common.mapper.StudentProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.mapper.TeacherProfileLiteMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TutorApplicationServiceTest {

    @Test
    void createAndSendToChatShouldCarryTeachingModeInTutorApplicationCard() {
        TutorApplicationMapper tutorApplicationMapper = mock(TutorApplicationMapper.class);
        BrokerageOrderMapper brokerageOrderMapper = mock(BrokerageOrderMapper.class);
        ApplicationBrokerageOrderMapper applicationBrokerageOrderMapper = mock(ApplicationBrokerageOrderMapper.class);
        ChatRoomService chatRoomService = mock(ChatRoomService.class);
        ChatService chatService = mock(ChatService.class);
        SseSessionManager sseSessionManager = mock(SseSessionManager.class);
        StudentJobPostingLiteMapper studentJobPostingLiteMapper = mock(StudentJobPostingLiteMapper.class);
        TeacherProfileLiteMapper teacherProfileLiteMapper = mock(TeacherProfileLiteMapper.class);
        StudentProfileLiteMapper studentProfileLiteMapper = mock(StudentProfileLiteMapper.class);

        when(chatRoomService.getOrCreateRoomWithUser(3001L, 2001L)).thenReturn(66L);
        when(teacherProfileLiteMapper.selectIdByUserId(3001L)).thenReturn(88L);
        when(tutorApplicationMapper.selectBySenderAndClientRequestId(eq(2001L), any())).thenReturn(null);
        when(tutorApplicationMapper.selectLatestBySenderReceiverContext(2001L, 3001L, "TUTOR", 88L)).thenReturn(null);
        when(tutorApplicationMapper.countCreatedBySenderBetween(eq(2001L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
        when(chatService.sendMsg(any(ChatMessageReq.class), eq(2001L))).thenReturn(7001L);

        ChatMessageResp.UserInfo fromUser = new ChatMessageResp.UserInfo();
        fromUser.setUid(2001L);
        ChatMessageResp.Message message = new ChatMessageResp.Message();
        message.setId(7001L);
        message.setRoomId(66L);
        when(chatService.getMsgResp(7001L, 2001L)).thenReturn(ChatMessageResp.builder()
                .fromUser(fromUser)
                .message(message)
                .build());

        doAnswer(invocation -> {
            TutorApplication application = invocation.getArgument(0);
            application.setId(9528L);
            return 1;
        }).when(tutorApplicationMapper).insert(any(TutorApplication.class));

        TutorApplicationService service = new TutorApplicationService();
        ReflectionTestUtils.setField(service, "tutorApplicationMapper", tutorApplicationMapper);
        ReflectionTestUtils.setField(service, "brokerageOrderMapper", brokerageOrderMapper);
        ReflectionTestUtils.setField(service, "applicationBrokerageOrderMapper", applicationBrokerageOrderMapper);
        ReflectionTestUtils.setField(service, "chatRoomService", chatRoomService);
        ReflectionTestUtils.setField(service, "chatService", chatService);
        ReflectionTestUtils.setField(service, "sseSessionManager", sseSessionManager);
        ReflectionTestUtils.setField(service, "studentJobPostingLiteMapper", studentJobPostingLiteMapper);
        ReflectionTestUtils.setField(service, "teacherProfileLiteMapper", teacherProfileLiteMapper);
        ReflectionTestUtils.setField(service, "studentProfileLiteMapper", studentProfileLiteMapper);
        ReflectionTestUtils.setField(service, "defaultAmountFen", 19900L);

        RequestInfo info = new RequestInfo();
        info.setUid(2001L);
        ReflectionTestUtils.setField(info, "role", 2);
        RequestHolder.set(info);
        try {
            CreateTutorApplicationReq req = new CreateTutorApplicationReq();
            req.setReceiverUid(3001L);
            req.setContextType("TUTOR");
            req.setContextId(88L);
            req.setContent("您好，想先约一节试听课");
            req.setTeachingMode("ONLINE");
            req.setClientRequestId("cid-online-1");

            service.createAndSendToChat(req, 2001L);
        } finally {
            RequestHolder.remove();
        }

        ArgumentCaptor<ChatMessageReq> reqCaptor = ArgumentCaptor.forClass(ChatMessageReq.class);
        verify(chatService).sendMsg(reqCaptor.capture(), eq(2001L));
        SystemMsgReq body = (SystemMsgReq) reqCaptor.getValue().getBody();
        assertThat(body.getBizType()).isEqualTo("TUTOR_APPLICATION");
        assertThat(body.getTeachingMode()).isEqualTo("ONLINE");
    }

    @Test
    void createShouldInheritTeachingModeFromDemandForTeacherApply() {
        TutorApplicationMapper tutorApplicationMapper = mock(TutorApplicationMapper.class);
        BrokerageOrderMapper brokerageOrderMapper = mock(BrokerageOrderMapper.class);
        ApplicationBrokerageOrderMapper applicationBrokerageOrderMapper = mock(ApplicationBrokerageOrderMapper.class);
        ChatRoomService chatRoomService = mock(ChatRoomService.class);
        ChatService chatService = mock(ChatService.class);
        SseSessionManager sseSessionManager = mock(SseSessionManager.class);
        StudentJobPostingLiteMapper studentJobPostingLiteMapper = mock(StudentJobPostingLiteMapper.class);
        TeacherProfileLiteMapper teacherProfileLiteMapper = mock(TeacherProfileLiteMapper.class);
        StudentProfileLiteMapper studentProfileLiteMapper = mock(StudentProfileLiteMapper.class);

        StudentJobPostingLite demand = new StudentJobPostingLite();
        demand.setId(501L);
        demand.setClassMode("online");
        when(studentJobPostingLiteMapper.selectById(501L)).thenReturn(demand);
        when(studentProfileLiteMapper.selectIdByUserId(2001L)).thenReturn(66L);
        when(tutorApplicationMapper.selectBySenderAndClientRequestId(eq(1001L), any())).thenReturn(null);
        when(tutorApplicationMapper.selectLatestBySenderReceiverContext(1001L, 2001L, "DEMAND", 501L)).thenReturn(null);
        when(tutorApplicationMapper.countCreatedBySenderBetween(eq(1001L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
        doAnswer(invocation -> {
            TutorApplication application = invocation.getArgument(0);
            application.setId(9901L);
            return 1;
        }).when(tutorApplicationMapper).insert(any(TutorApplication.class));

        TutorApplicationService service = new TutorApplicationService();
        ReflectionTestUtils.setField(service, "tutorApplicationMapper", tutorApplicationMapper);
        ReflectionTestUtils.setField(service, "brokerageOrderMapper", brokerageOrderMapper);
        ReflectionTestUtils.setField(service, "applicationBrokerageOrderMapper", applicationBrokerageOrderMapper);
        ReflectionTestUtils.setField(service, "chatRoomService", chatRoomService);
        ReflectionTestUtils.setField(service, "chatService", chatService);
        ReflectionTestUtils.setField(service, "sseSessionManager", sseSessionManager);
        ReflectionTestUtils.setField(service, "studentJobPostingLiteMapper", studentJobPostingLiteMapper);
        ReflectionTestUtils.setField(service, "teacherProfileLiteMapper", teacherProfileLiteMapper);
        ReflectionTestUtils.setField(service, "studentProfileLiteMapper", studentProfileLiteMapper);

        RequestInfo info = new RequestInfo();
        info.setUid(1001L);
        ReflectionTestUtils.setField(info, "role", 1);
        RequestHolder.set(info);
        try {
            CreateTutorApplicationReq req = new CreateTutorApplicationReq();
            req.setReceiverUid(2001L);
            req.setContextType("DEMAND");
            req.setContextId(501L);
            req.setContent("老师看到您的需求，想进一步沟通");
            req.setClientRequestId("cid-demand-1");

            TutorApplicationVO vo = service.create(req, 1001L);
            assertThat(vo.getTeachingMode()).isEqualTo("ONLINE");
        } finally {
            RequestHolder.remove();
        }
    }

    @Test
    void decideAcceptedAgainShouldRecreateMissingBrokerageOrder() {
        TutorApplicationMapper tutorApplicationMapper = mock(TutorApplicationMapper.class);
        BrokerageOrderMapper brokerageOrderMapper = mock(BrokerageOrderMapper.class);
        ApplicationBrokerageOrderMapper applicationBrokerageOrderMapper = mock(ApplicationBrokerageOrderMapper.class);

        TutorApplication accepted = TutorApplication.builder()
                .id(9527L)
                .senderUid(3001L)
                .receiverUid(2001L)
                .senderRole("STUDENT")
                .receiverRole("TEACHER")
                .contextType("TUTOR")
                .contextId(88L)
                .status("ACCEPTED")
                .chatAccessStatus("PAYMENT_REQUIRED")
                .roomId(66L)
                .build();

        when(tutorApplicationMapper.selectById(9527L)).thenReturn(accepted);
        when(tutorApplicationMapper.decide(eq(9527L), eq(2001L), eq("ACCEPTED"), eq("PAYMENT_REQUIRED"), any(LocalDateTime.class))).thenReturn(0);
        when(applicationBrokerageOrderMapper.selectByApplicationId(9527L)).thenReturn(null);
        doAnswer(invocation -> {
            BrokerageOrder order = invocation.getArgument(0);
            order.setId(9001L);
            return null;
        }).when(brokerageOrderMapper).insert(any(BrokerageOrder.class));

        TutorApplicationService service = new TutorApplicationService();
        ReflectionTestUtils.setField(service, "tutorApplicationMapper", tutorApplicationMapper);
        ReflectionTestUtils.setField(service, "brokerageOrderMapper", brokerageOrderMapper);
        ReflectionTestUtils.setField(service, "applicationBrokerageOrderMapper", applicationBrokerageOrderMapper);
        ReflectionTestUtils.setField(service, "defaultAmountFen", 19900L);

        TutorApplicationVO vo = service.decide(9527L, acceptReq(), 2001L);

        assertThat(vo.getOrderId()).isEqualTo(9001L);
        verify(brokerageOrderMapper).insert(any(BrokerageOrder.class));
        verify(applicationBrokerageOrderMapper).insert(any(ApplicationBrokerageOrder.class));
    }

    @Test
    void decideMessageAcceptedShouldSendBrokerageRequiredCardWhenRecoveredFromHalfSuccess() {
        TutorApplicationMapper tutorApplicationMapper = mock(TutorApplicationMapper.class);
        BrokerageOrderMapper brokerageOrderMapper = mock(BrokerageOrderMapper.class);
        ApplicationBrokerageOrderMapper applicationBrokerageOrderMapper = mock(ApplicationBrokerageOrderMapper.class);
        ChatRoomService chatRoomService = mock(ChatRoomService.class);
        ChatService chatService = mock(ChatService.class);
        SseSessionManager sseSessionManager = mock(SseSessionManager.class);
        StudentJobPostingLiteMapper studentJobPostingLiteMapper = mock(StudentJobPostingLiteMapper.class);

        TutorApplication accepted = TutorApplication.builder()
                .id(9527L)
                .senderUid(3001L)
                .receiverUid(2001L)
                .senderRole("STUDENT")
                .receiverRole("TEACHER")
                .contextType("TUTOR")
                .contextId(88L)
                .status("ACCEPTED")
                .chatAccessStatus("PAYMENT_REQUIRED")
                .roomId(66L)
                .build();

        when(tutorApplicationMapper.selectById(9527L)).thenReturn(accepted);
        when(tutorApplicationMapper.decide(eq(9527L), eq(2001L), eq("ACCEPTED"), eq("PAYMENT_REQUIRED"), any(LocalDateTime.class))).thenReturn(0);
        when(applicationBrokerageOrderMapper.selectByApplicationId(9527L)).thenReturn(null);
        doAnswer(invocation -> {
            BrokerageOrder order = invocation.getArgument(0);
            order.setId(9001L);
            return null;
        }).when(brokerageOrderMapper).insert(any(BrokerageOrder.class));
        when(brokerageOrderMapper.selectById(9001L)).thenReturn(BrokerageOrder.builder()
                .id(9001L)
                .applicationId(9527L)
                .payerUid(2001L)
                .amountFen(19900L)
                .status(BrokerageOrderStatus.PENDING.name())
                .build());
        when(chatService.sendMsg(any(), eq(2001L))).thenReturn(7001L, 7002L);
        ChatMessageResp.UserInfo fromUser = new ChatMessageResp.UserInfo();
        fromUser.setUid(2001L);
        ChatMessageResp.Message message = new ChatMessageResp.Message();
        message.setId(7001L);
        message.setRoomId(66L);
        when(chatService.getMsgResp(7001L, 2001L)).thenReturn(ChatMessageResp.builder()
                .fromUser(fromUser)
                .message(message)
                .build());

        TutorApplicationService service = new TutorApplicationService();
        ReflectionTestUtils.setField(service, "tutorApplicationMapper", tutorApplicationMapper);
        ReflectionTestUtils.setField(service, "brokerageOrderMapper", brokerageOrderMapper);
        ReflectionTestUtils.setField(service, "applicationBrokerageOrderMapper", applicationBrokerageOrderMapper);
        ReflectionTestUtils.setField(service, "chatRoomService", chatRoomService);
        ReflectionTestUtils.setField(service, "chatService", chatService);
        ReflectionTestUtils.setField(service, "sseSessionManager", sseSessionManager);
        ReflectionTestUtils.setField(service, "studentJobPostingLiteMapper", studentJobPostingLiteMapper);
        ReflectionTestUtils.setField(service, "defaultAmountFen", 19900L);

        service.decideAndSendToChat(9527L, acceptReq(), 2001L);

        ArgumentCaptor<com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq> reqCaptor =
                ArgumentCaptor.forClass(com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq.class);
        verify(chatService, times(2)).sendMsg(reqCaptor.capture(), eq(2001L));
        SystemMsgReq firstBody = (SystemMsgReq) reqCaptor.getAllValues().get(0).getBody();
        SystemMsgReq secondBody = (SystemMsgReq) reqCaptor.getAllValues().get(1).getBody();
        assertThat(firstBody.getBizType()).isEqualTo("TUTOR_APPLICATION_STATUS");
        assertThat(secondBody.getBizType()).isEqualTo("BROKERAGE_REQUIRED");
        assertThat(secondBody.getEventId()).isEqualTo(9001L);
        assertThat(secondBody.getAmountFen()).isEqualTo(19900L);
        verify(chatRoomService, never()).getOrCreateRoomWithUser(any(), any());
    }

    private static DecideTutorApplicationReq acceptReq() {
        DecideTutorApplicationReq req = new DecideTutorApplicationReq();
        req.setAction("ACCEPT");
        return req;
    }
}
