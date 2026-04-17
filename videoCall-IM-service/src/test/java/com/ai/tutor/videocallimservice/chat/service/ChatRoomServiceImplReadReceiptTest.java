package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.entity.RoomReadState;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatRoomPageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatRoomItemResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CursorPageResp;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomReadStateMapper;
import com.ai.tutor.videocallimservice.chat.mapper.TutorApplicationMapper;
import com.ai.tutor.videocallimservice.chat.service.impl.ChatRoomServiceImpl;
import com.ai.tutor.videocallimservice.common.domain.entity.ImUser;
import com.ai.tutor.videocallimservice.common.mapper.ImUserMapper;
import com.ai.tutor.videocallimservice.common.mapper.StudentProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.mapper.TeacherProfileLiteMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatRoomServiceImplReadReceiptTest {

    @Test
    void listRoomsShouldExposePeerLastReadMsgId() {
        RoomMapper roomMapper = mock(RoomMapper.class);
        MessageMapper messageMapper = mock(MessageMapper.class);
        RoomReadStateMapper roomReadStateMapper = mock(RoomReadStateMapper.class);
        ImUserMapper imUserMapper = mock(ImUserMapper.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        TeacherProfileLiteMapper teacherProfileLiteMapper = mock(TeacherProfileLiteMapper.class);
        StudentProfileLiteMapper studentProfileLiteMapper = mock(StudentProfileLiteMapper.class);
        ChatService chatService = mock(ChatService.class);
        TutorApplicationMapper tutorApplicationMapper = mock(TutorApplicationMapper.class);

        Room room = Room.builder()
                .id(10L)
                .teacherProfileId(100L)
                .studentProfileId(200L)
                .lastMsgId(null)
                .activeTime(LocalDateTime.of(2026, 4, 18, 10, 0))
                .status(1)
                .build();
        when(roomMapper.listByStudentProfileId(200L, null, 20)).thenReturn(List.of(room));
        when(studentProfileLiteMapper.selectIdByUserId(2001L)).thenReturn(200L);
        when(teacherProfileLiteMapper.selectUserIdById(100L)).thenReturn(1001L);
        when(roomReadStateMapper.listByRoomIdsAndUid(List.of(10L), 2001L))
                .thenReturn(List.of(RoomReadState.builder().roomId(10L).uid(2001L).lastReadMsgId(998L).build()));
        when(roomReadStateMapper.listByRoomIdsAndUids(List.of(10L), List.of(1001L)))
                .thenReturn(List.of(RoomReadState.builder().roomId(10L).uid(1001L).lastReadMsgId(999L).build()));

        ImUser currentUser = new ImUser();
        currentUser.setId(2001L);
        currentUser.setUserType(2);
        currentUser.setRefId(200L);
        currentUser.setStatus(0);
        when(imUserMapper.selectById(2001L)).thenReturn(currentUser);

        ChatRoomServiceImpl service = new ChatRoomServiceImpl();
        ReflectionTestUtils.setField(service, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(service, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(service, "roomReadStateMapper", roomReadStateMapper);
        ReflectionTestUtils.setField(service, "imUserMapper", imUserMapper);
        ReflectionTestUtils.setField(service, "jdbcTemplate", jdbcTemplate);
        ReflectionTestUtils.setField(service, "teacherProfileLiteMapper", teacherProfileLiteMapper);
        ReflectionTestUtils.setField(service, "studentProfileLiteMapper", studentProfileLiteMapper);
        ReflectionTestUtils.setField(service, "chatService", chatService);
        ReflectionTestUtils.setField(service, "tutorApplicationMapper", tutorApplicationMapper);
        ReflectionTestUtils.setField(service, "tutorApplicationGatingEnabled", false);
        ReflectionTestUtils.setField(service, "skipPaymentCheck", false);

        RequestInfo info = new RequestInfo();
        info.setUid(2001L);
        ReflectionTestUtils.setField(info, "role", 2);
        RequestHolder.set(info);
        try {
            ChatRoomPageReq req = new ChatRoomPageReq();
            req.setPageSize(20);

            CursorPageResp<ChatRoomItemResp> page = service.listRooms(req, 2001L);

            assertThat(page.getList()).hasSize(1);
            ChatRoomItemResp item = page.getList().get(0);
            assertThat(item.getRoomId()).isEqualTo(10L);
            assertThat(item.getMyLastReadMsgId()).isEqualTo(998L);
            assertThat(item.getPeerLastReadMsgId()).isEqualTo(999L);
        } finally {
            RequestHolder.remove();
        }
    }
}
