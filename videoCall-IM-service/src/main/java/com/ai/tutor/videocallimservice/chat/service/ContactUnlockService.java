package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.videocallimservice.chat.domain.entity.BrokerageOrder;
import com.ai.tutor.videocallimservice.chat.domain.entity.CollaborationProposal;
import com.ai.tutor.videocallimservice.chat.domain.entity.CourseEnrollment;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.entity.TutorApplication;
import com.ai.tutor.videocallimservice.chat.domain.enums.BrokerageOrderStatus;
import com.ai.tutor.videocallimservice.chat.domain.enums.CollaborationProposalStatus;
import com.ai.tutor.videocallimservice.chat.domain.enums.CourseEnrollmentStatus;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.UnlockedContactVO;
import com.ai.tutor.videocallimservice.chat.mapper.BrokerageOrderMapper;
import com.ai.tutor.videocallimservice.chat.mapper.CollaborationProposalMapper;
import com.ai.tutor.videocallimservice.chat.mapper.CourseEnrollmentMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.mapper.TutorApplicationMapper;
import com.ai.tutor.videocallimservice.common.mapper.StudentProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.mapper.TeacherProfileLiteMapper;
import com.ai.tutor.videocallimservice.integration.AppointmentInternalClient;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ContactUnlockService {

    @Resource
    private RoomMapper roomMapper;
    @Resource
    private TeacherProfileLiteMapper teacherProfileLiteMapper;
    @Resource
    private StudentProfileLiteMapper studentProfileLiteMapper;
    @Resource
    private CollaborationProposalMapper collaborationProposalMapper;
    @Resource
    private BrokerageOrderMapper brokerageOrderMapper;
    @Resource
    private TutorApplicationMapper tutorApplicationMapper;
    @Resource
    private CourseEnrollmentMapper courseEnrollmentMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private ObjectProvider<AppointmentInternalClient> appointmentInternalClientProvider;

    public UnlockedContactVO getUnlockedContact(Long roomId, Long targetUid, Long uid) {
        ThrowUtils.throwIf(roomId == null || targetUid == null || uid == null, ErrorCode.PARAMS_ERROR);
        Room room = roomMapper.selectById(roomId);
        ThrowUtils.throwIf(room == null || room.getStatus() == null || room.getStatus() != 1, ErrorCode.NOT_FOUND_ERROR);
        Long teacherUid = teacherProfileLiteMapper.selectUserIdById(room.getTeacherProfileId());
        Long studentUid = studentProfileLiteMapper.selectUserIdById(room.getStudentProfileId());
        ThrowUtils.throwIf(teacherUid == null || studentUid == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(teacherUid) && !uid.equals(studentUid), ErrorCode.NO_AUTH_ERROR);
        if (uid.equals(teacherUid)) {
            ThrowUtils.throwIf(!targetUid.equals(studentUid), ErrorCode.NO_AUTH_ERROR);
        } else {
            ThrowUtils.throwIf(!targetUid.equals(teacherUid), ErrorCode.NO_AUTH_ERROR);
        }

        ThrowUtils.throwIf(!isContactUnlocked(roomId), ErrorCode.NOT_FOUND_ERROR);

        String phone = loadPhone(targetUid);
        String normalizedPhone = phone == null ? "" : phone.trim();
        ThrowUtils.throwIf(normalizedPhone.isEmpty(), ErrorCode.OPERATION_ERROR, "对方联系方式暂不可用");
        return UnlockedContactVO.builder().uid(targetUid).phone(normalizedPhone).build();
    }

    private boolean isContactUnlocked(Long roomId) {
        if (roomId == null) {
            return false;
        }
        Room room = roomMapper.selectById(roomId);
        if (room == null || room.getStatus() == null || room.getStatus() != 1) {
            return false;
        }

        CourseEnrollment course = courseEnrollmentMapper.selectLatestByRoomId(roomId);
        if (course != null && course.getStatus() != null) {
            String courseStatus = course.getStatus().trim().toUpperCase();
            if (CourseEnrollmentStatus.REFUND_REVIEW.name().equals(courseStatus)
                    || CourseEnrollmentStatus.TRIAL_REFUND_REVIEW.name().equals(courseStatus)
                    || CourseEnrollmentStatus.REFUNDED.name().equals(courseStatus)
                    || CourseEnrollmentStatus.FINISHED.name().equals(courseStatus)) {
                return false;
            }
        }

        BrokerageOrder paidOrder = brokerageOrderMapper.selectPaidByRoomId(roomId);
        if (paidOrder != null) {
            return true;
        }

        CollaborationProposal proposal = collaborationProposalMapper.selectLatestByRoomId(roomId);
        if (proposal != null && CollaborationProposalStatus.ACCEPTED.name().equalsIgnoreCase(proposal.getStatus())) {
            TutorApplication latestApplication = tutorApplicationMapper.selectLatestByRoomId(roomId);
            if (latestApplication != null
                    && "OFFLINE".equalsIgnoreCase(latestApplication.getTeachingMode())
                    && "ACCEPTED".equalsIgnoreCase(latestApplication.getStatus())) {
                return true;
            }
            BrokerageOrder proposalOrder = brokerageOrderMapper.selectByProposalId(proposal.getId());
            if (proposalOrder != null && BrokerageOrderStatus.PAID.name().equalsIgnoreCase(proposalOrder.getStatus())) {
                return true;
            }
        }
        return false;
    }

    private String loadPhone(Long targetUid) {
        AppointmentInternalClient internalClient = appointmentInternalClientProvider == null ? null : appointmentInternalClientProvider.getIfAvailable();
        if (internalClient != null) {
            try {
                return internalClient.getUserPhoneById(targetUid);
            } catch (Exception ignored) {
            }
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT phone FROM user WHERE id = ? LIMIT 1",
                    new Object[]{targetUid},
                    (rs, rowNum) -> rs.getString("phone")
            );
        } catch (EmptyResultDataAccessException ignored) {
            return "";
        }
    }
}
