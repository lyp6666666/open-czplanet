package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.EmailVerifyCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface EmailVerifyCodeMapper {
    int insert(EmailVerifyCode code);

    EmailVerifyCode selectLatestPending(@Param("userId") Long userId,
                                        @Param("email") String email,
                                        @Param("emailType") String emailType,
                                        @Param("scene") String scene);

    int increaseTryCount(@Param("id") Long id);

    int markVerified(@Param("id") Long id, @Param("verifiedAt") LocalDateTime verifiedAt);

    int cancelPendingByUserAndType(@Param("userId") Long userId,
                                   @Param("emailType") String emailType,
                                   @Param("scene") String scene);
}
