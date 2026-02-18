package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.enums.VerificationTypeEnum;

import java.util.List;

public interface TeacherVerificationService {
    void submitEducation(Long userId, List<String> proofUrls);

    void submitRealnameIdPhoto(Long userId, String idFrontUrl, String idBackUrl);

    void submitRealnameNameIdno(Long userId, String realName, String idNo);

    void opsApprove(Long userId, VerificationTypeEnum type);

    void opsReject(Long userId, VerificationTypeEnum type, String reason);
}

