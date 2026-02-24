package com.ai.tutor.appointment.controller;


import com.ai.tutor.appointment.enums.RedisKeyPrefix;
import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.ai.tutor.appointment.model.dto.user.SendCodeRequest;
import com.ai.tutor.appointment.model.dto.user.UpdatePhoneRequest;
import com.ai.tutor.appointment.model.dto.user.UserLoginRequest;
import com.ai.tutor.appointment.model.dto.user.UserUpdateRequest;
import com.ai.tutor.appointment.mapper.StudentProfileMapper;
import com.ai.tutor.appointment.mapper.StudentJobPostingMapper;
import com.ai.tutor.appointment.mapper.TeacherProfileMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.model.vo.LoginUserVO;
import com.ai.tutor.appointment.model.vo.UserCardVO;
import com.ai.tutor.appointment.model.vo.UserMeVO;
import com.ai.tutor.appointment.model.vo.UserSimpleVO;
import com.ai.tutor.appointment.service.UserService;
import com.ai.tutor.appointment.service.impl.SmsServiceImpl;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.annotation.FrequencyControl;
import com.ai.tutor.enums.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Tag(name = "用户模块接口", description = "提供用户登录、注册、验证码发送等相关接口")
public class UserController {

    @Resource
    private SmsServiceImpl smsService;
    @Resource
    private UserService userService;
    @Resource
    private UserMapper userMapper;
    @Resource
    private TeacherProfileMapper teacherProfileMapper;
    @Resource
    private StudentProfileMapper studentProfileMapper;
    @Resource
    private StudentJobPostingMapper studentJobPostingMapper;

    /**
     *  获取验证码
     * @param sendCodeRequest
     * @return
     */
    @PostMapping("/sendcode")
    @Operation(summary = "发送验证码", description = "根据手机号发送验证码（模拟）")
    @FrequencyControl(
            prefixKey = "sms:send:",   // Redis key 前缀
            target = FrequencyControl.Target.EL,  // 通过 EL 取手机号
            spEl = "#p0?.phone",           // 取入参 phone 参数
            time = 1,                  // 时间范围（1 分钟）
            unit = TimeUnit.MINUTES,   // 时间单位：分钟
            count = 1                  // 限制 1 次
    )
    @FrequencyControl(
            prefixKey = "sms:send:ip:",
            target = FrequencyControl.Target.IP,
            time = 1,
            unit = TimeUnit.MINUTES,
            count = 5
    )
    public BaseResponse<String> sendCode(@RequestBody SendCodeRequest sendCodeRequest) {
        ThrowUtils.throwIf(sendCodeRequest.getPhone() == null || sendCodeRequest.getPhone().isEmpty(), ErrorCode.PARAMS_ERROR);
        // 生成并发送验证码
        smsService.sendCode(sendCodeRequest.getPhone(), RedisKeyPrefix.SMS_CODE.getPrefix());
        return ResultUtils.success("验证码发送成功(模拟)");
    }

    /**
     * 用户手脚登录注册功能
     * @param userLoginRequest
     * @return
     */
    @PostMapping("/loginOrRegister")
    @Operation(summary = "手机号验证码登录或注册",
            description = "使用手机号+验证码登录，若用户不存在则自动注册。userRoleEnum 指定注册角色（TEACHER/ STUDENT）")
    public BaseResponse<LoginUserVO> loginOrRegister(@RequestBody UserLoginRequest userLoginRequest) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String phone = userLoginRequest.getPhone();
        String code = userLoginRequest.getCode();
        UserRoleEnum userRoleEnum = userLoginRequest.getUserRoleEnum();
        LoginUserVO loginUserVO = userService.userLoginOrRegister(phone, code, userRoleEnum);
        return ResultUtils.success(loginUserVO);
    }

    @PostMapping("/updateUserInfo")
    @Operation(summary = "更新用户信息", description = "更新用户信息")
    public BaseResponse<String> updateUserInfo(@RequestBody UserUpdateRequest requestDto, HttpServletRequest request) {

        userService.updateUserInfo(requestDto,request);
        return ResultUtils.success("更新成功");
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前登录用户信息", description = "基于当前登录态返回用户基础信息与角色扩展资料")
    public BaseResponse<UserMeVO> me(HttpServletRequest request) {
        String uidStr = (String) request.getAttribute(com.ai.tutor.utils.RequestHolder.ATTRIBUTE_UID);
        ThrowUtils.throwIf(uidStr == null, ErrorCode.NOT_LOGIN_ERROR);
        Long userId = Long.parseLong(uidStr);
        User user = userMapper.selectById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);

        UserRoleEnum role = UserRoleEnum.fromValue(user.getUserType());
        UserMeVO.UserMeVOBuilder builder = UserMeVO.builder()
                .id(user.getId())
                .name(user.getName())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .sex(user.getSex())
                .userType(user.getUserType());
        if (role == UserRoleEnum.TEACHER) {
            builder.teacherProfile(teacherProfileMapper.selectByUserId(userId));
        } else if (role == UserRoleEnum.STUDENT) {
            builder.studentProfile(studentProfileMapper.selectByUserId(userId));
        }
        return ResultUtils.success(builder.build());
    }

    @GetMapping("/batch")
    @Operation(summary = "批量获取用户基础信息", description = "用于会话列表/聊天等场景的昵称头像补齐")
    public BaseResponse<List<UserSimpleVO>> batch(@RequestParam(value = "ids", required = false) List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResultUtils.success(Collections.emptyList());
        }
        List<User> users = userMapper.selectByIds(ids);
        if (users == null || users.isEmpty()) {
            return ResultUtils.success(Collections.emptyList());
        }
        List<UserSimpleVO> result = users.stream()
                .map(u -> UserSimpleVO.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .avatar(u.getAvatar())
                        .userType(u.getUserType())
                        .build())
                .collect(Collectors.toList());
        return ResultUtils.success(result);
    }

    @GetMapping("/card")
    @Operation(summary = "获取用户名片信息", description = "用于聊天等场景查看对方基础信息（不包含手机号等敏感字段）")
    public BaseResponse<UserCardVO> card(@RequestParam("uid") Long uid, HttpServletRequest request) {
        String uidStr = (String) request.getAttribute(com.ai.tutor.utils.RequestHolder.ATTRIBUTE_UID);
        ThrowUtils.throwIf(uidStr == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(uid == null, ErrorCode.PARAMS_ERROR);

        User user = userMapper.selectById(uid);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);

        UserRoleEnum role = UserRoleEnum.fromValue(user.getUserType());
        UserCardVO.UserCardVOBuilder builder = UserCardVO.builder()
                .user(UserSimpleVO.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .avatar(user.getAvatar())
                        .userType(user.getUserType())
                        .build());

        if (role == UserRoleEnum.TEACHER) {
            builder.teacherProfile(teacherProfileMapper.selectByUserId(uid));
        } else if (role == UserRoleEnum.STUDENT) {
            builder.studentProfile(studentProfileMapper.selectByUserId(uid));
            builder.jobPosting(studentJobPostingMapper.selectLatestPublishedByParentId(uid));
        }

        return ResultUtils.success(builder.build());
    }

    @PostMapping("/updateUserPhone")
    @Operation(summary = "更新用户手机号", description = "更新用户手机号")
    public BaseResponse<String> updateUserPhone(@RequestBody UpdatePhoneRequest requestDto, HttpServletRequest request) {
        userService.updateUserPhone(requestDto,request);
        return ResultUtils.success("更新成功 请重新登录");
    }

    @GetMapping("/sendUpdateUserPhoneCode")
    @Operation(summary = "发送更新用户手机号验证码", description = "发送更新用户手机号验证码")
    public BaseResponse<String> sendUpdateUserPhoneCode(HttpServletRequest request) {
        String uidStr = (String) request.getAttribute(com.ai.tutor.utils.RequestHolder.ATTRIBUTE_UID);
        ThrowUtils.throwIf(uidStr == null, ErrorCode.NOT_LOGIN_ERROR);
        Long userId = Long.parseLong(uidStr);
        User user = userMapper.selectById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        String code = smsService.sendCode(user.getPhone(), RedisKeyPrefix.USER_PHONE.getPrefix());
        return ResultUtils.success(code);
    }


}
