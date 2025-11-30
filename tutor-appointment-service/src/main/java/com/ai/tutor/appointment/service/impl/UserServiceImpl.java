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
     * @param role
     * @return
     */
    public LoginUserVO userLoginOrRegister(String phone, String code, UserRoleEnum role) {
        ThrowUtils.throwIf(phone == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(code == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(role == null, ErrorCode.PARAMS_ERROR);

        //1. 验证验证码是否正确
        boolean isValid = smsService.verifyCode(phone, code);
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

        // 5. 生成 JWT Token
        String token = jwtUtil.generateToken(phone, role);

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
