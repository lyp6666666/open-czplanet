package com.ai.tutor.videocallimservice.chat.mapper;

import com.ai.tutor.videocallimservice.chat.domain.entity.CourseEnrollment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CourseEnrollmentMapper {

    int insert(CourseEnrollment enrollment);

    CourseEnrollment selectByApplicationId(@Param("applicationId") Long applicationId);

    CourseEnrollment selectById(@Param("id") Long id);

    CourseEnrollment selectLatestByRoomId(@Param("roomId") Long roomId);

    Integer countActiveByRoomId(@Param("roomId") Long roomId);

    List<CourseEnrollment> listByTeacher(@Param("teacherUid") Long teacherUid, @Param("offset") long offset, @Param("size") int size);

    List<CourseEnrollment> listByStudent(@Param("studentUid") Long studentUid, @Param("offset") long offset, @Param("size") int size);

    int updateStatus(@Param("id") Long id,
                     @Param("expectedStatus") String expectedStatus,
                     @Param("nextStatus") String nextStatus,
                     @Param("proposalId") Long proposalId,
                     @Param("trialStartAt") LocalDateTime trialStartAt,
                     @Param("trialEndAt") LocalDateTime trialEndAt);

    int updateRoomId(@Param("applicationId") Long applicationId, @Param("roomId") Long roomId);
}
