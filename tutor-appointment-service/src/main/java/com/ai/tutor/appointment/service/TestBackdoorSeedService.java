package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.config.TestBackdoorTeacherProperties;
import com.ai.tutor.appointment.mapper.TestBackdoorSeedMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class TestBackdoorSeedService {

    private static final String DEFAULT_AVATAR = "/api/v1/public/assets/avatars/default.svg";
    private static final long PRIMARY_DEMAND_ID = 666600L;
    private static final long PRIMARY_EXCLUSIVE_DEMAND_ID = 666601L;
    private static final long LOCAL_DEMAND_ID = 667600L;
    private static final long LOCAL_EXCLUSIVE_DEMAND_ID = 667601L;

    @Resource
    private TestBackdoorSeedMapper testBackdoorSeedMapper;

    @Resource
    private TestBackdoorTeacherProperties props;

    public void ensureSeed() {
        ensureSeedPair(primaryPair());
        ensureSeedPair(localPair());
    }

    public void resetDemoProgress() {
        resetDemoProgress(primaryPair());
        resetDemoProgress(localPair());
    }

    private void resetDemoProgress(TestAccountPair pair) {
        Long teacherUid = pair.teacherUid;
        Long studentUid = pair.studentUid;
        Long roomId = pair.roomId;

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

        ensureSeedPair(pair);
        testBackdoorSeedMapper.upsertRoom(roomId, pair.teacherProfileId, pair.studentProfileId, null);
    }

    private void ensureSeedPair(TestAccountPair pair) {
        testBackdoorSeedMapper.upsertUser(pair.teacherUid, pair.teacherName, pair.teacherPhone,
                DEFAULT_AVATAR, 1, 1, pair.teacherProfileId);
        testBackdoorSeedMapper.upsertUser(pair.studentUid, pair.studentName, pair.studentPhone,
                DEFAULT_AVATAR, 0, 2, pair.studentProfileId);

        testBackdoorSeedMapper.upsertTeacherProfile(
                pair.teacherProfileId,
                pair.teacherUid,
                pair.teacherName,
                "用于联调支付权限的测试教师账号。"
        );
        testBackdoorSeedMapper.upsertStudentProfile(
                pair.studentProfileId,
                pair.studentUid,
                pair.studentName,
                "用于联调支付权限的测试学生账号。"
        );

        testBackdoorSeedMapper.upsertStudentJobPosting(pair.demandId, pair.studentUid);
        testBackdoorSeedMapper.upsertExclusiveStudentJobPosting(pair.exclusiveDemandId, pair.studentUid);
        testBackdoorSeedMapper.upsertRoom(pair.roomId, pair.teacherProfileId, pair.studentProfileId, null);
    }

    private TestAccountPair primaryPair() {
        Long teacherUid = props.getUserId();
        Long studentUid = props.getStudentUserId();
        return new TestAccountPair(
                teacherUid,
                studentUid,
                teacherUid,
                studentUid,
                props.getRedirectRoomId(),
                PRIMARY_DEMAND_ID,
                PRIMARY_EXCLUSIVE_DEMAND_ID,
                props.teacherPhone(),
                props.studentPhoneValue(),
                "后门教师",
                "测试学生"
        );
    }

    private TestAccountPair localPair() {
        return new TestAccountPair(
                props.getLocalTeacherUserId(),
                props.getLocalStudentUserId(),
                props.getLocalTeacherUserId(),
                props.getLocalStudentUserId(),
                props.getLocalRedirectRoomId(),
                LOCAL_DEMAND_ID,
                LOCAL_EXCLUSIVE_DEMAND_ID,
                props.localTeacherPhoneValue(),
                props.localStudentPhoneValue(),
                "本地测试教师",
                "本地测试学生"
        );
    }

    private static class TestAccountPair {
        private final Long teacherUid;
        private final Long studentUid;
        private final Long teacherProfileId;
        private final Long studentProfileId;
        private final Long roomId;
        private final Long demandId;
        private final Long exclusiveDemandId;
        private final String teacherPhone;
        private final String studentPhone;
        private final String teacherName;
        private final String studentName;

        private TestAccountPair(Long teacherUid, Long studentUid, Long teacherProfileId, Long studentProfileId,
                                Long roomId, Long demandId, Long exclusiveDemandId, String teacherPhone,
                                String studentPhone, String teacherName, String studentName) {
            this.teacherUid = teacherUid;
            this.studentUid = studentUid;
            this.teacherProfileId = teacherProfileId;
            this.studentProfileId = studentProfileId;
            this.roomId = roomId;
            this.demandId = demandId;
            this.exclusiveDemandId = exclusiveDemandId;
            this.teacherPhone = teacherPhone;
            this.studentPhone = studentPhone;
            this.teacherName = teacherName;
            this.studentName = studentName;
        }
    }
}
