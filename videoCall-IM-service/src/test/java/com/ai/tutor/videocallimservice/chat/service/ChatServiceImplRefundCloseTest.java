package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.entity.TutorApplication;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.mapper.CourseEnrollmentMapper;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.mapper.TutorApplicationMapper;
import com.ai.tutor.videocallimservice.chat.service.impl.ChatServiceImpl;
import com.ai.tutor.videocallimservice.chat.service.strategy.msg.SystemMsgHandler;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceImplRefundCloseTest {

    @Test
    void sendMsgShouldCloseRoomWhenBrokerageRefundRequested() {
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

        ImUser teacher = new ImUser();
        teacher.setId(1L);
        teacher.setStatus(0);
        when(imUserMapper.selectById(1L)).thenReturn(teacher);

        ImUser student = new ImUser();
        student.setId(2L);
        student.setStatus(0);
        when(imUserMapper.selectById(2L)).thenReturn(student);

        when(tutorApplicationMapper.selectLatestAcceptedBetween(1L, 2L))
                .thenReturn(TutorApplication.builder().id(999L).chatAccessStatus("CHAT_ENABLED").build());

        doAnswer(inv -> {
            Message m = inv.getArgument(0, Message.class);
            m.setId(123L);
            return 1;
        }).when(messageMapper).save(any(Message.class));

        SystemMsgHandler systemMsgHandler = new SystemMsgHandler();
        ReflectionTestUtils.setField(systemMsgHandler, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(systemMsgHandler, "imUserMapper", imUserMapper);
        ReflectionTestUtils.setField(systemMsgHandler, "teacherProfileLiteMapper", teacherProfileLiteMapper);
        ReflectionTestUtils.setField(systemMsgHandler, "studentProfileLiteMapper", studentProfileLiteMapper);
        ReflectionTestUtils.setField(systemMsgHandler, "messageMapper", messageMapper);
        ReflectionTestUtils.invokeMethod(systemMsgHandler, "init");

        ChatServiceImpl svc = new ChatServiceImpl();
        ReflectionTestUtils.setField(svc, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(svc, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(svc, "tutorApplicationMapper", tutorApplicationMapper);
        ReflectionTestUtils.setField(svc, "courseEnrollmentMapper", courseEnrollmentMapper);
        ReflectionTestUtils.setField(svc, "teacherProfileLiteMapper", teacherProfileLiteMapper);
        ReflectionTestUtils.setField(svc, "studentProfileLiteMapper", studentProfileLiteMapper);
        ReflectionTestUtils.setField(svc, "applicationEventPublisher", publisher);
        ReflectionTestUtils.setField(svc, "skipPaymentCheck", false);

        Map<String, Object> body = new HashMap<>();
        body.put("bizType", "BROKERAGE_REFUND_REQUEST");
        body.put("eventId", 1L);
        body.put("title", "结束沟通");
        body.put("status", "PENDING_REVIEW");

        ChatMessageReq req = ChatMessageReq.builder()
                .roomId(10L)
                .msgType(8)
                .body(body)
                .build();

        svc.sendMsg(req, 1L);

        verify(roomMapper).updateAfterSend(10L, 123L);
        verify(roomMapper).closeRoom(10L);
    }
}
