package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.mapper.UserSettingsMapper;
import com.ai.tutor.appointment.model.dto.user.UpdateUserSettingsRequest;
import com.ai.tutor.appointment.model.entity.UserSettings;
import com.ai.tutor.appointment.model.vo.UserSettingsVO;
import com.ai.tutor.appointment.service.UserSettingsService;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class UserSettingsServiceImpl implements UserSettingsService {

    public static final String DEFAULT_APPLICATION_GREETING = "您好，我和岗位的匹配度很高，可以通过详细聊聊吗";

    @Resource
    private UserSettingsMapper userSettingsMapper;

    @Override
    public UserSettingsVO getOrCreate(Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        UserSettings settings = userSettingsMapper.selectByUserId(userId);
        if (settings == null) {
            settings = UserSettings.builder()
                    .userId(userId)
                    .applicationGreeting(DEFAULT_APPLICATION_GREETING)
                    .settingsJson("{}")
                    .build();
            try {
                userSettingsMapper.insert(settings);
            } catch (DuplicateKeyException ignored) {
            }
            settings = userSettingsMapper.selectByUserId(userId);
        }
        String greeting = settings == null ? DEFAULT_APPLICATION_GREETING : normalizeGreeting(settings.getApplicationGreeting());
        return UserSettingsVO.builder().applicationGreeting(greeting).build();
    }

    @Override
    public UserSettingsVO update(Long userId, UpdateUserSettingsRequest req) {
        ThrowUtils.throwIf(userId == null || req == null, ErrorCode.PARAMS_ERROR);
        String greeting = normalizeGreeting(req.getApplicationGreeting());
        getOrCreate(userId);
        int updated = userSettingsMapper.updateApplicationGreeting(userId, greeting);
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR);
        return UserSettingsVO.builder().applicationGreeting(greeting).build();
    }

    private static String normalizeGreeting(String v) {
        String s = v == null ? "" : v.trim();
        if (s.isEmpty()) return DEFAULT_APPLICATION_GREETING;
        ThrowUtils.throwIf(s.length() > 500, ErrorCode.PARAMS_ERROR);
        return s;
    }
}

