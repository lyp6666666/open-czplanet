package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.dto.user.TeacherExtInfo;
import com.ai.tutor.appointment.model.dto.parent.TutorBrowseRow;
import com.ai.tutor.appointment.model.entity.TeacherProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TeacherProfileMapper {

    int insert(TeacherProfile teacherProfile);

    TeacherProfile selectByUserId(Long userId);

    int updateTeacherProfile(@Param("teacherExtInfo") TeacherExtInfo teacherExtInfo, @Param("userId") Long userId);

    List<TeacherProfile> listByUserIds(@Param("userIds") List<Long> userIds);

    List<TutorBrowseRow> pageTutorCards(@Param("q") String q,
                                        @Param("city") String city,
                                        @Param("mode") String mode,
                                        @Param("subject") String subject,
                                        @Param("rateMin") BigDecimal rateMin,
                                        @Param("rateMax") BigDecimal rateMax,
                                        @Param("cursor") Long cursor,
                                        @Param("pageSize") Integer pageSize);

    int submitEduVerification(@Param("userId") Long userId,
                              @Param("proofUrls") String proofUrls,
                              @Param("submitTime") LocalDateTime submitTime);

    int submitRealnameVerificationIdPhoto(@Param("userId") Long userId,
                                          @Param("idFrontUrl") String idFrontUrl,
                                          @Param("idBackUrl") String idBackUrl,
                                          @Param("submitTime") LocalDateTime submitTime);

    int submitRealnameVerificationNameIdno(@Param("userId") Long userId,
                                           @Param("realName") String realName,
                                           @Param("idnoCipher") String idnoCipher,
                                           @Param("idnoMasked") String idnoMasked,
                                           @Param("submitTime") LocalDateTime submitTime);

    int approveRealnameVerification(@Param("userId") Long userId, @Param("verifyTime") LocalDateTime verifyTime);

    int rejectRealnameVerification(@Param("userId") Long userId,
                                   @Param("reason") String reason,
                                   @Param("verifyTime") LocalDateTime verifyTime);

    int approveEduVerification(@Param("userId") Long userId, @Param("verifyTime") LocalDateTime verifyTime);

    int rejectEduVerification(@Param("userId") Long userId,
                              @Param("reason") String reason,
                              @Param("verifyTime") LocalDateTime verifyTime);

    int markBasicCompleted(@Param("userId") Long userId);

    int markResumeCompleted(@Param("userId") Long userId);
}
