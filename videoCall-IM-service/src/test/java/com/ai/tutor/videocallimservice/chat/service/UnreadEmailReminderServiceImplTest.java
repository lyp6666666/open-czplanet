package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.videocallimservice.chat.domain.entity.EmailNotificationTask;
import com.ai.tutor.videocallimservice.chat.domain.entity.EmailSendLog;
import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.entity.RoomReadState;
import com.ai.tutor.videocallimservice.chat.mapper.EmailNotificationTaskMapper;
import com.ai.tutor.videocallimservice.chat.mapper.EmailSendLogMapper;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomReadStateMapper;
import com.ai.tutor.videocallimservice.chat.service.impl.UnreadEmailReminderServiceImpl;
import com.ai.tutor.videocallimservice.chat.service.impl.UnreadEmailSender;
import com.ai.tutor.videocallimservice.integration.dto.InternalUserEmailsVO;
import com.ai.tutor.videocallimservice.integration.feign.AppointmentInternalFeignClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnreadEmailReminderServiceImplTest {

    @Test
    void processOneShouldUseUnreadEmailSenderAndMarkSent() {
        MessageMapper messageMapper = mock(MessageMapper.class);
        RoomMapper roomMapper = mock(RoomMapper.class);
        RoomReadStateMapper roomReadStateMapper = mock(RoomReadStateMapper.class);
        EmailNotificationTaskMapper taskMapper = mock(EmailNotificationTaskMapper.class);
        EmailSendLogMapper sendLogMapper = mock(EmailSendLogMapper.class);
        AppointmentInternalFeignClient appointmentInternalFeignClient = mock(AppointmentInternalFeignClient.class);
        UnreadEmailSender unreadEmailSender = mock(UnreadEmailSender.class);

        UnreadEmailReminderServiceImpl service = new UnreadEmailReminderServiceImpl();
        ReflectionTestUtils.setField(service, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(service, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(service, "roomReadStateMapper", roomReadStateMapper);
        ReflectionTestUtils.setField(service, "taskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "sendLogMapper", sendLogMapper);
        ReflectionTestUtils.setField(service, "appointmentInternalFeignClient", appointmentInternalFeignClient);
        ReflectionTestUtils.setField(service, "unreadEmailSender", unreadEmailSender);
        ReflectionTestUtils.setField(service, "unreadRoomDailyLimit", 1);
        ReflectionTestUtils.setField(service, "unreadUserDailyLimit", 3);

        EmailNotificationTask task = EmailNotificationTask.builder()
                .id(1L)
                .bizId(9L)
                .email("user@example.com")
                .payloadJson("{\"receiverName\":\"用户\",\"senderName\":\"站内联系人\",\"senderRole\":\"用户\",\"messageSummary\":\"你收到一条普通消息\"}")
                .build();
        Message message = Message.builder()
                .id(9L)
                .roomId(11L)
                .toUid(1001L)
                .fromUid(2001L)
                .status(0)
                .type(1)
                .createTime(LocalDateTime.now().minusHours(3))
                .build();

        when(taskMapper.updateStatusIfCurrent(1L, "PENDING", "VALIDATING")).thenReturn(1);
        when(messageMapper.getById(9L)).thenReturn(message);
        when(roomMapper.selectById(11L)).thenReturn(Room.builder().id(11L).status(1).build());
        when(roomReadStateMapper.getByRoomAndUid(11L, 1001L)).thenReturn(RoomReadState.builder().roomId(11L).uid(1001L).lastReadMsgId(1L).build());
        when(taskMapper.countSentSince(org.mockito.ArgumentMatchers.eq(1001L), org.mockito.ArgumentMatchers.eq("UNREAD_MESSAGE_REMINDER"),
                org.mockito.ArgumentMatchers.eq("UNREAD_MESSAGE"), org.mockito.ArgumentMatchers.eq(9L), any(LocalDateTime.class))).thenReturn(0);
        when(taskMapper.countSentSince(org.mockito.ArgumentMatchers.eq(1001L), org.mockito.ArgumentMatchers.eq("UNREAD_MESSAGE_REMINDER"),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), any(LocalDateTime.class))).thenReturn(0);
        InternalUserEmailsVO.EmailValue emailValue = new InternalUserEmailsVO.EmailValue();
        emailValue.setEmail("user@example.com");
        emailValue.setVerified(true);
        emailValue.setBounceStatus("NORMAL");
        InternalUserEmailsVO emailsVO = new InternalUserEmailsVO();
        emailsVO.setPrimaryEmail(emailValue);
        when(appointmentInternalFeignClient.getUserEmailsById(1001L)).thenReturn(new BaseResponse<>(0, emailsVO, ""));
        when(unreadEmailSender.sendUnreadReminder(any(), any(), any(), any())).thenReturn(
                new UnreadEmailSender.SendResult(true, "TENCENT", "msg-im-1", "req-im-1", null, null)
        );

        service.processOne(task);

        verify(taskMapper).markSent(org.mockito.ArgumentMatchers.eq(1L), any(LocalDateTime.class), org.mockito.ArgumentMatchers.eq("未读消息待查看"));
        ArgumentCaptor<EmailSendLog> logCaptor = ArgumentCaptor.forClass(EmailSendLog.class);
        verify(sendLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getProvider()).isEqualTo("TENCENT");
        assertThat(logCaptor.getValue().getProviderMessageId()).isEqualTo("msg-im-1");
    }
}
