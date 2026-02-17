package com.ai.tutor.videocallimservice;

import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.service.impl.ChatRoomServiceImpl;
import com.ai.tutor.videocallimservice.common.mapper.StudentProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.mapper.TeacherProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.domain.entity.ImUser;
import com.ai.tutor.videocallimservice.common.mapper.ImUserMapper;
import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.utils.RequestHolder;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class VideoCallImServiceApplicationTests {

    @Test
    void sanity() {
    }

    @Test
    void getOrCreateRoomShouldReturnExistingWhenDuplicateInsert() {
        RoomMapper roomMapper = mock(RoomMapper.class);
        MessageMapper messageMapper = mock(MessageMapper.class);
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

        when(roomMapper.selectByTeacherAndStudent(10L, 20L)).thenReturn(null, Room.builder().id(100L).teacherProfileId(10L).studentProfileId(20L).build());
        doThrow(new DuplicateKeyException("dup")).when(roomMapper).insert(org.mockito.ArgumentMatchers.any(Room.class));

        ChatRoomServiceImpl service = new ChatRoomServiceImpl();
        ReflectionTestUtils.setField(service, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(service, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(service, "imUserMapper", imUserMapper);
        ReflectionTestUtils.setField(service, "teacherProfileLiteMapper", teacherProfileLiteMapper);
        ReflectionTestUtils.setField(service, "studentProfileLiteMapper", studentProfileLiteMapper);

        RequestInfo info = new RequestInfo();
        info.setUid(1L);
        info.setRole(1);
        RequestHolder.set(info);
        try {
            Long roomId = service.getOrCreateRoomWithUser(2L, 1L);
            assertThat(roomId).isEqualTo(100L);
        } finally {
            RequestHolder.remove();
        }
    }

}
