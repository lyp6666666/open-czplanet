package com.ai.tutor.appointment.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TestBackdoorSeedMapper {

    int upsertUser(@Param("id") Long id,
                   @Param("name") String name,
                   @Param("phone") String phone,
                   @Param("avatar") String avatar,
                   @Param("sex") Integer sex,
                   @Param("userType") Integer userType,
                   @Param("refId") Long refId);

    int upsertTeacherProfile(@Param("id") Long id, @Param("userId") Long userId);

    int upsertStudentProfile(@Param("id") Long id, @Param("userId") Long userId);

    int upsertStudentJobPosting(@Param("id") Long id, @Param("parentId") Long parentId);

    int upsertExclusiveStudentJobPosting(@Param("id") Long id, @Param("parentId") Long parentId);

    int deleteRefundRequestsByRoomId(@Param("roomId") Long roomId);

    int deleteApplicationBrokerageOrdersByRoomId(@Param("roomId") Long roomId,
                                                 @Param("teacherUid") Long teacherUid,
                                                 @Param("studentUid") Long studentUid);

    int deleteBrokerageOrdersByRoomId(@Param("roomId") Long roomId);

    int deleteTutorAppointmentsByRoomId(@Param("roomId") Long roomId);

    int deleteCourseEnrollmentsByRoomId(@Param("roomId") Long roomId);

    int deleteCollaborationProposalsByRoomId(@Param("roomId") Long roomId);

    int deleteTutorApplicationsByUsers(@Param("roomId") Long roomId,
                                       @Param("teacherUid") Long teacherUid,
                                       @Param("studentUid") Long studentUid);

    int deleteChatRealtimeEvents(@Param("roomId") Long roomId,
                                 @Param("teacherUid") Long teacherUid,
                                 @Param("studentUid") Long studentUid);

    int deleteRoomReadStatesByRoomId(@Param("roomId") Long roomId);

    int deleteMessagesByRoomId(@Param("roomId") Long roomId);

    int upsertRoom(@Param("id") Long id,
                   @Param("teacherProfileId") Long teacherProfileId,
                   @Param("studentProfileId") Long studentProfileId,
                   @Param("lastMsgId") Long lastMsgId);

    int upsertTutorApplication(@Param("id") Long id,
                               @Param("senderUid") Long senderUid,
                               @Param("receiverUid") Long receiverUid,
                               @Param("contextId") Long contextId,
                               @Param("roomId") Long roomId);

    int upsertBrokerageOrder(@Param("id") Long id,
                             @Param("applicationId") Long applicationId,
                             @Param("roomId") Long roomId,
                             @Param("payerUid") Long payerUid);

    int upsertApplicationBrokerageOrder(@Param("id") Long id,
                                        @Param("applicationId") Long applicationId,
                                        @Param("orderId") Long orderId);

    int upsertMessage(@Param("id") Long id,
                      @Param("roomId") Long roomId,
                      @Param("fromUid") Long fromUid,
                      @Param("toUid") Long toUid,
                      @Param("content") String content,
                      @Param("extra") String extra);
}
