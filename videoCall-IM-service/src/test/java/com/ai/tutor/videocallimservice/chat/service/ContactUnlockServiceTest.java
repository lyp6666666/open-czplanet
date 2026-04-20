package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.videocallimservice.chat.domain.entity.BrokerageOrder;
import com.ai.tutor.videocallimservice.chat.domain.entity.CourseEnrollment;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.entity.TutorApplication;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.UnlockedContactVO;
import com.ai.tutor.videocallimservice.chat.mapper.BrokerageOrderMapper;
import com.ai.tutor.videocallimservice.chat.mapper.CollaborationProposalMapper;
import com.ai.tutor.videocallimservice.chat.mapper.CourseEnrollmentMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.mapper.TutorApplicationMapper;
import com.ai.tutor.videocallimservice.common.mapper.StudentProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.mapper.TeacherProfileLiteMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactUnlockServiceTest {

    @Mock
    private RoomMapper roomMapper;
    @Mock
    private TeacherProfileLiteMapper teacherProfileLiteMapper;
    @Mock
    private StudentProfileLiteMapper studentProfileLiteMapper;
    @Mock
    private CollaborationProposalMapper collaborationProposalMapper;
    @Mock
    private BrokerageOrderMapper brokerageOrderMapper;
    @Mock
    private TutorApplicationMapper tutorApplicationMapper;
    @Mock
    private CourseEnrollmentMapper courseEnrollmentMapper;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ContactUnlockService contactUnlockService;

    @Test
    void shouldReturnPhoneWhenApplicationChatAlreadyEnabledEvenWithoutProposalOrder() {
        Room room = Room.builder()
                .id(10L)
                .teacherProfileId(100L)
                .studentProfileId(200L)
                .status(1)
                .build();
        when(roomMapper.selectById(10L)).thenReturn(room);
        when(teacherProfileLiteMapper.selectUserIdById(100L)).thenReturn(3001L);
        when(studentProfileLiteMapper.selectUserIdById(200L)).thenReturn(4001L);
        when(brokerageOrderMapper.selectPaidByRoomId(10L)).thenReturn(null);
        when(courseEnrollmentMapper.selectLatestByRoomId(10L)).thenReturn(null);
        when(collaborationProposalMapper.selectLatestByRoomId(10L)).thenReturn(null);
        when(tutorApplicationMapper.selectLatestUnlockedBetween(3001L, 4001L)).thenReturn(TutorApplication.builder()
                .id(9527L)
                .status("ACCEPTED")
                .roomId(10L)
                .chatAccessStatus("CHAT_ENABLED")
                .build());
        when(jdbcTemplate.queryForObject(any(String.class), any(Object[].class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn("13800138000");

        UnlockedContactVO result = contactUnlockService.getUnlockedContact(10L, 4001L, 3001L);

        assertThat(result.getUid()).isEqualTo(4001L);
        assertThat(result.getPhone()).isEqualTo("13800138000");
        verify(tutorApplicationMapper).selectLatestUnlockedBetween(3001L, 4001L);
    }

    @Test
    void shouldReturnPhoneWhenRoomAlreadyHasPaidOrder() {
        Room room = Room.builder()
                .id(11L)
                .teacherProfileId(101L)
                .studentProfileId(201L)
                .status(1)
                .build();
        when(roomMapper.selectById(11L)).thenReturn(room);
        when(teacherProfileLiteMapper.selectUserIdById(101L)).thenReturn(3002L);
        when(studentProfileLiteMapper.selectUserIdById(201L)).thenReturn(4002L);
        when(courseEnrollmentMapper.selectLatestByRoomId(11L)).thenReturn(null);
        when(brokerageOrderMapper.selectPaidByRoomId(11L)).thenReturn(BrokerageOrder.builder()
                .id(88L)
                .roomId(11L)
                .status("PAID")
                .build());
        when(jdbcTemplate.queryForObject(any(String.class), any(Object[].class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn("13900139000");

        UnlockedContactVO result = contactUnlockService.getUnlockedContact(11L, 3002L, 4002L);

        assertThat(result.getPhone()).isEqualTo("13900139000");
    }

    @Test
    void shouldReturnPhoneWhenLatestRoomApplicationChangedButAcceptedUnlockedApplicationStillExistsBetweenUsers() {
        Room room = Room.builder()
                .id(12L)
                .teacherProfileId(102L)
                .studentProfileId(202L)
                .status(1)
                .build();
        when(roomMapper.selectById(12L)).thenReturn(room);
        when(teacherProfileLiteMapper.selectUserIdById(102L)).thenReturn(3003L);
        when(studentProfileLiteMapper.selectUserIdById(202L)).thenReturn(4003L);
        when(brokerageOrderMapper.selectPaidByRoomId(12L)).thenReturn(null);
        when(courseEnrollmentMapper.selectLatestByRoomId(12L)).thenReturn(null);
        when(collaborationProposalMapper.selectLatestByRoomId(12L)).thenReturn(null);
        when(tutorApplicationMapper.selectLatestUnlockedBetween(3003L, 4003L)).thenReturn(TutorApplication.builder()
                .id(9528L)
                .roomId(12L)
                .status("ACCEPTED")
                .chatAccessStatus("CHAT_ENABLED")
                .build());
        when(jdbcTemplate.queryForObject(any(String.class), any(Object[].class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn("13700137000");

        UnlockedContactVO result = contactUnlockService.getUnlockedContact(12L, 3003L, 4003L);

        assertThat(result.getPhone()).isEqualTo("13700137000");
        verify(tutorApplicationMapper, never()).selectLatestByRoomId(12L);
        verify(tutorApplicationMapper).selectLatestUnlockedBetween(3003L, 4003L);
    }

    @Test
    void shouldNotUnlockContactWhenCourseAlreadyFinishedOrRefunding() {
        Room room = Room.builder()
                .id(13L)
                .teacherProfileId(103L)
                .studentProfileId(203L)
                .status(1)
                .build();
        when(roomMapper.selectById(13L)).thenReturn(room);
        when(teacherProfileLiteMapper.selectUserIdById(103L)).thenReturn(3004L);
        when(studentProfileLiteMapper.selectUserIdById(203L)).thenReturn(4004L);
        when(courseEnrollmentMapper.selectLatestByRoomId(13L)).thenReturn(CourseEnrollment.builder()
                .id(66L)
                .roomId(13L)
                .status("TRIAL_REFUND_REVIEW")
                .build());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> contactUnlockService.getUnlockedContact(13L, 4004L, 3004L));
    }
}
