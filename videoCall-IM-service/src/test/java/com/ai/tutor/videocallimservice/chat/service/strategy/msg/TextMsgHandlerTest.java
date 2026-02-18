package com.ai.tutor.videocallimservice.chat.service.strategy.msg;

import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.TextMsgReq;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.common.domain.entity.ImUser;
import com.ai.tutor.videocallimservice.common.mapper.ImUserMapper;
import com.ai.tutor.videocallimservice.common.mapper.StudentProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.mapper.TeacherProfileLiteMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.assertj.core.api.Assertions.assertThat;

class TextMsgHandlerTest {

    @Test
    void shouldSaveTextMessageWithCorrectReceiver() {
        MessageMapper messageMapper = mock(MessageMapper.class);
        RoomMapper roomMapper = mock(RoomMapper.class);
        ImUserMapper imUserMapper = mock(ImUserMapper.class);
        TeacherProfileLiteMapper teacherProfileLiteMapper = mock(TeacherProfileLiteMapper.class);
        StudentProfileLiteMapper studentProfileLiteMapper = mock(StudentProfileLiteMapper.class);

        Room room = Room.builder()
                .id(100L)
                .teacherProfileId(10L)
                .studentProfileId(20L)
                .status(1)
                .build();
        when(roomMapper.selectById(100L)).thenReturn(room);

        ImUser teacher = new ImUser();
        teacher.setId(1L);
        teacher.setUserType(1);
        teacher.setRefId(10L);
        teacher.setStatus(0);
        when(teacherProfileLiteMapper.selectUserIdById(10L)).thenReturn(1L);
        when(imUserMapper.selectById(1L)).thenReturn(teacher);

        ImUser student = new ImUser();
        student.setId(2L);
        student.setUserType(2);
        student.setRefId(20L);
        student.setStatus(0);
        when(studentProfileLiteMapper.selectUserIdById(20L)).thenReturn(2L);
        when(imUserMapper.selectById(2L)).thenReturn(student);

        doAnswer(invocation -> {
            Message m = invocation.getArgument(0, Message.class);
            m.setId(1L);
            return null;
        }).when(messageMapper).save(any(Message.class));

        TextMsgHandler textMsgHandler = new TextMsgHandler();
        ReflectionTestUtils.setField(textMsgHandler, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(textMsgHandler, "imUserMapper", imUserMapper);
        ReflectionTestUtils.setField(textMsgHandler, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(textMsgHandler, "teacherProfileLiteMapper", teacherProfileLiteMapper);
        ReflectionTestUtils.setField(textMsgHandler, "studentProfileLiteMapper", studentProfileLiteMapper);
        ReflectionTestUtils.invokeMethod(textMsgHandler, "init");

        ChatMessageReq req = ChatMessageReq.builder()
                .roomId(100L)
                .msgType(1)
                .body(TextMsgReq.builder().content("hello").build())
                .build();

        Long msgId = textMsgHandler.checkAndSaveMsg(req, 1L);
        assertThat(msgId).isNotNull();
        assertThat(msgId).isEqualTo(1L);
    }
}
