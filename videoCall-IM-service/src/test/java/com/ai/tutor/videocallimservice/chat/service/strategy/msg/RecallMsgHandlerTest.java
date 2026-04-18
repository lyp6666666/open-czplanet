package com.ai.tutor.videocallimservice.chat.service.strategy.msg;

import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.RecallMsgReq;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.service.strategy.AbstractMsgHandler;
import com.ai.tutor.videocallimservice.common.domain.entity.ImUser;
import com.ai.tutor.videocallimservice.common.mapper.ImUserMapper;
import com.ai.tutor.videocallimservice.common.mapper.StudentProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.mapper.TeacherProfileLiteMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecallMsgHandlerTest {

    @Test
    void shouldSaveRecallMessageForOwnTextMessage() {
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
        teacher.setStatus(0);
        when(teacherProfileLiteMapper.selectUserIdById(10L)).thenReturn(1L);
        when(imUserMapper.selectById(1L)).thenReturn(teacher);

        ImUser student = new ImUser();
        student.setId(2L);
        student.setStatus(0);
        when(studentProfileLiteMapper.selectUserIdById(20L)).thenReturn(2L);
        when(imUserMapper.selectById(2L)).thenReturn(student);

        Message target = Message.builder()
                .id(501L)
                .roomId(100L)
                .fromUid(1L)
                .toUid(2L)
                .type(1)
                .status(0)
                .content("待撤回文本")
                .build();
        when(messageMapper.getById(501L)).thenReturn(target);
        when(messageMapper.countRecallByTarget(100L, 1L, 501L)).thenReturn(0);

        doAnswer(invocation -> {
            Message insert = invocation.getArgument(0, Message.class);
            assertThat(insert.getToUid()).isEqualTo(2L);
            assertThat(insert.getContent()).isEqualTo("[消息已撤回]");
            assertThat(insert.getReplyMsgId()).isEqualTo(501L);
            insert.setId(9001L);
            return null;
        }).when(messageMapper).save(any(Message.class));

        RecallMsgHandler handler = new RecallMsgHandler();
        ReflectionTestUtils.setField(handler, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(handler, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(handler, AbstractMsgHandler.class, "messageMapper", messageMapper, MessageMapper.class);
        ReflectionTestUtils.setField(handler, "imUserMapper", imUserMapper);
        ReflectionTestUtils.setField(handler, "teacherProfileLiteMapper", teacherProfileLiteMapper);
        ReflectionTestUtils.setField(handler, "studentProfileLiteMapper", studentProfileLiteMapper);
        ReflectionTestUtils.invokeMethod(handler, "init");

        Long msgId = handler.checkAndSaveMsg(ChatMessageReq.builder()
                .roomId(100L)
                .msgType(2)
                .body(RecallMsgReq.builder().targetMsgId(501L).build())
                .build(), 1L);

        assertThat(msgId).isEqualTo(9001L);
        @SuppressWarnings("unchecked")
        Map<String, Object> shown = (Map<String, Object>) handler.showMsg(Message.builder().fromUid(1L).replyMsgId(501L).build());
        assertThat(shown.get("type")).isEqualTo("recall");
        assertThat(shown.get("targetMsgId")).isEqualTo(501L);
        assertThat(shown.get("operatorUid")).isEqualTo(1L);
        assertThat(handler.showContactMsg(null)).isEqualTo("[消息已撤回]");
    }

    @Test
    void shouldRejectRecallWhenTargetMessageBelongsToAnotherSender() {
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
        teacher.setStatus(0);
        when(teacherProfileLiteMapper.selectUserIdById(10L)).thenReturn(1L);
        when(imUserMapper.selectById(1L)).thenReturn(teacher);

        ImUser student = new ImUser();
        student.setId(2L);
        student.setStatus(0);
        when(studentProfileLiteMapper.selectUserIdById(20L)).thenReturn(2L);
        when(imUserMapper.selectById(2L)).thenReturn(student);

        Message target = Message.builder()
                .id(501L)
                .roomId(100L)
                .fromUid(2L)
                .toUid(1L)
                .type(1)
                .status(0)
                .content("对方消息")
                .build();
        when(messageMapper.getById(501L)).thenReturn(target);

        RecallMsgHandler handler = new RecallMsgHandler();
        ReflectionTestUtils.setField(handler, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(handler, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(handler, AbstractMsgHandler.class, "messageMapper", messageMapper, MessageMapper.class);
        ReflectionTestUtils.setField(handler, "imUserMapper", imUserMapper);
        ReflectionTestUtils.setField(handler, "teacherProfileLiteMapper", teacherProfileLiteMapper);
        ReflectionTestUtils.setField(handler, "studentProfileLiteMapper", studentProfileLiteMapper);
        ReflectionTestUtils.invokeMethod(handler, "init");

        assertThatThrownBy(() -> handler.checkAndSaveMsg(ChatMessageReq.builder()
                .roomId(100L)
                .msgType(2)
                .body(RecallMsgReq.builder().targetMsgId(501L).build())
                .build(), 1L)).isInstanceOf(BusinessException.class);
    }
}
