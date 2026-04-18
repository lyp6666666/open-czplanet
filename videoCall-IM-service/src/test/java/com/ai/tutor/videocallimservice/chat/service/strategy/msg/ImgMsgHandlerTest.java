package com.ai.tutor.videocallimservice.chat.service.strategy.msg;

import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ImgMsgReq;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.common.domain.entity.ImUser;
import com.ai.tutor.videocallimservice.common.mapper.ImUserMapper;
import com.ai.tutor.videocallimservice.common.mapper.StudentProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.mapper.TeacherProfileLiteMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImgMsgHandlerTest {

    @Test
    void shouldSaveImageMessageWithStructuredExtra() {
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

        doAnswer(invocation -> {
            Message m = invocation.getArgument(0, Message.class);
            assertThat(m.getContent()).isEqualTo("[图片]");
            assertThat(m.getExtra()).contains("chat/1001/a.png");
            m.setId(8L);
            return null;
        }).when(messageMapper).save(any(Message.class));

        ImgMsgHandler handler = new ImgMsgHandler();
        ReflectionTestUtils.setField(handler, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(handler, "imUserMapper", imUserMapper);
        ReflectionTestUtils.setField(handler, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(handler, "teacherProfileLiteMapper", teacherProfileLiteMapper);
        ReflectionTestUtils.setField(handler, "studentProfileLiteMapper", studentProfileLiteMapper);
        ReflectionTestUtils.invokeMethod(handler, "init");

        ChatMessageReq req = ChatMessageReq.builder()
                .roomId(100L)
                .msgType(3)
                .body(ImgMsgReq.builder()
                        .url("/api/v1/public/assets/chat/1001/a.png")
                        .objectKey("chat/1001/a.png")
                        .contentType("image/png")
                        .size(1234L)
                        .width(400)
                        .height(300)
                        .build())
                .build();

        Long msgId = handler.checkAndSaveMsg(req, 1L);
        assertThat(msgId).isEqualTo(8L);
    }

    @Test
    void shouldShowImageBodyFromStoredExtra() {
        ImgMsgHandler handler = new ImgMsgHandler();
        Message msg = Message.builder()
                .content("[图片]")
                .extra("{\"url\":\"/api/v1/public/assets/chat/1001/a.png\",\"objectKey\":\"chat/1001/a.png\",\"contentType\":\"image/png\",\"size\":1234,\"width\":400,\"height\":300}")
                .build();

        Object shown = handler.showMsg(msg);
        assertThat(shown).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) shown;
        assertThat(body.get("type")).isEqualTo("image");
        assertThat(body.get("url")).isEqualTo("/api/v1/public/assets/chat/1001/a.png");
        assertThat(body.get("size")).isEqualTo(1234L);
    }
}
