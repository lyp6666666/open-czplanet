package com.ai.tutor.videocallimservice.chat;

import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.entity.RoomReadState;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatReadAckReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatReadAckResp;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomReadStateMapper;
import com.ai.tutor.videocallimservice.chat.service.impl.ChatReadServiceImpl;
import com.ai.tutor.videocallimservice.chat.service.stream.SseSessionManager;
import com.ai.tutor.videocallimservice.common.domain.entity.ImUser;
import com.ai.tutor.videocallimservice.common.mapper.ImUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;

class ChatReadServiceImplTest {

    @Test
    void ackReadShouldUpsertReadState() {
        RoomMapper roomMapper = mock(RoomMapper.class);
        MessageMapper messageMapper = mock(MessageMapper.class);
        RoomReadStateMapper roomReadStateMapper = mock(RoomReadStateMapper.class);
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

        Message msg = Message.builder().id(999L).roomId(10L).status(0).build();
        when(messageMapper.getById(999L)).thenReturn(msg);

        ChatReadServiceImpl svc = new ChatReadServiceImpl();
        ReflectionTestUtils.setField(svc, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(svc, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(svc, "roomReadStateMapper", roomReadStateMapper);
        ReflectionTestUtils.setField(svc, "imUserMapper", imUserMapper);
        ReflectionTestUtils.setField(svc, "sseSessionManager", sseSessionManager);

        ChatReadAckReq req = new ChatReadAckReq();
        req.setRoomId(10L);
        req.setLastReadMsgId(999L);

        ChatReadAckResp resp = svc.ackRead(req, 2L);

        verify(roomReadStateMapper).upsertReadState(10L, 2L, 999L);
        verify(sseSessionManager).sendToUid(eq(1L), eq("read"), any());
        assertEquals(999L, resp.getLastReadMsgId());
    }

    @Test
    void ackReadShouldFailWhenUpsertFails() {
        RoomMapper roomMapper = mock(RoomMapper.class);
        MessageMapper messageMapper = mock(MessageMapper.class);
        RoomReadStateMapper roomReadStateMapper = mock(RoomReadStateMapper.class);
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

        Message msg = Message.builder().id(999L).roomId(10L).status(0).build();
        when(messageMapper.getById(999L)).thenReturn(msg);

        doThrow(new RuntimeException("db down")).when(roomReadStateMapper).upsertReadState(10L, 2L, 999L);

        ChatReadServiceImpl svc = new ChatReadServiceImpl();
        ReflectionTestUtils.setField(svc, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(svc, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(svc, "roomReadStateMapper", roomReadStateMapper);
        ReflectionTestUtils.setField(svc, "imUserMapper", imUserMapper);
        ReflectionTestUtils.setField(svc, "sseSessionManager", sseSessionManager);

        ChatReadAckReq req = new ChatReadAckReq();
        req.setRoomId(10L);
        req.setLastReadMsgId(999L);

        assertThrows(BusinessException.class, () -> svc.ackRead(req, 2L));
    }

    @Test
    void ackReadShouldNotPushReceiptWhenWatermarkDoesNotAdvance() {
        RoomMapper roomMapper = mock(RoomMapper.class);
        MessageMapper messageMapper = mock(MessageMapper.class);
        RoomReadStateMapper roomReadStateMapper = mock(RoomReadStateMapper.class);
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

        Message msg = Message.builder().id(999L).roomId(10L).status(0).build();
        when(messageMapper.getById(999L)).thenReturn(msg);
        when(roomReadStateMapper.getByRoomAndUid(10L, 2L))
                .thenReturn(RoomReadState.builder().roomId(10L).uid(2L).lastReadMsgId(1000L).build());

        ChatReadServiceImpl svc = new ChatReadServiceImpl();
        ReflectionTestUtils.setField(svc, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(svc, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(svc, "roomReadStateMapper", roomReadStateMapper);
        ReflectionTestUtils.setField(svc, "imUserMapper", imUserMapper);
        ReflectionTestUtils.setField(svc, "sseSessionManager", sseSessionManager);

        ChatReadAckReq req = new ChatReadAckReq();
        req.setRoomId(10L);
        req.setLastReadMsgId(999L);

        ChatReadAckResp resp = svc.ackRead(req, 2L);

        verify(roomReadStateMapper).upsertReadState(10L, 2L, 1000L);
        verify(sseSessionManager, never()).sendToUid(anyLong(), any(), any());
        assertEquals(1000L, resp.getLastReadMsgId());
    }
}
