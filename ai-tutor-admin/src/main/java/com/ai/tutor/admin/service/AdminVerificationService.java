package com.ai.tutor.admin.service;

import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.model.entity.TeacherProfile;

public interface AdminVerificationService {
    PageResult<TeacherProfile> listPendingVerifications(int page, int size);
    TeacherProfile getVerificationDetails(Long userId);
    void approveVerification(Long userId, String type);
    void rejectVerification(Long userId, String type, String reason);
}
