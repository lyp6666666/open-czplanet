package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.mapper.CourseEnrollmentMapper;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.mapper.TutorApplicationMapper;
import com.ai.tutor.videocallimservice.chat.service.impl.ChatServiceImpl;
import com.ai.tutor.videocallimservice.chat.service.strategy.msg.SystemMsgHandler;
import com.ai.tutor.videocallimservice.chat.domain.entity.TutorApplication;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.videocallimservice.common.domain.entity.ImUser;
import com.ai.tutor.videocallimservice.common.mapper.ImUserMapper;
import com.ai.tutor.videocallimservice.common.mapper.StudentProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.mapper.TeacherProfileLiteMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceImplLessonSystemMessageTest {

    @Test
    void shouldRejectLessonRequestSystemMessageBeforeChatUnlocked() {
        RoomMapper roomMapper = mock(RoomMapper.class);
        MessageMapper messageMapper = mock(MessageMapper.class);
        TutorApplicationMapper tutorApplicationMapper = mock(TutorApplicationMapper.class);
        CourseEnrollmentMapper courseEnrollmentMapper = mock(CourseEnrollmentMapper.class);
        TeacherProfileLiteMapper teacherProfileLiteMapper = mock(TeacherProfileLiteMapper.class);
        StudentProfileLiteMapper studentProfileLiteMapper = mock(StudentProfileLiteMapper.class);
        ImUserMapper imUserMapper = mock(ImUserMapper.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

        Room room = Room.builder().id(10L).teacherProfileId(100L).studentProfileId(200L).status(1).build();
        when(roomMapper.selectById(10L)).thenReturn(room);
        when(teacherProfileLiteMapper.selectUserIdById(100L)).thenReturn(1L);
        when(studentProfileLiteMapper.selectUserIdById(200L)).thenReturn(2L);
        when(tutorApplicationMapper.selectLatestAcceptedBetween(1L, 2L))
                .thenReturn(TutorApplication.builder().id(99L).chatAccessStatus("PAYMENT_REQUIRED").build());

        ImUser teacher = new ImUser();
        teacher.setId(1L);
        teacher.setRefId(100L);
        teacher.setStatus(0);
        when(imUserMapper.selectById(1L)).thenReturn(teacher);

        ImUser student = new ImUser();
        student.setId(2L);
        student.setRefId(200L);
        student.setStatus(0);
        when(imUserMapper.selectById(2L)).thenReturn(student);

        doAnswer(inv -> {
            Message m = inv.getArgument(0, Message.class);
            m.setId(321L);
            return 1;
        }).when(messageMapper).save(any(Message.class));

        SystemMsgHandler systemMsgHandler = new SystemMsgHandler();
        ReflectionTestUtils.setField(systemMsgHandler, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(systemMsgHandler, "imUserMapper", imUserMapper);
        ReflectionTestUtils.setField(systemMsgHandler, "teacherProfileLiteMapper", teacherProfileLiteMapper);
        ReflectionTestUtils.setField(systemMsgHandler, "studentProfileLiteMapper", studentProfileLiteMapper);
        ReflectionTestUtils.setField(systemMsgHandler, "messageMapper", messageMapper);
        ReflectionTestUtils.invokeMethod(systemMsgHandler, "init");

        ChatServiceImpl service = new ChatServiceImpl();
        ReflectionTestUtils.setField(service, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(service, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(service, "tutorApplicationMapper", tutorApplicationMapper);
        ReflectionTestUtils.setField(service, "courseEnrollmentMapper", courseEnrollmentMapper);
        ReflectionTestUtils.setField(service, "teacherProfileLiteMapper", teacherProfileLiteMapper);
        ReflectionTestUtils.setField(service, "studentProfileLiteMapper", studentProfileLiteMapper);
        ReflectionTestUtils.setField(service, "applicationEventPublisher", publisher);
        ReflectionTestUtils.setField(service, "skipPaymentCheck", false);

        Map<String, Object> body = new HashMap<>();
        body.put("bizType", "LESSON_REQUEST");
        body.put("eventId", 99L);
        body.put("title", "E2E 实时课程");
        body.put("startAt", System.currentTimeMillis());
        body.put("endAt", System.currentTimeMillis() + 3_600_000);
        body.put("status", "PENDING");
        body.put("creatorUserId", 1L);

        ChatMessageReq req = ChatMessageReq.builder()
                .roomId(10L)
                .msgType(8)
                .body(body)
                .build();

        assertThatThrownBy(() -> service.sendMsg(req, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前仅可发送家教申请");
    }

    @Test
    void shouldAllowLessonRequestSystemMessageAfterChatUnlocked() {
        RoomMapper roomMapper = mock(RoomMapper.class);
        MessageMapper messageMapper = mock(MessageMapper.class);
        TutorApplicationMapper tutorApplicationMapper = mock(TutorApplicationMapper.class);
        CourseEnrollmentMapper courseEnrollmentMapper = mock(CourseEnrollmentMapper.class);
        TeacherProfileLiteMapper teacherProfileLiteMapper = mock(TeacherProfileLiteMapper.class);
        StudentProfileLiteMapper studentProfileLiteMapper = mock(StudentProfileLiteMapper.class);
        ImUserMapper imUserMapper = mock(ImUserMapper.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

        Room room = Room.builder().id(10L).teacherProfileId(100L).studentProfileId(200L).status(1).build();
        when(roomMapper.selectById(10L)).thenReturn(room);
        when(teacherProfileLiteMapper.selectUserIdById(100L)).thenReturn(1L);
        when(studentProfileLiteMapper.selectUserIdById(200L)).thenReturn(2L);
        when(tutorApplicationMapper.selectLatestAcceptedBetween(1L, 2L))
                .thenReturn(TutorApplication.builder().id(99L).chatAccessStatus("CHAT_ENABLED").build());

        ImUser teacher = new ImUser();
        teacher.setId(1L);
        teacher.setRefId(100L);
        teacher.setStatus(0);
        when(imUserMapper.selectById(1L)).thenReturn(teacher);

        ImUser student = new ImUser();
        student.setId(2L);
        student.setRefId(200L);
        student.setStatus(0);
        when(imUserMapper.selectById(2L)).thenReturn(student);

        doAnswer(inv -> {
            Message m = inv.getArgument(0, Message.class);
            m.setId(321L);
            return 1;
        }).when(messageMapper).save(any(Message.class));

        SystemMsgHandler systemMsgHandler = new SystemMsgHandler();
        ReflectionTestUtils.setField(systemMsgHandler, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(systemMsgHandler, "imUserMapper", imUserMapper);
        ReflectionTestUtils.setField(systemMsgHandler, "teacherProfileLiteMapper", teacherProfileLiteMapper);
        ReflectionTestUtils.setField(systemMsgHandler, "studentProfileLiteMapper", studentProfileLiteMapper);
        ReflectionTestUtils.setField(systemMsgHandler, "messageMapper", messageMapper);
        ReflectionTestUtils.invokeMethod(systemMsgHandler, "init");

        ChatServiceImpl service = new ChatServiceImpl();
        ReflectionTestUtils.setField(service, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(service, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(service, "tutorApplicationMapper", tutorApplicationMapper);
        ReflectionTestUtils.setField(service, "courseEnrollmentMapper", courseEnrollmentMapper);
        ReflectionTestUtils.setField(service, "teacherProfileLiteMapper", teacherProfileLiteMapper);
        ReflectionTestUtils.setField(service, "studentProfileLiteMapper", studentProfileLiteMapper);
        ReflectionTestUtils.setField(service, "applicationEventPublisher", publisher);
        ReflectionTestUtils.setField(service, "skipPaymentCheck", false);

        Map<String, Object> body = new HashMap<>();
        body.put("bizType", "LESSON_REQUEST");
        body.put("eventId", 99L);
        body.put("title", "E2E 实时课程");
        body.put("startAt", System.currentTimeMillis());
        body.put("endAt", System.currentTimeMillis() + 3_600_000);
        body.put("status", "PENDING");
        body.put("creatorUserId", 1L);

        ChatMessageReq req = ChatMessageReq.builder()
                .roomId(10L)
                .msgType(8)
                .body(body)
                .build();

        service.sendMsg(req, 1L);

        verify(roomMapper).updateAfterSend(10L, 321L);
    }
}
