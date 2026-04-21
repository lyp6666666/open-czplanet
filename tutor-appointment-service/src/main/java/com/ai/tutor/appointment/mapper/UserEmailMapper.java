package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.UserEmail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserEmailMapper {
    int insert(UserEmail userEmail);

    UserEmail selectActiveByUserAndType(@Param("userId") Long userId, @Param("emailType") String emailType);

    List<UserEmail> selectActiveByUser(@Param("userId") Long userId);

    UserEmail selectPrimaryVerifiedByEmail(@Param("email") String email);

    int deactivateByUserAndType(@Param("userId") Long userId, @Param("emailType") String emailType);

    int markInvalid(@Param("id") Long id);

    int updateLastNotifyAt(@Param("id") Long id, @Param("lastNotifyAt") LocalDateTime lastNotifyAt);
}
