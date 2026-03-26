package com.ai.tutor.admin.service.impl;

import com.ai.tutor.admin.mapper.AdminVerificationMapper;
import com.ai.tutor.admin.model.entity.TeacherProfile;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.service.AdminVerificationService;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminVerificationServiceImpl implements AdminVerificationService {

    @Resource
    private AdminVerificationMapper adminVerificationMapper;

    @Override
    public PageResult<TeacherProfile> listPendingVerifications(int page, int size) {
        long offset = (long) (page - 1) * size;
        List<TeacherProfile> records = adminVerificationMapper.listPendingVerifications(offset, size);
        long total = adminVerificationMapper.countPendingVerifications();

        return PageResult.<TeacherProfile>builder()
                .records(records)
                .total(total)
                .size(size)
                .current(page)
                .build();
    }

    @Override
    public TeacherProfile getVerificationDetails(Long userId) {
        return adminVerificationMapper.selectByUserId(userId);
    }

    @Override
    public void approveVerification(Long userId, String type) {
        if ("REALNAME".equalsIgnoreCase(type)) {
            adminVerificationMapper.approveRealname(userId);
        } else if ("EDU".equalsIgnoreCase(type)) {
            adminVerificationMapper.approveEdu(userId);
        } else {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "Invalid verification type");
        }
    }

    @Override
    public void rejectVerification(Long userId, String type, String reason) {
        if ("REALNAME".equalsIgnoreCase(type)) {
            adminVerificationMapper.rejectRealname(userId, reason);
        } else if ("EDU".equalsIgnoreCase(type)) {
            adminVerificationMapper.rejectEdu(userId, reason);
        } else {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "Invalid verification type");
        }
    }
}
