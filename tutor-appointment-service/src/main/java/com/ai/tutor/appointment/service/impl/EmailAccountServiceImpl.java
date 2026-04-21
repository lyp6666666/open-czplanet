package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.config.EmailNotificationProperties;
import com.ai.tutor.appointment.mapper.EmailVerifyCodeMapper;
import com.ai.tutor.appointment.mapper.UserEmailMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.dto.email.SendEmailCodeRequest;
import com.ai.tutor.appointment.model.dto.email.VerifyEmailRequest;
import com.ai.tutor.appointment.model.entity.EmailVerifyCode;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.model.entity.UserEmail;
import com.ai.tutor.appointment.model.vo.email.EmailCodeVO;
import com.ai.tutor.appointment.model.vo.email.EmailReminderHintVO;
import com.ai.tutor.appointment.model.vo.email.InternalUserEmailsVO;
import com.ai.tutor.appointment.model.vo.email.UserEmailItemVO;
import com.ai.tutor.appointment.model.vo.email.UserEmailStatusVO;
import com.ai.tutor.appointment.service.EmailAccountService;
import com.ai.tutor.appointment.service.EmailNotificationService;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

@Service
public class EmailAccountServiceImpl implements EmailAccountService {

    private static final String PRIMARY = "PRIMARY";
    private static final String SUMMARY_ONLY = "SUMMARY_ONLY";
    private static final String VERIFIED = "VERIFIED";
    private static final String PENDING = "PENDING";
    private static final String NORMAL = "NORMAL";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Resource
    private UserMapper userMapper;
    @Resource
    private UserEmailMapper userEmailMapper;
    @Resource
    private EmailVerifyCodeMapper verifyCodeMapper;
    @Resource
    private EmailNotificationProperties properties;
    @Resource
    private EmailNotificationService emailNotificationService;

    @Override
    public UserEmailStatusVO getStatus(Long userId) {
        User user = requireUser(userId);
        UserEmail primary = userEmailMapper.selectActiveByUserAndType(userId, PRIMARY);
        UserEmail summary = user.getUserType() != null && user.getUserType() == 2
                ? userEmailMapper.selectActiveByUserAndType(userId, SUMMARY_ONLY)
                : null;
        return UserEmailStatusVO.builder()
                .primaryEmail(toItem(primary))
                .summaryEmail(toItem(summary))
                .canUseSummaryEmail(user.getUserType() != null && user.getUserType() == 2)
                .tips(UserEmailStatusVO.Tips.builder()
                        .primaryEmailMissing(!isVerified(primary))
                        .summaryEmailMissing(user.getUserType() != null && user.getUserType() == 2 && !isVerified(summary))
                        .build())
                .build();
    }

    @Override
    public EmailCodeVO sendCode(Long userId, SendEmailCodeRequest request, String ip) {
        User user = requireUser(userId);
        String emailType = normalizeEmailType(request.getEmailType());
        String scene = normalizeScene(request.getScene());
        String email = normalizeEmail(request.getEmail());
        ensureCanUseEmailType(user, emailType);
        ensurePrimaryNotOccupied(email, emailType, userId);

        EmailVerifyCode latest = verifyCodeMapper.selectLatestPending(userId, email, emailType, scene);
        if (latest != null && latest.getCreateTime() != null) {
            LocalDateTime allowAt = latest.getCreateTime().plusSeconds(properties.getVerify().getResendCooldownSeconds());
            ThrowUtils.throwIf(allowAt.isAfter(LocalDateTime.now()), ErrorCode.OPERATION_ERROR, "验证码发送过于频繁");
        }

        String code = String.format("%06d", new Random().nextInt(1_000_000));
        verifyCodeMapper.cancelPendingByUserAndType(userId, emailType, scene);
        verifyCodeMapper.insert(EmailVerifyCode.builder()
                .userId(userId)
                .email(email)
                .emailType(emailType)
                .codeHash(hash(code))
                .scene(scene)
                .expireAt(LocalDateTime.now().plusMinutes(properties.getVerify().getExpireMinutes()))
                .tryCount(0)
                .sendIp(ip)
                .status(PENDING)
                .build());
        emailNotificationService.createVerificationCodeEmail(userId, emailType, email, code, properties.getVerify().getExpireMinutes());
        return new EmailCodeVO(properties.getVerify().getResendCooldownSeconds(), properties.getVerify().getExpireMinutes() * 60);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserEmailItemVO verify(Long userId, VerifyEmailRequest request) {
        User user = requireUser(userId);
        String emailType = normalizeEmailType(request.getEmailType());
        String scene = normalizeScene(request.getScene());
        String email = normalizeEmail(request.getEmail());
        String code = request.getCode() == null ? "" : request.getCode().trim();
        ensureCanUseEmailType(user, emailType);
        ensurePrimaryNotOccupied(email, emailType, userId);

        EmailVerifyCode latest = verifyCodeMapper.selectLatestPending(userId, email, emailType, scene);
        ThrowUtils.throwIf(latest == null, ErrorCode.VERIFICATION_EXPIRED_ERROR, "验证码不存在或已过期");
        ThrowUtils.throwIf(latest.getExpireAt() == null || latest.getExpireAt().isBefore(LocalDateTime.now()), ErrorCode.VERIFICATION_EXPIRED_ERROR, "验证码已过期");
        ThrowUtils.throwIf(latest.getTryCount() != null && latest.getTryCount() >= properties.getVerify().getMaxTryCount(), ErrorCode.OPERATION_ERROR, "验证码错误次数过多，请重新获取");
        if (!hash(code).equals(latest.getCodeHash())) {
            verifyCodeMapper.increaseTryCount(latest.getId());
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "验证码不正确，请重新输入");
        }

        LocalDateTime now = LocalDateTime.now();
        verifyCodeMapper.markVerified(latest.getId(), now);
        userEmailMapper.deactivateByUserAndType(userId, emailType);
        UserEmail userEmail = UserEmail.builder()
                .userId(userId)
                .emailType(emailType)
                .email(email)
                .emailMasked(mask(email))
                .verifyStatus(VERIFIED)
                .verifiedAt(now)
                .bindSource(normalizeBindSource(request.getBindSource()))
                .bounceStatus(NORMAL)
                .status(1)
                .build();
        userEmailMapper.insert(userEmail);
        emailNotificationService.createLatestSummaryBackfillTasks(userId, emailType, email);
        return toItem(userEmail);
    }

    @Override
    public boolean deleteSummaryEmail(Long userId) {
        User user = requireUser(userId);
        ThrowUtils.throwIf(user.getUserType() == null || user.getUserType() != 2, ErrorCode.NO_AUTH_ERROR);
        userEmailMapper.deactivateByUserAndType(userId, SUMMARY_ONLY);
        return true;
    }

    @Override
    public EmailReminderHintVO getReminderHint(Long userId, String scene) {
        UserEmail primary = userEmailMapper.selectActiveByUserAndType(userId, PRIMARY);
        boolean missing = !isVerified(primary);
        if (!missing) {
            return EmailReminderHintVO.builder().show(false).build();
        }
        String normalized = scene == null ? "" : scene.trim().toUpperCase(Locale.ROOT);
        String level = "CHAT".equals(normalized) ? "LIGHT" : ("COURSE_DETAIL".equals(normalized) ? "STRONG" : "MIDDLE");
        String title = "绑定邮箱，避免错过重要提醒";
        String desc = "绑定后可接收未读消息提醒、上课提醒和课后总结。";
        if ("CHAT".equals(normalized)) {
            title = "绑定邮箱，2 小时未读消息会提醒你";
            desc = "不再错过老师或系统的重要回复。";
        } else if (normalized.startsWith("COURSE")) {
            title = "开课提醒将通过邮箱发送";
            desc = "邮件提醒会和站内提醒同一时间发送，绑定主邮箱即可接收。";
        } else if ("SUMMARY_DETAIL".equals(normalized)) {
            title = "绑定邮箱，后续每节课总结会自动发送给你";
            desc = "学生还可以额外设置一个家长邮箱，仅接收课后总结。";
        }
        return EmailReminderHintVO.builder()
                .show(true)
                .level(level)
                .title(title)
                .description(desc)
                .actionText("立即绑定")
                .actionTarget("/settings/email")
                .dismissible(true)
                .cooldownDays(7)
                .build();
    }

    @Override
    public InternalUserEmailsVO getInternalUserEmails(Long userId) {
        User user = requireUser(userId);
        UserEmail primary = userEmailMapper.selectActiveByUserAndType(userId, PRIMARY);
        UserEmail summary = user.getUserType() != null && user.getUserType() == 2
                ? userEmailMapper.selectActiveByUserAndType(userId, SUMMARY_ONLY)
                : null;
        return InternalUserEmailsVO.builder()
                .userId(userId)
                .userType(user.getUserType())
                .primaryEmail(toInternal(primary))
                .summaryEmail(toInternal(summary))
                .build();
    }

    private User requireUser(Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        User user = userMapper.selectById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return user;
    }

    private String normalizeEmail(String email) {
        String v = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        ThrowUtils.throwIf(v.isEmpty() || !EMAIL_PATTERN.matcher(v).matches(), ErrorCode.PARAMS_ERROR, "请输入正确的邮箱地址");
        return v;
    }

    private String normalizeEmailType(String emailType) {
        String v = emailType == null ? PRIMARY : emailType.trim().toUpperCase(Locale.ROOT);
        ThrowUtils.throwIf(!PRIMARY.equals(v) && !SUMMARY_ONLY.equals(v), ErrorCode.PARAMS_ERROR, "邮箱类型不合法");
        return v;
    }

    private String normalizeScene(String scene) {
        String v = scene == null || scene.isBlank() ? "BIND" : scene.trim().toUpperCase(Locale.ROOT);
        ThrowUtils.throwIf(!"BIND".equals(v) && !"CHANGE".equals(v) && !"REBIND".equals(v), ErrorCode.PARAMS_ERROR, "邮箱验证场景不合法");
        return v;
    }

    private String normalizeBindSource(String bindSource) {
        return bindSource == null || bindSource.isBlank() ? "MY_PAGE" : bindSource.trim().toUpperCase(Locale.ROOT);
    }

    private void ensureCanUseEmailType(User user, String emailType) {
        if (SUMMARY_ONLY.equals(emailType)) {
            ThrowUtils.throwIf(user.getUserType() == null || user.getUserType() != 2, ErrorCode.NO_AUTH_ERROR, "仅学生可设置课后总结邮箱");
        }
    }

    private void ensurePrimaryNotOccupied(String email, String emailType, Long userId) {
        if (!PRIMARY.equals(emailType)) {
            return;
        }
        UserEmail occupied = userEmailMapper.selectPrimaryVerifiedByEmail(email);
        ThrowUtils.throwIf(occupied != null && !occupied.getUserId().equals(userId), ErrorCode.OPERATION_ERROR, "该邮箱已绑定其他账号，请更换邮箱");
    }

    private boolean isVerified(UserEmail email) {
        return email != null && VERIFIED.equals(email.getVerifyStatus()) && NORMAL.equals(email.getBounceStatus());
    }

    private UserEmailItemVO toItem(UserEmail email) {
        if (email == null) {
            return null;
        }
        return UserEmailItemVO.builder()
                .emailMasked(email.getEmailMasked())
                .verifyStatus(email.getVerifyStatus())
                .bounceStatus(email.getBounceStatus())
                .verifiedAt(email.getVerifiedAt())
                .build();
    }

    private InternalUserEmailsVO.EmailValue toInternal(UserEmail email) {
        if (email == null) {
            return null;
        }
        return InternalUserEmailsVO.EmailValue.builder()
                .email(email.getEmail())
                .verified(isVerified(email))
                .bounceStatus(email.getBounceStatus())
                .build();
    }

    private String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + email.substring(Math.max(0, at));
        }
        String name = email.substring(0, at);
        String domain = email.substring(at);
        String prefix = name.substring(0, Math.min(2, name.length()));
        return prefix + "***" + domain;
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
