package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.ai.tutor.appointment.enums.VerificationStatusEnum;
import com.ai.tutor.appointment.enums.VerificationTypeEnum;
import com.ai.tutor.appointment.mapper.TeacherProfileMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.entity.TeacherProfile;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.service.TeacherVerificationService;
import com.ai.tutor.appointment.storage.MinioProperties;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class TeacherVerificationServiceImpl implements TeacherVerificationService {

    @Resource
    private TeacherProfileMapper teacherProfileMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private MinioProperties minioProperties;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void submitEducation(Long userId, List<String> proofUrls) {
        ensureTeacher(userId);
        ThrowUtils.throwIf(proofUrls == null || proofUrls.isEmpty(), ErrorCode.PARAMS_ERROR, "请上传学信网截图");
        ThrowUtils.throwIf(proofUrls.size() > 3, ErrorCode.PARAMS_ERROR, "最多上传3张截图");
        for (String u : proofUrls) {
            validateImageUrl(u);
        }

        TeacherProfile tp = teacherProfileMapper.selectByUserId(userId);
        ThrowUtils.throwIf(tp == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(tp.getEduVerifyStatus() != null && tp.getEduVerifyStatus() == VerificationStatusEnum.PENDING.getValue(),
                ErrorCode.OPERATION_ERROR, "认证审核中");

        String json;
        try {
            json = objectMapper.writeValueAsString(proofUrls);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        int n = teacherProfileMapper.submitEduVerification(userId, json, LocalDateTime.now());
        ThrowUtils.throwIf(n <= 0, ErrorCode.OPERATION_ERROR, "提交失败");
    }

    @Override
    public void submitRealnameIdPhoto(Long userId, String idFrontUrl, String idBackUrl) {
        ensureTeacher(userId);
        validateImageUrl(idFrontUrl);
        validateImageUrl(idBackUrl);

        TeacherProfile tp = teacherProfileMapper.selectByUserId(userId);
        ThrowUtils.throwIf(tp == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(tp.getRealnameVerifyStatus() != null && tp.getRealnameVerifyStatus() == VerificationStatusEnum.PENDING.getValue(),
                ErrorCode.OPERATION_ERROR, "认证审核中");

        int n = teacherProfileMapper.submitRealnameVerificationIdPhoto(userId, idFrontUrl.trim(), idBackUrl.trim(), LocalDateTime.now());
        ThrowUtils.throwIf(n <= 0, ErrorCode.OPERATION_ERROR, "提交失败");
    }

    @Override
    public void submitRealnameNameIdno(Long userId, String realName, String idNo) {
        ensureTeacher(userId);
        String rn = realName == null ? "" : realName.trim();
        ThrowUtils.throwIf(rn.isEmpty(), ErrorCode.PARAMS_ERROR, "请输入姓名");
        ThrowUtils.throwIf(rn.length() > 20, ErrorCode.PARAMS_ERROR, "姓名过长");

        String id = idNo == null ? "" : idNo.trim();
        ThrowUtils.throwIf(id.isEmpty(), ErrorCode.PARAMS_ERROR, "请输入身份证号");
        ThrowUtils.throwIf(!isValidChineseId18(id), ErrorCode.PARAMS_ERROR, "身份证号格式不正确");

        TeacherProfile tp = teacherProfileMapper.selectByUserId(userId);
        ThrowUtils.throwIf(tp == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(tp.getRealnameVerifyStatus() != null && tp.getRealnameVerifyStatus() == VerificationStatusEnum.PENDING.getValue(),
                ErrorCode.OPERATION_ERROR, "认证审核中");

        String masked = maskIdNo(id);
        String cipher = sha256Hex(id);
        int n = teacherProfileMapper.submitRealnameVerificationNameIdno(userId, rn, cipher, masked, LocalDateTime.now());
        ThrowUtils.throwIf(n <= 0, ErrorCode.OPERATION_ERROR, "提交失败");
    }

    @Override
    public void opsApprove(Long userId, VerificationTypeEnum type) {
        ensureTeacher(userId);
        LocalDateTime now = LocalDateTime.now();
        if (type == VerificationTypeEnum.REALNAME) {
            int n = teacherProfileMapper.approveRealnameVerification(userId, now);
            ThrowUtils.throwIf(n <= 0, ErrorCode.OPERATION_ERROR, "审核失败");
            return;
        }
        if (type == VerificationTypeEnum.EDU) {
            int n = teacherProfileMapper.approveEduVerification(userId, now);
            ThrowUtils.throwIf(n <= 0, ErrorCode.OPERATION_ERROR, "审核失败");
            return;
        }
        ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR);
    }

    @Override
    public void opsReject(Long userId, VerificationTypeEnum type, String reason) {
        ensureTeacher(userId);
        String r = reason == null ? "" : reason.trim();
        ThrowUtils.throwIf(r.isEmpty(), ErrorCode.PARAMS_ERROR, "请填写驳回原因");
        ThrowUtils.throwIf(r.length() > 200, ErrorCode.PARAMS_ERROR, "驳回原因过长");
        LocalDateTime now = LocalDateTime.now();

        if (type == VerificationTypeEnum.REALNAME) {
            int n = teacherProfileMapper.rejectRealnameVerification(userId, r, now);
            ThrowUtils.throwIf(n <= 0, ErrorCode.OPERATION_ERROR, "审核失败");
            return;
        }
        if (type == VerificationTypeEnum.EDU) {
            int n = teacherProfileMapper.rejectEduVerification(userId, r, now);
            ThrowUtils.throwIf(n <= 0, ErrorCode.OPERATION_ERROR, "审核失败");
            return;
        }
        ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR);
    }

    private void ensureTeacher(Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR);
        User u = userMapper.selectById(userId);
        ThrowUtils.throwIf(u == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(UserRoleEnum.fromValue(u.getUserType()) != UserRoleEnum.TEACHER, ErrorCode.NO_AUTH_ERROR);
    }

    private void validateImageUrl(String url) {
        String v = url == null ? "" : url.trim();
        ThrowUtils.throwIf(v.isEmpty(), ErrorCode.PARAMS_ERROR, "图片地址不合法");
        if (v.startsWith("/avatars/")) return;
        String base = minioProperties == null ? null : minioProperties.getPublicBaseUrl();
        if (base != null && !base.isBlank() && v.startsWith(base)) return;
        if (minioProperties != null && minioProperties.getAllowedAvatarUrlPrefixes() != null) {
            for (String prefix : minioProperties.getAllowedAvatarUrlPrefixes()) {
                if (prefix != null && !prefix.isBlank() && v.startsWith(prefix)) return;
            }
        }
        ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "图片地址不合法");
    }

    private static boolean isValidChineseId18(String id) {
        if (id == null) return false;
        String v = id.trim();
        if (!v.matches("^\\d{17}[\\dXx]$")) return false;
        int[] w = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        char[] map = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (v.charAt(i) - '0') * w[i];
        }
        char c = map[sum % 11];
        char last = Character.toUpperCase(v.charAt(17));
        return last == c;
    }

    private static String maskIdNo(String id) {
        String v = id.trim();
        if (v.length() <= 8) return "********";
        return v.substring(0, 4) + "************" + v.substring(v.length() - 4);
    }

    private static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

