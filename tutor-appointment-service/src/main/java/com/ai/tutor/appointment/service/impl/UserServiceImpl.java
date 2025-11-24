package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.enums.RedisKeyPrefix;
import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.ai.tutor.appointment.mapper.StudentProfileMapper;
import com.ai.tutor.appointment.mapper.TeacherProfileMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.entity.StudentProfile;
import com.ai.tutor.appointment.model.entity.TeacherProfile;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.model.vo.LoginUserVO;
import com.ai.tutor.appointment.service.SmsService;
import com.ai.tutor.appointment.service.UserService;
import com.ai.tutor.appointment.utils.JwtUtil;
import com.ai.tutor.appointment.utils.ThrowUtils;
import com.ai.tutor.enums.ErrorCode;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
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

    /**
     * 登录或注册用户
     * @param phone
     * @param code
     * @param userRoleEnum
     * @return
     */
    public LoginUserVO userLoginOrRegister(String phone, String code, UserRoleEnum userRoleEnum) {
        ThrowUtils.throwIf(phone == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(code == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userRoleEnum == null, ErrorCode.PARAMS_ERROR);
        //1. 验证验证码是否正确
        boolean isValid = smsService.verifyCode(phone, code);
        ThrowUtils.throwIf(!isValid, ErrorCode.INCORRECT_VERIFICATION_CODE, "验证码错误或已过期");

        //2. 查询该用户是否存在
        // 获取数据库字段类型
        Integer userType = userRoleEnum.getValue();
        // 查询用户
        User user = userMapper.selectByPhoneAndUserType(phone, userType);

        if (user == null) {
            Long refId = null;
            // 教师注册逻辑
            // 如果是教师，先存教师表
            if (userRoleEnum == UserRoleEnum.TEACHER) {
                TeacherProfile teacherProfile = new TeacherProfile();
                teacherProfile.setRealName("教师" + phone.substring(phone.length() - 4));
                teacherProfile.setEducation("未知");
                teacherProfile.setSubject("未填写");
                teacherProfile.setExperienceYears(0);
                teacherProfile.setRatePerHour(BigDecimal.ZERO);
                teacherProfile.setStatus(1);
                teacherProfile.setCreateTime(LocalDateTime.now());
                teacherProfile.setUpdateTime(LocalDateTime.now());
                teacherProfileMapper.insert(teacherProfile);

                // 保存逻辑外键
                refId = teacherProfile.getId();
            }
            // 如果是学生，先存学生表
            else if (userRoleEnum == UserRoleEnum.STUDENT) {
                StudentProfile studentProfile = new StudentProfile();
                studentProfile.setRealName("学生" + phone.substring(phone.length() - 4));
                studentProfile.setStatus(1);
                studentProfile.setCreateTime(LocalDateTime.now());
                studentProfile.setUpdateTime(LocalDateTime.now());
                studentProfileMapper.insert(studentProfile);

                // 保存逻辑外键
                refId = studentProfile.getId();
            }

            // 4. 注册新用户，refId指向逻辑外键
            user = new User();
            user.setName("用户" + phone.substring(phone.length() - 4));
            user.setPhone(phone);
            user.setUserType(userRoleEnum.getValue());
            user.setRefId(refId);
            user.setStatus(0);
            user.setActiveStatus(2);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            userMapper.insert(user);

        }

        // 5. 生成 JWT Token
        String token = jwtUtil.generateToken(phone, userRoleEnum);

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
                .build();
    }
}
