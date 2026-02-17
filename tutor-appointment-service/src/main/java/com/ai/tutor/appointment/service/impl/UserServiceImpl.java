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
import com.ai.tutor.appointment.model.vo.LoginUserVO;
import com.ai.tutor.appointment.service.SmsService;
import com.ai.tutor.appointment.service.UserService;
import com.ai.tutor.appointment.utils.JwtUtil;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.enums.ErrorCode;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

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


    /**
     * 登录或注册用户
     * @param phone
     * @param code
     * @param role
     * @return
     */
    public LoginUserVO userLoginOrRegister(String phone, String code, UserRoleEnum role) {
        ThrowUtils.throwIf(phone == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(code == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(role == null, ErrorCode.PARAMS_ERROR);

        //1. 验证验证码是否正确
        boolean isValid = smsService.verifyCode(phone, code,RedisKeyPrefix.SMS_CODE.getPrefix());
        ThrowUtils.throwIf(!isValid, ErrorCode.INCORRECT_VERIFICATION_CODE, "验证码错误或已过期");

        //2. 查询手机号是否已存在账号（登录/注册同入口：存在则直接登录）
        User user = userMapper.selectByPhone(phone);

        if (user == null) {
            User created = transactionTemplate.execute(status -> {
                try {
                    String baseName = "用户" + phone.substring(phone.length() - 4);
                    User u = new User();
                    u.setName(baseName);
                    u.setPhone(phone);
                    u.setUserType(role.getValue());
                    u.setStatus(0);
                    u.setActiveStatus(2);
                    u.setCreateTime(LocalDateTime.now());
                    u.setUpdateTime(LocalDateTime.now());
                    userMapper.insert(u);
                    ensureProfile(u.getId(), phone, role);
                    return u;
                } catch (DuplicateKeyException e) {
                    User existing = userMapper.selectByPhone(phone);
                    if (existing != null) {
                        ensureProfile(existing.getId(), phone, role);
                        return existing;
                    }

                    String baseName = "用户" + phone.substring(phone.length() - 4);
                    for (int i = 0; i < 3; i++) {
                        try {
                            User u = new User();
                            u.setName(baseName + "-" + ThreadLocalRandom.current().nextInt(1000, 10000));
                            u.setPhone(phone);
                            u.setUserType(role.getValue());
                            u.setStatus(0);
                            u.setActiveStatus(2);
                            u.setCreateTime(LocalDateTime.now());
                            u.setUpdateTime(LocalDateTime.now());
                            userMapper.insert(u);
                            ensureProfile(u.getId(), phone, role);
                            return u;
                        } catch (DuplicateKeyException ignored) {
                        }
                    }

                    ThrowUtils.throwIf(true, ErrorCode.SYSTEM_ERROR);
                    return null;
                }
            });
            ThrowUtils.throwIf(created == null, ErrorCode.SYSTEM_ERROR);
            user = created;
        } else {
            Long userId = user.getId();
            transactionTemplate.execute(status -> {
                ensureProfile(userId, phone, role);
                return true;
            });
        }

        user.setUserType(role.getValue());
        userMapper.updateUserType(user.getId(), role.getValue());

        // 5. 生成 JWT Token（把 userId 写入 claim，后续所有鉴权都以 userId 为准）
        String token = jwtUtil.generateToken(user.getId(), phone, role);

        // 6. 缓存登录态
        String key = RedisKeyPrefix.USER_TOKEN.key(phone);
        redisTemplate.opsForValue().set(key, token, 7, TimeUnit.DAYS);

        return LoginUserVO.builder()
                .id(user.getId())
                .name(user.getName())
                .phone(phone)
                .avatar(user.getAvatar())
                .sex(user.getSex())
                .userType(user.getUserType())
                .token(token)
                .build();
    }

    private void ensureProfile(Long userId, String phone, UserRoleEnum role) {
        if (role == UserRoleEnum.TEACHER) {
            TeacherProfile tp = teacherProfileMapper.selectByUserId(userId);
            if (tp != null) return;
            TeacherProfile created = new TeacherProfile();
            created.setUserId(userId);
            created.setRealName("教师" + phone.substring(phone.length() - 4));
            created.setStatus(1);
            created.setCreateTime(LocalDateTime.now());
            created.setUpdateTime(LocalDateTime.now());
            teacherProfileMapper.insert(created);
        } else {
            StudentProfile sp = studentProfileMapper.selectByUserId(userId);
            if (sp != null) return;
            StudentProfile created = new StudentProfile();
            created.setUserId(userId);
            created.setRealName("学生" + phone.substring(phone.length() - 4));
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

        transactionTemplate.execute(status ->{
            try {
                BaseUserInfo baseUserInfo = requestDto.getBaseUserInfo();
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
                ThrowUtils.throwIf(updateCount <= 0, ErrorCode.OPERATION_ERROR);
                log.info("更新用户信息成功");
                return true;

            }catch (Exception e) {
                status.setRollbackOnly();
                log.info("更新用户信息失败");
                ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR);
            }
            return false;
        });

    }

    @Override
    public void updateUserPhone(UpdatePhoneRequest requestDto, HttpServletRequest request) {
        String newPhone = requestDto.getNewPhone();
        String code = requestDto.getCode();
        ThrowUtils.throwIf(newPhone == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(code == null, ErrorCode.PARAMS_ERROR);
        String uidStr = (String) request.getAttribute(ATTRIBUTE_UID);
        ThrowUtils.throwIf(uidStr == null, ErrorCode.NOT_LOGIN_ERROR);
        Long userId = Long.parseLong(uidStr);
        User user = userMapper.selectById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        String oldPhone = user.getPhone();

        boolean isValid = smsService.verifyCode(oldPhone, code,RedisKeyPrefix.USER_PHONE.getPrefix());
        ThrowUtils.throwIf(!isValid, ErrorCode.INCORRECT_VERIFICATION_CODE, "验证码错误或已过期");
        int count = userMapper.updateUserPhone(newPhone, user.getId());
        ThrowUtils.throwIf(count <= 0, ErrorCode.OPERATION_ERROR);
        redisTemplate.delete(RedisKeyPrefix.USER_PHONE.getPrefix() + oldPhone);
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
}
