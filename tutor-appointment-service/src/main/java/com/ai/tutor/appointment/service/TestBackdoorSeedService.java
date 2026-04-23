package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.config.TestBackdoorTeacherProperties;
import com.ai.tutor.appointment.mapper.TestBackdoorSeedMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class TestBackdoorSeedService {

    @Resource
    private TestBackdoorSeedMapper testBackdoorSeedMapper;

    @Resource
    private TestBackdoorTeacherProperties props;

    public void ensureSeed() {
        Long teacherUid = props.getUserId();
        Long studentUid = props.getStudentUserId();
        Long teacherProfileId = props.getUserId();
        Long studentProfileId = props.getStudentUserId();
        Long roomId = props.getRedirectRoomId();
        long demandId = 666600L;
        long exclusiveDemandId = 666601L;
        String defaultAvatar = "/api/v1/public/assets/avatars/default.svg";

        testBackdoorSeedMapper.upsertUser(teacherUid, "后门教师", props.teacherPhone(),
                defaultAvatar, 1, 1, teacherProfileId);
        testBackdoorSeedMapper.upsertUser(studentUid, "测试学生", props.studentPhoneValue(),
                defaultAvatar, 0, 2, studentProfileId);

        testBackdoorSeedMapper.upsertTeacherProfile(teacherProfileId, teacherUid);
        testBackdoorSeedMapper.upsertStudentProfile(studentProfileId, studentUid);

        testBackdoorSeedMapper.upsertStudentJobPosting(demandId, studentUid);
        testBackdoorSeedMapper.upsertExclusiveStudentJobPosting(exclusiveDemandId, studentUid);
        testBackdoorSeedMapper.upsertRoom(roomId, teacherProfileId, studentProfileId, null);
    }

    public void resetDemoProgress() {
        Long teacherUid = props.getUserId();
        Long studentUid = props.getStudentUserId();
        Long teacherProfileId = props.getUserId();
        Long studentProfileId = props.getStudentUserId();
        Long roomId = props.getRedirectRoomId();

        testBackdoorSeedMapper.deleteRefundRequestsByRoomId(roomId);
        testBackdoorSeedMapper.deleteApplicationBrokerageOrdersByRoomId(roomId, teacherUid, studentUid);
        testBackdoorSeedMapper.deleteBrokerageOrdersByRoomId(roomId);
        testBackdoorSeedMapper.deleteTutorAppointmentsByRoomId(roomId);
        testBackdoorSeedMapper.deleteCourseEnrollmentsByRoomId(roomId);
        testBackdoorSeedMapper.deleteCollaborationProposalsByRoomId(roomId);
        testBackdoorSeedMapper.deleteTutorApplicationsByUsers(roomId, teacherUid, studentUid);
        testBackdoorSeedMapper.deleteChatRealtimeEvents(roomId, teacherUid, studentUid);
        testBackdoorSeedMapper.deleteRoomReadStatesByRoomId(roomId);
        testBackdoorSeedMapper.deleteMessagesByRoomId(roomId);

        ensureSeed();
        testBackdoorSeedMapper.upsertRoom(roomId, teacherProfileId, studentProfileId, null);
    }
}
