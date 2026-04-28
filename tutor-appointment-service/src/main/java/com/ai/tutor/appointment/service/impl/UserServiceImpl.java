package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.enums.RedisKeyPrefix;
import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.ai.tutor.appointment.mapper.StudentProfileMapper;
import com.ai.tutor.appointment.mapper.TeacherProfileMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.dto.user.*;
import com.ai.tutor.appointment.model.entity.StudentProfile;
import com.ai.tutor.appointment.model.entity.TeacherProfile;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.config.TestBackdoorTeacherProperties;
import com.ai.tutor.appointment.model.vo.LoginUserVO;
import com.ai.tutor.appointment.service.SmsService;
import com.ai.tutor.appointment.service.TestBackdoorSeedService;
import com.ai.tutor.appointment.service.InviteService;
import com.ai.tutor.appointment.service.UserService;
import com.ai.tutor.appointment.service.WechatAuthService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.ai.tutor.common.metrics.BizKpiMetrics;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ai.tutor.appointment.utils.JwtUtil;
import com.ai.tutor.appointment.storage.MinioProperties;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.ai.tutor.utils.RequestHolder.ATTRIBUTE_UID;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private SmsService smsService;
    @Resource
    private UserMapper userMapper;
    @Resource
    private JwtUtil jwtUtil;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private TeacherProfileMapper teacherProfileMapper;
    @Resource
    private StudentProfileMapper studentProfileMapper;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private WechatAuthService wechatAuthService;

    @Resource
    private MinioProperties minioProperties;

    @Resource
    private BizKpiMetrics bizKpiMetrics;

    @Resource
    private TestBackdoorTeacherProperties testBackdoorTeacherProperties;

    @Resource
    private TestBackdoorSeedService testBackdoorSeedService;
    @Resource
    private InviteService inviteService;

    private static final String DEFAULT_AVATAR_PATH = "/avatars/default-avatar.svg";

    private String resolveDefaultAvatarUrl() {
        if (minioProperties != null && minioProperties.isEnabled()) {
            String publicBaseUrl = minioProperties.getPublicBaseUrl();
            String objectKey = minioProperties.getDefaultAvatarObjectKey();
            if (publicBaseUrl != null && !publicBaseUrl.trim().isEmpty() && objectKey != null && !objectKey.trim().isEmpty()) {
                String base = publicBaseUrl.trim();
                String key = objectKey.trim();
                if (key.startsWith("/")) {
                    key = key.substring(1);
                }
                if (base.endsWith("/")) {
                    return base + key;
                }
                return base + "/" + key;
            }
        }
        return DEFAULT_AVATAR_PATH;
    }


    /**
     * 登录或注册用户
     * @param phone
     * @param code
     * @param role
     * @return
     */
    public LoginUserVO userLoginOrRegister(String phone, String code, UserRoleEnum role, String inviteCode) {
        ThrowUtils.throwIf(phone == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(code == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(role == null, ErrorCode.PARAMS_ERROR);

        LoginUserVO backdoorLogin = tryBackdoorLogin(phone.trim(), code.trim(), role);
        if (backdoorLogin != null) {
            return backdoorLogin;
        }

        //1. 验证验证码是否正确
        boolean isValid = smsService.verifyCode(phone, code,RedisKeyPrefix.SMS_CODE.getPrefix());
        ThrowUtils.throwIf(!isValid, ErrorCode.INCORRECT_VERIFICATION_CODE, "验证码错误或已过期");

        //2. 查询手机号是否已存在账号（登录/注册同入口：存在则直接登录）
        User user = userMapper.selectByPhone(phone);
        boolean isNew = false;

        // 机构账号不允许走手机号验证码登录，避免普通用户冒用或把机构账号误切换角色
        if (user != null && user.getUserType() != null && user.getUserType() == UserRoleEnum.ORG.getValue()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "机构账号请使用机构登录入口");
        }
        validateUserLoginStatus(user);

        if (user == null) {
            CreateUserResult created = createOrGetExistingUser(phone, role, inviteCode);
            ThrowUtils.throwIf(created == null || created.user == null, ErrorCode.SYSTEM_ERROR);
            user = created.user;
            isNew = created.createdNew;
        } else {
            Long userId = user.getId();
            transactionTemplate.execute(status -> {
                ensureProfile(userId, phone, role);
                // 企业规范：历史用户每次成功登录时都要保证邀请码已补齐，避免依赖邀请页首访触发。
                ensureInviteCodeBestEffort(userId, "existing_login");
                return true;
            });
        }

        if (isNew && bizKpiMetrics != null) {
            /*
             * Grafana 业务 KPI 指标打点（每日新注册用户数，按角色）。
             * - metric: ai_tutor_biz_user_register_total
             * - labels: role=teacher|student|org（本接口仅 teacher/student；org 由管理端创建计入）
             * - PromQL（按天）：sum by (role) (increase(ai_tutor_biz_user_register_total[1d]))
             *
             * 说明：仅在首次创建用户成功时计数（isNew=true），避免重复登录/重试导致重复计数。
             */
            bizKpiMetrics.incUserRegister(role.getCode());
        } else if (!isNew && bizKpiMetrics != null) {
            /*
             * 中文注释：这里统计“老用户登录成功次数”，仅在账号已存在且登录成功后计数，
             * 避免首次注册被误算成登录活跃。
             */
            bizKpiMetrics.incUserLogin(role.getCode());
        }

        user.setUserType(role.getValue());
        userMapper.updateUserType(user.getId(), role.getValue());

        // 5. 生成 JWT Token（把 userId 写入 claim，后续所有鉴权都以 userId 为准）
        String token = jwtUtil.generateToken(user.getId(), phone, role);

        // 6. 缓存登录态
        String key = RedisKeyPrefix.USER_TOKEN.key(phone);
        try {
            redisTemplate.opsForValue().set(key, token, 7, TimeUnit.DAYS);
        } catch (Exception ignored) {
        }

        return LoginUserVO.builder()
                .id(user.getId())
                .name(user.getName())
                .phone(phone)
                .avatar(user.getAvatar() == null || user.getAvatar().trim().isEmpty() ? resolveDefaultAvatarUrl() : user.getAvatar())
                .sex(user.getSex())
                .userType(user.getUserType())
                .isNew(isNew)
                .token(token)
                .build();
    }

    private LoginUserVO tryBackdoorLogin(String phone, String code, UserRoleEnum role) {
        if (testBackdoorTeacherProperties == null || !testBackdoorTeacherProperties.isEnabled()) {
            return null;
        }
        boolean teacherBackdoor = role == UserRoleEnum.TEACHER
                && phone.equals(testBackdoorTeacherProperties.teacherPhone())
                && code.equals(testBackdoorTeacherProperties.teacherCode());
        boolean studentBackdoor = role == UserRoleEnum.STUDENT
                && phone.equals(testBackdoorTeacherProperties.studentPhoneValue())
                && code.equals(testBackdoorTeacherProperties.studentCodeValue());
        boolean localTeacherBackdoor = role == UserRoleEnum.TEACHER
                && phone.equals(testBackdoorTeacherProperties.localTeacherPhoneValue())
                && code.equals(testBackdoorTeacherProperties.localTeacherCodeValue());
        boolean localStudentBackdoor = role == UserRoleEnum.STUDENT
                && phone.equals(testBackdoorTeacherProperties.localStudentPhoneValue())
                && code.equals(testBackdoorTeacherProperties.localStudentCodeValue());
        if (!teacherBackdoor && !studentBackdoor && !localTeacherBackdoor && !localStudentBackdoor) {
            return null;
        }
        testBackdoorSeedService.ensureSeed();
        User user = userMapper.selectByPhone(phone);
        ThrowUtils.throwIf(user == null || user.getId() == null, ErrorCode.NO_AUTH_ERROR, "测试账号未初始化");
        ThrowUtils.throwIf(user.getUserType() == null || user.getUserType() != role.getValue(), ErrorCode.NO_AUTH_ERROR, "测试账号角色异常");
        String token = jwtUtil.generateToken(user.getId(), user.getPhone(), role);
        String key = RedisKeyPrefix.USER_TOKEN.key(user.getPhone());
        try {
            redisTemplate.opsForValue().set(key, token, 7, TimeUnit.DAYS);
        } catch (Exception ignored) {
        }
        LoginUserVO.LoginUserVOBuilder builder = LoginUserVO.builder()
                .id(user.getId())
                .name(user.getName())
                .phone(user.getPhone())
                .avatar(user.getAvatar() == null || user.getAvatar().trim().isEmpty() ? resolveDefaultAvatarUrl() : user.getAvatar())
                .sex(user.getSex())
                .userType(user.getUserType())
                .isNew(false)
                .token(token);
        return builder.build();
    }

    private static class CreateUserResult {
        private final User user;
        private final boolean createdNew;

        private CreateUserResult(User user, boolean createdNew) {
            this.user = user;
            this.createdNew = createdNew;
        }
    }

    private CreateUserResult createOrGetExistingUser(String phone, UserRoleEnum role, String inviteCode) {
        return transactionTemplate.execute(status -> {
            try {
                User u = new User();
                u.setName(null);
                u.setPhone(phone);
                u.setUserType(role.getValue());
                u.setStatus(0);
                u.setActiveStatus(2);
                u.setCreateTime(LocalDateTime.now());
                u.setUpdateTime(LocalDateTime.now());
                userMapper.insert(u);
                ensureProfile(u.getId(), phone, role);
                ensureInviteCodeBestEffort(u.getId(), "register");
                bindInviteCodeIfNeeded(u.getId(), inviteCode);
                return new CreateUserResult(u, true);
            } catch (DuplicateKeyException e) {
                User existing = userMapper.selectByPhone(phone);
                if (existing != null) {
                    ensureProfile(existing.getId(), phone, role);
                    ensureInviteCodeBestEffort(existing.getId(), "duplicate_fallback");
                    return new CreateUserResult(existing, false);
                }
                ThrowUtils.throwIf(true, ErrorCode.SYSTEM_ERROR);
                return null;
            }
        });
    }

    /**
     * 邀请体系属于登录后的增强能力，线上库迁移滞后或邀请子系统异常时不应阻断手机号登录主链路。
     */
    private void ensureInviteCodeBestEffort(Long userId, String scene) {
        if (inviteService == null || userId == null) {
            return;
        }
        try {
            inviteService.ensureInviteCode(userId);
        } catch (Exception e) {
            log.error("invite_code_ensure_skipped scene={} userId={} reason={}", scene, userId, e.getMessage(), e);
        }
    }

    /**
     * 用户主动填写的邀请码仍然保留强校验；只有“无邀请码”的普通登录/注册路径才允许降级。
     */
    private void bindInviteCodeIfNeeded(Long userId, String inviteCode) {
        if (inviteService == null || userId == null || !StringUtils.hasText(inviteCode)) {
            return;
        }
        inviteService.bindInviteCodeIfNeeded(userId, inviteCode);
    }

    private void ensureProfile(Long userId, String phone, UserRoleEnum role) {
        if (role == UserRoleEnum.TEACHER) {
            TeacherProfile tp = teacherProfileMapper.selectByUserId(userId);
            if (tp != null) return;
            TeacherProfile created = new TeacherProfile();
            created.setUserId(userId);
            created.setRealName("");
            created.setStatus(1);
            created.setCreateTime(LocalDateTime.now());
            created.setUpdateTime(LocalDateTime.now());
            teacherProfileMapper.insert(created);
        } else {
            StudentProfile sp = studentProfileMapper.selectByUserId(userId);
            if (sp != null) return;
            StudentProfile created = new StudentProfile();
            created.setUserId(userId);
            created.setRealName("");
            created.setStatus(1);
            created.setCreateTime(LocalDateTime.now());
            created.setUpdateTime(LocalDateTime.now());
            studentProfileMapper.insert(created);
        }
    }

    @Override
    public void updateUserInfo(UserUpdateRequest requestDto, HttpServletRequest request) {
        ThrowUtils.throwIf(requestDto == null, ErrorCode.PARAMS_ERROR);
        String uidStr = (String) request.getAttribute(ATTRIBUTE_UID);
        ThrowUtils.throwIf(uidStr == null, ErrorCode.NOT_LOGIN_ERROR);
        Long userId = Long.parseLong(uidStr);

        ThrowUtils.throwIf(requestDto.getBaseUserInfo() == null
                && requestDto.getTeacherExtInfo() == null
                && requestDto.getStudentExtInfo() == null, ErrorCode.PARAMS_ERROR);

        User user = userMapper.selectById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);

        // 兼容旧入参：如果前端还传 phone，则必须与当前登录用户一致
        if (requestDto.getPhone() != null) {
            ThrowUtils.throwIf(!requestDto.getPhone().equals(user.getPhone()), ErrorCode.NO_AUTH_ERROR);
        }

        StudentExtInfo studentExtInfo = requestDto.getStudentExtInfo();
        TeacherExtInfo teacherExtInfo = requestDto.getTeacherExtInfo();

        BaseUserInfo baseUserInfo = requestDto.getBaseUserInfo();
        if (baseUserInfo != null && baseUserInfo.getAvatar() != null && !baseUserInfo.getAvatar().trim().isEmpty()) {
            validateAvatarUrl(baseUserInfo.getAvatar());
        }

        boolean profileCompletedBefore = isProfileCompleted(user);

        transactionTemplate.execute(status ->{
            try {
                int updateCount = userMapper.updateUserBaseInfo(baseUserInfo, user.getId());
                Integer userType = user.getUserType();
                UserRoleEnum userRoleEnum = UserRoleEnum.fromValue(userType);
                switch (userRoleEnum) {
                    case TEACHER:{
                        if(teacherExtInfo != null) {
                            updateCount += updateTeacherProfile(teacherExtInfo, user.getId());
                        }
                        log.info("更新教师信息成功");
                        break;
                    }
                    case STUDENT: {
                        if(studentExtInfo != null) {
                            updateCount += updateStudentProfile(studentExtInfo, user.getId());
                        }
                        log.info("更新学生信息成功");
                        break;
                    }
                    default: {
                        ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR);// 未定义的用户角色
                        break;
                    }
                }

                if (userRoleEnum == UserRoleEnum.TEACHER) {
                    User latestUser = userMapper.selectById(user.getId());
                    TeacherProfile latestProfile = teacherProfileMapper.selectByUserId(user.getId());
                    boolean basicCompleted = latestUser != null
                            && latestUser.getAvatar() != null
                            && !latestUser.getAvatar().trim().isEmpty()
                            && latestProfile != null
                            && latestProfile.getRealName() != null
                            && !latestProfile.getRealName().trim().isEmpty();
                    if (basicCompleted) {
                        teacherProfileMapper.markBasicCompleted(user.getId());
                    }

                    boolean resumeCompleted = basicCompleted
                            && latestProfile.getEducation() != null
                            && !latestProfile.getEducation().trim().isEmpty()
                            && latestProfile.getCity() != null
                            && !latestProfile.getCity().trim().isEmpty()
                            && latestProfile.getHighestEduSchool() != null
                            && !latestProfile.getHighestEduSchool().trim().isEmpty()
                            && latestProfile.getIntroduction() != null
                            && !latestProfile.getIntroduction().trim().isEmpty()
                            && latestProfile.getSubject() != null
                            && !latestProfile.getSubject().trim().isEmpty();
                    if (resumeCompleted) {
                        teacherProfileMapper.markResumeCompleted(user.getId());
                    }
                }

                ThrowUtils.throwIf(updateCount <= 0, ErrorCode.OPERATION_ERROR);
                log.info("更新用户信息成功");
                return true;

            }catch (Exception e) {
                if (e instanceof BusinessException) {
                    throw (BusinessException) e;
                }
                status.setRollbackOnly();
                log.error("更新用户信息失败", e);
                Throwable root = e;
                while (root.getCause() != null && root.getCause() != root) {
                    root = root.getCause();
                }
                String rootMsg = root.getMessage() == null ? "" : root.getMessage();
                if (rootMsg.contains("Unknown column") || rootMsg.contains("doesn't exist") || rootMsg.contains("does not exist")) {
                    ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "服务端数据库未升级，请执行 sqlDoc/huoyue.sql 或对应迁移脚本后重试");
                }
                ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR);
            }
            return false;
        });

        User latestUser = userMapper.selectById(user.getId());
        if (bizKpiMetrics != null && !profileCompletedBefore && isProfileCompleted(latestUser)) {
            /*
             * 中文注释：资料完成只统计“第一次达到业务准入门槛”的瞬间，后续重复编辑资料不再累计，
             * 这样 dashboard 上展示的才是完成 onboarding 的真实用户数。
             */
            bizKpiMetrics.incProfileCompleted(resolveRoleCode(latestUser));
        }

    }

    /**
     * 头像 URL 写入白名单校验：
     * - 允许写入本项目的 MinIO 对外域名（publicBaseUrl 前缀）；
     * - 兼容历史 `/avatars/` 相对路径（便于灰度/迁移期不断图）；
     * - 可通过配置额外放行前缀（例如旧 CDN 域名）。
     */
    private void validateAvatarUrl(String avatar) {
        String v = avatar.trim();
        if (v.startsWith("/avatars/")) {
            return;
        }
        if (v.startsWith("/api/v1/public/assets/")) {
            return;
        }
        String publicBaseUrl = minioProperties == null ? null : minioProperties.getPublicBaseUrl();
        if (publicBaseUrl != null && !publicBaseUrl.isBlank() && v.startsWith(publicBaseUrl)) {
            return;
        }
        if (minioProperties != null && minioProperties.getAllowedAvatarUrlPrefixes() != null) {
            for (String prefix : minioProperties.getAllowedAvatarUrlPrefixes()) {
                if (prefix != null && !prefix.isBlank() && v.startsWith(prefix)) {
                    return;
                }
            }
        }
        ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "头像地址不合法");
    }

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");

    private void validatePhone(String phone) {
        ThrowUtils.throwIf(phone == null, ErrorCode.PARAMS_ERROR);
        String v = phone.trim();
        ThrowUtils.throwIf(v.isEmpty() || !PHONE_PATTERN.matcher(v).matches(), ErrorCode.PARAMS_ERROR, "手机号格式不合法");
    }

    @Override
    public void updateUserPhone(UpdatePhoneRequest requestDto, HttpServletRequest request) {
        String newPhone = requestDto.getNewPhone();
        String code = requestDto.getCode();
        validatePhone(newPhone);
        ThrowUtils.throwIf(code == null, ErrorCode.PARAMS_ERROR);
        String uidStr = (String) request.getAttribute(ATTRIBUTE_UID);
        ThrowUtils.throwIf(uidStr == null, ErrorCode.NOT_LOGIN_ERROR);
        Long userId = Long.parseLong(uidStr);
        User user = userMapper.selectById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        String oldPhone = user.getPhone();

        User occupied = userMapper.selectByPhone(newPhone.trim());
        ThrowUtils.throwIf(occupied != null && !occupied.getId().equals(userId), ErrorCode.OPERATION_ERROR, "手机号已被占用");

        boolean isValid = smsService.verifyCode(oldPhone, code,RedisKeyPrefix.USER_PHONE.getPrefix());
        ThrowUtils.throwIf(!isValid, ErrorCode.INCORRECT_VERIFICATION_CODE, "验证码错误或已过期");
        int count = userMapper.updateUserPhone(newPhone, user.getId());
        ThrowUtils.throwIf(count <= 0, ErrorCode.OPERATION_ERROR);
        redisTemplate.delete(RedisKeyPrefix.USER_PHONE.getPrefix() + oldPhone);
        redisTemplate.delete(RedisKeyPrefix.USER_TOKEN.getPrefix() + oldPhone);
        log.info("更新手机号成功");

    }

    @Override
    public void updateUserPhoneV2(UpdatePhoneV2Request requestDto, HttpServletRequest request) {
        ThrowUtils.throwIf(requestDto == null, ErrorCode.PARAMS_ERROR);
        validatePhone(requestDto.getNewPhone());
        ThrowUtils.throwIf(requestDto.getOldCode() == null || requestDto.getOldCode().trim().isEmpty(), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(requestDto.getNewCode() == null || requestDto.getNewCode().trim().isEmpty(), ErrorCode.PARAMS_ERROR);

        String uidStr = (String) request.getAttribute(ATTRIBUTE_UID);
        ThrowUtils.throwIf(uidStr == null, ErrorCode.NOT_LOGIN_ERROR);
        Long userId = Long.parseLong(uidStr);
        User user = userMapper.selectById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);

        String oldPhone = user.getPhone();
        String newPhone = requestDto.getNewPhone().trim();
        ThrowUtils.throwIf(oldPhone != null && oldPhone.trim().equals(newPhone), ErrorCode.PARAMS_ERROR, "新手机号不能与旧手机号相同");

        User occupied = userMapper.selectByPhone(newPhone);
        ThrowUtils.throwIf(occupied != null && !occupied.getId().equals(userId), ErrorCode.OPERATION_ERROR, "手机号已被占用");

        boolean oldOk = smsService.verifyCode(oldPhone, requestDto.getOldCode().trim(), RedisKeyPrefix.USER_PHONE.getPrefix());
        ThrowUtils.throwIf(!oldOk, ErrorCode.INCORRECT_VERIFICATION_CODE, "旧手机号验证码错误或已过期");
        boolean newOk = smsService.verifyCode(newPhone, requestDto.getNewCode().trim(), RedisKeyPrefix.USER_PHONE.getPrefix());
        ThrowUtils.throwIf(!newOk, ErrorCode.INCORRECT_VERIFICATION_CODE, "新手机号验证码错误或已过期");

        int count = userMapper.updateUserPhone(newPhone, userId);
        ThrowUtils.throwIf(count <= 0, ErrorCode.OPERATION_ERROR);

        redisTemplate.delete(RedisKeyPrefix.USER_PHONE.getPrefix() + oldPhone);
        redisTemplate.delete(RedisKeyPrefix.USER_PHONE.getPrefix() + newPhone);
        redisTemplate.delete(RedisKeyPrefix.USER_TOKEN.getPrefix() + oldPhone);
        log.info("更新手机号成功");
    }

    private int updateStudentProfile(StudentExtInfo studentExtInfo,Long userId) {
        StudentProfile studentProfile = studentProfileMapper.selectByUserId(userId);
        if(studentProfile == null) {
            studentProfile = new StudentProfile();
            studentProfile.setUserId(userId);
            studentProfile.setCreateTime(LocalDateTime.now());
            studentProfile.setUpdateTime(LocalDateTime.now());
            studentProfile.setStatus(1);
            studentProfileMapper.insert(studentProfile);
        }
        int count = studentProfileMapper.updateStudentProfile(studentExtInfo, userId);
        return count;
    }
    private int updateTeacherProfile(TeacherExtInfo teacherExtInfo,Long userId) {
        TeacherProfile teacherProfile = teacherProfileMapper.selectByUserId(userId);
        if(teacherProfile == null) {
            teacherProfile = new TeacherProfile();
            teacherProfile.setUserId(userId);
            teacherProfile.setCreateTime(LocalDateTime.now());
            teacherProfile.setUpdateTime(LocalDateTime.now());
            teacherProfile.setStatus(1);
            teacherProfileMapper.insert(teacherProfile);
        }
        int count = teacherProfileMapper.updateTeacherProfile(teacherExtInfo, userId);
        return count;
    }

    private boolean isProfileCompleted(User user) {
        if (user == null || user.getId() == null || user.getUserType() == null) {
            return false;
        }
        UserRoleEnum role = UserRoleEnum.fromValue(user.getUserType());
        if (role == null) {
            return false;
        }
        if (role == UserRoleEnum.TEACHER) {
            TeacherProfile teacherProfile = teacherProfileMapper.selectByUserId(user.getId());
            return teacherProfile != null
                    && hasText(user.getAvatar())
                    && hasText(teacherProfile.getRealName())
                    && hasText(teacherProfile.getIntroduction())
                    && hasText(teacherProfile.getSubject());
        }
        StudentProfile studentProfile = studentProfileMapper.selectByUserId(user.getId());
        return studentProfile != null
                && hasText(user.getAvatar())
                && hasText(studentProfile.getRealName());
    }

    private static String resolveRoleCode(User user) {
        if (user == null || user.getUserType() == null) {
            return "unknown";
        }
        UserRoleEnum role = UserRoleEnum.fromValue(user.getUserType());
        return role == null ? "unknown" : role.getCode();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Override
    public LoginUserVO wechatLogin(String code) {
        // 1. Get OpenID from WeChat
        WxMaJscode2SessionResult session = wechatAuthService.login(code);
        String openid = session.getOpenid();

        // 2. Find user by OpenID
        User user = userMapper.selectByOpenId(openid);
        boolean isNew = false;

        if (user == null) {
            // 3. Create new user if not exists
            isNew = true;
            user = new User();
            user.setOpenId(openid);
            user.setUserType(UserRoleEnum.STUDENT.getValue()); // Default to Student
            user.setStatus(0);
            user.setActiveStatus(1);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            // Name and Avatar can be empty initially, or updated later
            userMapper.insert(user);
            ensureProfile(user.getId(), null, UserRoleEnum.STUDENT);
            if (inviteService != null) {
                inviteService.ensureInviteCode(user.getId());
            }
        } else {
            validateUserLoginStatus(user);
            if (inviteService != null) {
                inviteService.ensureInviteCode(user.getId());
            }
        }

        // 4. Generate Token
        // Phone might be null for WeChat users initially
        String phone = user.getPhone();
        String token = jwtUtil.generateToken(user.getId(), phone != null ? phone : "", UserRoleEnum.fromValue(user.getUserType()));

        // 5. Cache token
        if (phone != null) {
             String key = RedisKeyPrefix.USER_TOKEN.key(phone);
             redisTemplate.opsForValue().set(key, token, 7, TimeUnit.DAYS);
        }

        return LoginUserVO.builder()
                .id(user.getId())
                .name(user.getName())
                .phone(user.getPhone())
                .avatar(user.getAvatar() == null || user.getAvatar().trim().isEmpty() ? resolveDefaultAvatarUrl() : user.getAvatar())
                .sex(user.getSex())
                .userType(user.getUserType())
                .isNew(isNew)
                .token(token)
                .openid(user.getOpenId())
                .build();
    }

    private void validateUserLoginStatus(User user) {
        if (user == null) {
            return;
        }
        /*
         * 企业规范：被管理端禁用/删除的账号不得继续登录，也不得继续沿用旧 token 访问业务接口。
         * 当前库表以 user.status=0 表示正常，1 表示已拉黑/禁用。
         */
        ThrowUtils.throwIf(user.getStatus() != null && user.getStatus() != 0, ErrorCode.NO_AUTH_ERROR, "当前账号已被禁用，请联系平台处理");
    }
}
