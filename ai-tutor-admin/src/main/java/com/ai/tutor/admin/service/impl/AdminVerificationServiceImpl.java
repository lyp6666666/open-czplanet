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
        TeacherProfile profile = adminVerificationMapper.selectByUserId(userId);
        ThrowUtils.throwIf(profile == null, ErrorCode.NOT_FOUND_ERROR, "教师资料不存在");
        return profile;
    }

    @Override
    public void approveVerification(Long userId, String type) {
        TeacherProfile profile = getVerificationDetails(userId);
        if ("REALNAME".equalsIgnoreCase(type)) {
            ThrowUtils.throwIf(profile.getRealnameVerifyStatus() == null || profile.getRealnameVerifyStatus() != 1,
                    ErrorCode.OPERATION_ERROR, "实名认证当前不可审核");
            ThrowUtils.throwIf(!"ID_PHOTO".equalsIgnoreCase(profile.getRealnameVerifyMethod()),
                    ErrorCode.OPERATION_ERROR, "实名认证需基于身份证照片审核，请先要求老师重新提交图片材料");
            ThrowUtils.throwIf(isBlank(profile.getRealnameVerifyIdFrontUrl()) || isBlank(profile.getRealnameVerifyIdBackUrl()),
                    ErrorCode.OPERATION_ERROR, "身份证照片不完整，无法通过审核");
            int updated = adminVerificationMapper.approveRealname(userId);
            ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR, "实名认证审核失败");
        } else if ("EDU".equalsIgnoreCase(type)) {
            ThrowUtils.throwIf(profile.getEduVerifyStatus() == null || profile.getEduVerifyStatus() != 1,
                    ErrorCode.OPERATION_ERROR, "学信网认证当前不可审核");
            ThrowUtils.throwIf(isBlank(profile.getEduVerifyProofUrls()),
                    ErrorCode.OPERATION_ERROR, "学信网截图为空，无法通过审核");
            int updated = adminVerificationMapper.approveEdu(userId);
            ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR, "学信网认证审核失败");
        } else {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "Invalid verification type");
        }
    }

    @Override
    public void rejectVerification(Long userId, String type, String reason) {
        String rejectReason = reason == null ? "" : reason.trim();
        ThrowUtils.throwIf(rejectReason.isEmpty(), ErrorCode.PARAMS_ERROR, "请填写驳回原因");
        ThrowUtils.throwIf(rejectReason.length() > 200, ErrorCode.PARAMS_ERROR, "驳回原因过长");
        TeacherProfile profile = getVerificationDetails(userId);
        if ("REALNAME".equalsIgnoreCase(type)) {
            ThrowUtils.throwIf(profile.getRealnameVerifyStatus() == null || profile.getRealnameVerifyStatus() != 1,
                    ErrorCode.OPERATION_ERROR, "实名认证当前不可审核");
            int updated = adminVerificationMapper.rejectRealname(userId, rejectReason);
            ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR, "实名认证驳回失败");
        } else if ("EDU".equalsIgnoreCase(type)) {
            ThrowUtils.throwIf(profile.getEduVerifyStatus() == null || profile.getEduVerifyStatus() != 1,
                    ErrorCode.OPERATION_ERROR, "学信网认证当前不可审核");
            int updated = adminVerificationMapper.rejectEdu(userId, rejectReason);
            ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR, "学信网认证驳回失败");
        } else {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "Invalid verification type");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
