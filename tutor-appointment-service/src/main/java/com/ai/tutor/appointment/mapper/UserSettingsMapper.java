package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.UserSettings;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserSettingsMapper {
    UserSettings selectByUserId(@Param("userId") Long userId);

    int insert(UserSettings settings);

    int updateApplicationGreeting(@Param("userId") Long userId, @Param("applicationGreeting") String applicationGreeting);
}

