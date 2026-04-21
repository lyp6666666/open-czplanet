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
        long applicationId = 666002L;
        long orderId = 666003L;
        long appOrderId = 666004L;
        long msg1 = 9000101L;
        long msg2 = 9000102L;
        long msg3 = 9000103L;

        testBackdoorSeedMapper.upsertUser(teacherUid, "后门教师", props.teacherPhone(),
                "http://127.0.0.1:9000/ai-tutor-assets/avatars/default.svg", 1, 1, teacherProfileId);
        testBackdoorSeedMapper.upsertUser(studentUid, "测试学生", props.studentPhoneValue(),
                "http://127.0.0.1:9000/ai-tutor-assets/avatars/default.svg", 0, 2, studentProfileId);

        testBackdoorSeedMapper.upsertTeacherProfile(teacherProfileId, teacherUid);
        testBackdoorSeedMapper.upsertStudentProfile(studentProfileId, studentUid);

        testBackdoorSeedMapper.upsertStudentJobPosting(demandId, studentUid);

        testBackdoorSeedMapper.upsertRoom(roomId, teacherProfileId, studentProfileId, msg3);

        testBackdoorSeedMapper.upsertTutorApplication(applicationId, teacherUid, studentUid, demandId, roomId);

        testBackdoorSeedMapper.upsertBrokerageOrder(orderId, applicationId, roomId, teacherUid);
        testBackdoorSeedMapper.upsertApplicationBrokerageOrder(appOrderId, applicationId, orderId);

        String extra1 = "{\"bizType\":\"TUTOR_APPLICATION\",\"eventId\":666002,\"title\":\"家教申请\",\"status\":\"PENDING\",\"creatorUserId\":666888,\"contextType\":\"DEMAND\",\"contextId\":666600,\"content\":\"您好，我看了您的需求，和我的授课方向非常匹配，我们可以进一步详细沟通吗？\"}";
        String extra2 = "{\"bizType\":\"TUTOR_APPLICATION_STATUS\",\"eventId\":666002,\"title\":\"家教申请\",\"status\":\"ACCEPTED\",\"actorUserId\":666777}";
        String extra3 = "{\"bizType\":\"BROKERAGE_REQUIRED\",\"eventId\":666003,\"proposalId\":666002,\"title\":\"信息费支付\",\"status\":\"PENDING\",\"creatorUserId\":666888,\"amountFen\":1}";

        testBackdoorSeedMapper.upsertMessage(msg1, roomId, teacherUid, studentUid, "家教申请", extra1);
        testBackdoorSeedMapper.upsertMessage(msg2, roomId, studentUid, teacherUid, "家教申请：ACCEPTED", extra2);
        testBackdoorSeedMapper.upsertMessage(msg3, roomId, studentUid, teacherUid, "信息费支付", extra3);
    }
}
