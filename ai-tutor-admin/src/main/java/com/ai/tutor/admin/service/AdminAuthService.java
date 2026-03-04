package com.ai.tutor.admin.service;

import com.ai.tutor.admin.model.dto.AdminLoginRequest;
import com.ai.tutor.admin.model.vo.AdminLoginResponse;

public interface AdminAuthService {
    AdminLoginResponse login(AdminLoginRequest request);
}
