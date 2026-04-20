package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.videocallimservice.chat.domain.entity.CollaborationProposal;
import com.ai.tutor.videocallimservice.chat.domain.entity.CourseEnrollment;
import com.ai.tutor.videocallimservice.chat.domain.entity.TutorApplication;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CourseDetailVO;
import com.ai.tutor.videocallimservice.chat.mapper.BrokerageOrderMapper;
import com.ai.tutor.videocallimservice.chat.mapper.CollaborationProposalMapper;
import com.ai.tutor.videocallimservice.chat.mapper.CourseEnrollmentMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RefundRequestMapper;
import com.ai.tutor.videocallimservice.chat.mapper.TutorApplicationMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseEnrollmentServiceTest {

    @Test
    void onCollaborationAcceptedShouldPersistOnlineCourseSnapshot() {
        CourseEnrollmentMapper courseEnrollmentMapper = mock(CourseEnrollmentMapper.class);
        RefundRequestMapper refundRequestMapper = mock(RefundRequestMapper.class);
        BrokerageOrderMapper brokerageOrderMapper = mock(BrokerageOrderMapper.class);
        CollaborationProposalMapper collaborationProposalMapper = mock(CollaborationProposalMapper.class);
        TutorApplicationMapper tutorApplicationMapper = mock(TutorApplicationMapper.class);

        CourseEnrollmentService service = new CourseEnrollmentService();
        ReflectionTestUtils.setField(service, "courseEnrollmentMapper", courseEnrollmentMapper);
        ReflectionTestUtils.setField(service, "refundRequestMapper", refundRequestMapper);
        ReflectionTestUtils.setField(service, "brokerageOrderMapper", brokerageOrderMapper);
        ReflectionTestUtils.setField(service, "collaborationProposalMapper", collaborationProposalMapper);
        ReflectionTestUtils.setField(service, "tutorApplicationMapper", tutorApplicationMapper);

        when(courseEnrollmentMapper.selectLatestByRoomId(88L)).thenReturn(CourseEnrollment.builder()
                .id(66L)
                .applicationId(501L)
                .roomId(88L)
                .teacherUid(1001L)
                .studentUid(2001L)
                .status("COMMUNICATING")
                .build());
        when(tutorApplicationMapper.selectLatestByRoomId(88L)).thenReturn(TutorApplication.builder()
                .id(501L)
                .roomId(88L)
                .teachingMode("ONLINE")
                .build());
        when(collaborationProposalMapper.selectById(9001L)).thenReturn(CollaborationProposal.builder()
                .id(9001L)
                .pricePerHour("200 元/小时")
                .classTime("每周三 19:00-21:00")
                .frequencyPerWeek(2)
                .build());

        service.onCollaborationAccepted(88L, 9001L);

        verify(courseEnrollmentMapper).startOnlineCourse(
                eq(66L),
                eq("COMMUNICATING"),
                eq(9001L),
                eq("ONLINE"),
                eq("线上一对一｜200 元/小时｜每周三 19:00-21:00"),
                eq("每周三 19:00-21:00"),
                eq(2),
                eq("200 元/小时"),
                any(),
                any()
        );
    }

    @Test
    void getCourseByRoomShouldRequireParticipant() {
        CourseEnrollmentMapper courseEnrollmentMapper = mock(CourseEnrollmentMapper.class);
        RefundRequestMapper refundRequestMapper = mock(RefundRequestMapper.class);
        BrokerageOrderMapper brokerageOrderMapper = mock(BrokerageOrderMapper.class);
        CollaborationProposalMapper collaborationProposalMapper = mock(CollaborationProposalMapper.class);
        TutorApplicationMapper tutorApplicationMapper = mock(TutorApplicationMapper.class);

        CourseEnrollmentService service = new CourseEnrollmentService();
        ReflectionTestUtils.setField(service, "courseEnrollmentMapper", courseEnrollmentMapper);
        ReflectionTestUtils.setField(service, "refundRequestMapper", refundRequestMapper);
        ReflectionTestUtils.setField(service, "brokerageOrderMapper", brokerageOrderMapper);
        ReflectionTestUtils.setField(service, "collaborationProposalMapper", collaborationProposalMapper);
        ReflectionTestUtils.setField(service, "tutorApplicationMapper", tutorApplicationMapper);

        when(courseEnrollmentMapper.selectLatestByRoomId(88L)).thenReturn(CourseEnrollment.builder()
                .id(66L)
                .applicationId(501L)
                .roomId(88L)
                .teacherUid(1001L)
                .studentUid(2001L)
                .teachingMode("ONLINE")
                .courseName("线上长期课程")
                .status("TRIALING")
                .build());

        CourseDetailVO detail = service.getCourseByRoom(88L, 1001L);
        assertThat(detail.getCourseId()).isEqualTo(66L);
        assertThat(detail.getCourseName()).isEqualTo("线上长期课程");

        assertThatThrownBy(() -> service.getCourseByRoom(88L, 3001L))
                .isInstanceOf(BusinessException.class);
    }
}
