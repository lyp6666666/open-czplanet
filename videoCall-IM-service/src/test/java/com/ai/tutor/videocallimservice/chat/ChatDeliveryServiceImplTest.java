package com.ai.tutor.videocallimservice.chat;

import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatDeliveryAckReq;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.service.impl.ChatDeliveryServiceImpl;
import com.ai.tutor.videocallimservice.chat.service.stream.SseSessionManager;
import com.ai.tutor.videocallimservice.common.domain.entity.ImUser;
import com.ai.tutor.videocallimservice.common.mapper.ImUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatDeliveryServiceImplTest {

    @Test
    void ackDeliveredShouldPushDeliveryEventToPeer() {
        RoomMapper roomMapper = mock(RoomMapper.class);
        MessageMapper messageMapper = mock(MessageMapper.class);
        ImUserMapper imUserMapper = mock(ImUserMapper.class);
        SseSessionManager sseSessionManager = mock(SseSessionManager.class);

        Room room = Room.builder().id(10L).status(1).teacherProfileId(100L).studentProfileId(200L).build();
        when(roomMapper.selectById(10L)).thenReturn(room);

        ImUser teacher = new ImUser();
        teacher.setId(1L);
        teacher.setUserType(1);
        teacher.setRefId(100L);
        teacher.setStatus(0);
        when(imUserMapper.selectByUserTypeAndRefId(1, 100L)).thenReturn(teacher);

        ImUser student = new ImUser();
        student.setId(2L);
        student.setUserType(2);
        student.setRefId(200L);
        student.setStatus(0);
        when(imUserMapper.selectByUserTypeAndRefId(2, 200L)).thenReturn(student);

        Message msg = Message.builder().id(999L).roomId(10L).fromUid(1L).toUid(2L).status(0).build();
        when(messageMapper.getById(999L)).thenReturn(msg);

        ChatDeliveryServiceImpl service = new ChatDeliveryServiceImpl();
        ReflectionTestUtils.setField(service, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(service, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(service, "imUserMapper", imUserMapper);
        ReflectionTestUtils.setField(service, "sseSessionManager", sseSessionManager);

        ChatDeliveryAckReq req = new ChatDeliveryAckReq();
        req.setRoomId(10L);
        req.setLastDeliveredMsgId(999L);

        service.ackDelivered(req, 2L);

        verify(sseSessionManager).sendToUid(eq(1L), eq("delivery"), any());
    }

    @Test
    void ackDeliveredShouldRejectWhenCurrentUserIsNotMessageReceiver() {
        RoomMapper roomMapper = mock(RoomMapper.class);
        MessageMapper messageMapper = mock(MessageMapper.class);
        ImUserMapper imUserMapper = mock(ImUserMapper.class);
        SseSessionManager sseSessionManager = mock(SseSessionManager.class);

        Room room = Room.builder().id(10L).status(1).teacherProfileId(100L).studentProfileId(200L).build();
        when(roomMapper.selectById(10L)).thenReturn(room);

        ImUser teacher = new ImUser();
        teacher.setId(1L);
        teacher.setUserType(1);
        teacher.setRefId(100L);
        teacher.setStatus(0);
        when(imUserMapper.selectByUserTypeAndRefId(1, 100L)).thenReturn(teacher);

        ImUser student = new ImUser();
        student.setId(2L);
        student.setUserType(2);
        student.setRefId(200L);
        student.setStatus(0);
        when(imUserMapper.selectByUserTypeAndRefId(2, 200L)).thenReturn(student);

        Message msg = Message.builder().id(999L).roomId(10L).fromUid(1L).toUid(2L).status(0).build();
        when(messageMapper.getById(999L)).thenReturn(msg);

        ChatDeliveryServiceImpl service = new ChatDeliveryServiceImpl();
        ReflectionTestUtils.setField(service, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(service, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(service, "imUserMapper", imUserMapper);
        ReflectionTestUtils.setField(service, "sseSessionManager", sseSessionManager);

        ChatDeliveryAckReq req = new ChatDeliveryAckReq();
        req.setRoomId(10L);
        req.setLastDeliveredMsgId(999L);

        assertThrows(BusinessException.class, () -> service.ackDelivered(req, 1L));
    }
}
