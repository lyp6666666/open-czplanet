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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
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

        //2. 查询该用户是否存在
        // 查询用户
        User user = userMapper.selectByPhoneAndUserType(phone, role.getValue());

        // 如果用户不存在就注册用户到user表和教师或学生表
        if (user == null) {
            user = new User();
            user.setName("用户" + phone.substring(phone.length() - 4));
            user.setPhone(phone);
            user.setUserType(role.getValue());
            user.setStatus(0);
            user.setActiveStatus(2);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            user.setUserType(role.getValue());
            userMapper.insert(user);

            // 教师注册逻辑
            // 2. 再建 profile
            if (role == UserRoleEnum.TEACHER) {
                TeacherProfile tp = new TeacherProfile();
                tp.setUserId(user.getId());
                tp.setRealName("教师" + phone.substring(phone.length() - 4));
                tp.setStatus(1);
                teacherProfileMapper.insert(tp);
            } else {
                StudentProfile sp = new StudentProfile();
                sp.setUserId(user.getId());
                sp.setRealName("学生" + phone.substring(phone.length() - 4));
                sp.setStatus(1);
                studentProfileMapper.insert(sp);
            }

        }

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
