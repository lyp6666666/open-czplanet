package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatRoomStartReq;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.service.impl.ChatRoomServiceImpl;
import com.ai.tutor.videocallimservice.common.domain.entity.ImUser;
import com.ai.tutor.videocallimservice.common.mapper.ImUserMapper;
import com.ai.tutor.videocallimservice.common.mapper.StudentProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.mapper.TeacherProfileLiteMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatRoomServiceImplStartChatTest {

    @Test
    void getOrCreateRoomShouldRejectSelfChatWithFriendlyMessage() {
        RoomMapper roomMapper = mock(RoomMapper.class);
        MessageMapper messageMapper = mock(MessageMapper.class);
        ChatService chatService = mock(ChatService.class);
        ImUserMapper imUserMapper = mock(ImUserMapper.class);
        TeacherProfileLiteMapper teacherProfileLiteMapper = mock(TeacherProfileLiteMapper.class);
        StudentProfileLiteMapper studentProfileLiteMapper = mock(StudentProfileLiteMapper.class);

        ChatRoomServiceImpl service = new ChatRoomServiceImpl();
        ReflectionTestUtils.setField(service, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(service, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(service, "chatService", chatService);
        ReflectionTestUtils.setField(service, "imUserMapper", imUserMapper);
        ReflectionTestUtils.setField(service, "teacherProfileLiteMapper", teacherProfileLiteMapper);
        ReflectionTestUtils.setField(service, "studentProfileLiteMapper", studentProfileLiteMapper);

        RequestInfo info = new RequestInfo();
        info.setUid(1L);
        ReflectionTestUtils.setField(info, "role", 1);
        RequestHolder.set(info);
        try {
            assertThatThrownBy(() -> service.getOrCreateRoomWithUser(1L, 1L))
                    .hasMessageContaining("不能与自己发起聊天");
        } finally {
            RequestHolder.remove();
        }
    }

    @Test
    void startChatShouldRejectRoleMismatchWithFriendlyMessage() {
        RoomMapper roomMapper = mock(RoomMapper.class);
        MessageMapper messageMapper = mock(MessageMapper.class);
        ChatService chatService = mock(ChatService.class);
        ImUserMapper imUserMapper = mock(ImUserMapper.class);
        TeacherProfileLiteMapper teacherProfileLiteMapper = mock(TeacherProfileLiteMapper.class);
        StudentProfileLiteMapper studentProfileLiteMapper = mock(StudentProfileLiteMapper.class);

        ImUser teacher = new ImUser();
        teacher.setId(1L);
        teacher.setUserType(1);
        teacher.setRefId(10L);
        teacher.setStatus(0);

        ImUser otherTeacher = new ImUser();
        otherTeacher.setId(2L);
        otherTeacher.setUserType(1);
        otherTeacher.setRefId(11L);
        otherTeacher.setStatus(0);

        when(imUserMapper.selectById(1L)).thenReturn(teacher);
        when(imUserMapper.selectById(2L)).thenReturn(otherTeacher);
        when(teacherProfileLiteMapper.selectIdByUserId(1L)).thenReturn(10L);
        when(studentProfileLiteMapper.selectIdByUserId(2L)).thenReturn(null);

        ChatRoomServiceImpl service = new ChatRoomServiceImpl();
        ReflectionTestUtils.setField(service, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(service, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(service, "chatService", chatService);
        ReflectionTestUtils.setField(service, "imUserMapper", imUserMapper);
        ReflectionTestUtils.setField(service, "teacherProfileLiteMapper", teacherProfileLiteMapper);
        ReflectionTestUtils.setField(service, "studentProfileLiteMapper", studentProfileLiteMapper);

        RequestInfo info = new RequestInfo();
        info.setUid(1L);
        ReflectionTestUtils.setField(info, "role", 1);
        RequestHolder.set(info);
        try {
            ChatRoomStartReq req = new ChatRoomStartReq();
            req.setTargetUid(2L);
            req.setGreeting("你好");

            assertThatThrownBy(() -> service.startChat(req, 1L))
                    .hasMessageContaining("对方不是学生账号");
        } finally {
            RequestHolder.remove();
        }
    }

    @Test
    void startChatShouldSendGreetingWhenFirstContact() {
        RoomMapper roomMapper = mock(RoomMapper.class);
        MessageMapper messageMapper = mock(MessageMapper.class);
        ChatService chatService = mock(ChatService.class);
        ImUserMapper imUserMapper = mock(ImUserMapper.class);
        TeacherProfileLiteMapper teacherProfileLiteMapper = mock(TeacherProfileLiteMapper.class);
        StudentProfileLiteMapper studentProfileLiteMapper = mock(StudentProfileLiteMapper.class);

        ImUser teacher = new ImUser();
        teacher.setId(1L);
        teacher.setUserType(1);
        teacher.setRefId(10L);
        teacher.setStatus(0);

        ImUser student = new ImUser();
        student.setId(2L);
        student.setUserType(2);
        student.setRefId(20L);
        student.setStatus(0);

        when(imUserMapper.selectById(1L)).thenReturn(teacher);
        when(imUserMapper.selectById(2L)).thenReturn(student);
        when(teacherProfileLiteMapper.selectIdByUserId(1L)).thenReturn(10L);
        when(studentProfileLiteMapper.selectIdByUserId(2L)).thenReturn(20L);

        when(roomMapper.selectByTeacherAndStudent(10L, 20L)).thenReturn(null);
        doAnswer(inv -> {
            Room r = inv.getArgument(0, Room.class);
            r.setId(100L);
            return 1;
        }).when(roomMapper).insert(any(Room.class));

        when(roomMapper.selectByIdForUpdate(100L)).thenReturn(Room.builder().id(100L).teacherProfileId(10L).studentProfileId(20L).status(1).lastMsgId(null).build());
        when(chatService.sendMsg(any(ChatMessageReq.class), eq(1L))).thenReturn(1000L);

        ChatRoomServiceImpl service = new ChatRoomServiceImpl();
        ReflectionTestUtils.setField(service, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(service, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(service, "chatService", chatService);
        ReflectionTestUtils.setField(service, "imUserMapper", imUserMapper);
        ReflectionTestUtils.setField(service, "teacherProfileLiteMapper", teacherProfileLiteMapper);
        ReflectionTestUtils.setField(service, "studentProfileLiteMapper", studentProfileLiteMapper);

        RequestInfo info = new RequestInfo();
        info.setUid(1L);
        ReflectionTestUtils.setField(info, "role", 1);
        RequestHolder.set(info);
        try {
            ChatRoomStartReq req = new ChatRoomStartReq();
            req.setTargetUid(2L);
            req.setGreeting("你好");

            Long roomId = service.startChat(req, 1L);
            assertThat(roomId).isEqualTo(100L);
            verify(chatService).sendMsg(any(ChatMessageReq.class), eq(1L));
        } finally {
            RequestHolder.remove();
        }
    }

    @Test
    void startChatShouldNotSendGreetingWhenRoomHasMessages() {
        RoomMapper roomMapper = mock(RoomMapper.class);
        MessageMapper messageMapper = mock(MessageMapper.class);
        ChatService chatService = mock(ChatService.class);
        ImUserMapper imUserMapper = mock(ImUserMapper.class);
        TeacherProfileLiteMapper teacherProfileLiteMapper = mock(TeacherProfileLiteMapper.class);
        StudentProfileLiteMapper studentProfileLiteMapper = mock(StudentProfileLiteMapper.class);

        ImUser teacher = new ImUser();
        teacher.setId(1L);
        teacher.setUserType(1);
        teacher.setRefId(10L);
        teacher.setStatus(0);

        ImUser student = new ImUser();
        student.setId(2L);
        student.setUserType(2);
        student.setRefId(20L);
        student.setStatus(0);

        when(imUserMapper.selectById(1L)).thenReturn(teacher);
        when(imUserMapper.selectById(2L)).thenReturn(student);
        when(teacherProfileLiteMapper.selectIdByUserId(1L)).thenReturn(10L);
        when(studentProfileLiteMapper.selectIdByUserId(2L)).thenReturn(20L);

        when(roomMapper.selectByTeacherAndStudent(10L, 20L)).thenReturn(Room.builder().id(100L).teacherProfileId(10L).studentProfileId(20L).status(1).lastMsgId(999L).build());
        when(roomMapper.selectByIdForUpdate(100L)).thenReturn(Room.builder().id(100L).teacherProfileId(10L).studentProfileId(20L).status(1).lastMsgId(999L).build());

        ChatRoomServiceImpl service = new ChatRoomServiceImpl();
        ReflectionTestUtils.setField(service, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(service, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(service, "chatService", chatService);
        ReflectionTestUtils.setField(service, "imUserMapper", imUserMapper);
        ReflectionTestUtils.setField(service, "teacherProfileLiteMapper", teacherProfileLiteMapper);
        ReflectionTestUtils.setField(service, "studentProfileLiteMapper", studentProfileLiteMapper);

        RequestInfo info = new RequestInfo();
        info.setUid(1L);
        ReflectionTestUtils.setField(info, "role", 1);
        RequestHolder.set(info);
        try {
            ChatRoomStartReq req = new ChatRoomStartReq();
            req.setTargetUid(2L);
            req.setGreeting("你好");

            Long roomId = service.startChat(req, 1L);
            assertThat(roomId).isEqualTo(100L);
            verify(chatService, never()).sendMsg(any(ChatMessageReq.class), anyLong());
        } finally {
            RequestHolder.remove();
        }
    }
}
