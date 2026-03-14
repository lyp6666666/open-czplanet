package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.dto.user.UpdateUserSettingsRequest;
import com.ai.tutor.appointment.model.vo.UserSettingsVO;

public interface UserSettingsService {
    UserSettingsVO getOrCreate(Long userId);

    UserSettingsVO update(Long userId, UpdateUserSettingsRequest req);
}

