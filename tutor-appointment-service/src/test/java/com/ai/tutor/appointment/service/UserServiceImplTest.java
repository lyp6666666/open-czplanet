package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.enums.RedisKeyPrefix;
import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.ai.tutor.appointment.mapper.StudentProfileMapper;
import com.ai.tutor.appointment.mapper.TeacherProfileMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.entity.StudentProfile;
import com.ai.tutor.appointment.model.entity.TeacherProfile;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.model.dto.user.BaseUserInfo;
import com.ai.tutor.appointment.model.dto.user.TeacherExtInfo;
import com.ai.tutor.appointment.model.dto.user.UserUpdateRequest;
import com.ai.tutor.appointment.model.vo.LoginUserVO;
import com.ai.tutor.appointment.service.impl.UserServiceImpl;
import com.ai.tutor.appointment.service.InviteService;
import com.ai.tutor.appointment.utils.JwtUtil;
import com.ai.tutor.appointment.storage.MinioProperties;
import com.ai.tutor.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.servlet.http.HttpServletRequest;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private SmsService smsService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private TeacherProfileMapper teacherProfileMapper;
    @Mock
    private StudentProfileMapper studentProfileMapper;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private InviteService inviteService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl();
        ReflectionTestUtils.setField(userService, "smsService", smsService);
        ReflectionTestUtils.setField(userService, "userMapper", userMapper);
        ReflectionTestUtils.setField(userService, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(userService, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(userService, "teacherProfileMapper", teacherProfileMapper);
        ReflectionTestUtils.setField(userService, "studentProfileMapper", studentProfileMapper);
        ReflectionTestUtils.setField(userService, "transactionTemplate", transactionTemplate);
        ReflectionTestUtils.setField(userService, "inviteService", inviteService);

        MinioProperties minioProperties = new MinioProperties();
        minioProperties.setEnabled(true);
        minioProperties.setPublicBaseUrl("https://assets.example.com/ai-tutor");
        ReflectionTestUtils.setField(userService, "minioProperties", minioProperties);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> cb = inv.getArgument(0);
            return cb.doInTransaction(new SimpleTransactionStatus());
        });
    }

    @Test
    void shouldLoginDirectlyWhenPhoneExists() {
        String phone = "13800006909";
        when(smsService.verifyCode(eq(phone), anyString(), anyString())).thenReturn(true);

        User existing = new User();
        existing.setId(1001L);
        existing.setPhone(phone);
        existing.setName("用户6909");
        when(userMapper.selectByPhone(phone)).thenReturn(existing);

        when(teacherProfileMapper.selectByUserId(1001L)).thenReturn(null);
        when(userMapper.updateUserType(1001L, UserRoleEnum.TEACHER.getValue())).thenReturn(1);

        when(jwtUtil.generateToken(1001L, phone, UserRoleEnum.TEACHER)).thenReturn("token");

        LoginUserVO vo = userService.userLoginOrRegister(phone, "1234", UserRoleEnum.TEACHER, null);
        assertThat(vo.getId()).isEqualTo(1001L);
        assertThat(vo.getToken()).isEqualTo("token");
        assertThat(vo.getUserType()).isEqualTo(UserRoleEnum.TEACHER.getValue());
        assertThat(vo.getIsNew()).isFalse();

        verify(userMapper, never()).insert(any());
        verify(teacherProfileMapper, times(1)).insert(any(TeacherProfile.class));
        verify(valueOperations, times(1)).set(eq(RedisKeyPrefix.USER_TOKEN.key(phone)), eq("token"), eq(7L), eq(TimeUnit.DAYS));
    }

    @Test
    void shouldRegisterWhenPhoneNotExists() {
        String phone = "13800001234";
        when(smsService.verifyCode(eq(phone), anyString(), anyString())).thenReturn(true);

        when(userMapper.selectByPhone(phone)).thenReturn(null);
        doAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2002L);
            return 1;
        }).when(userMapper).insert(any(User.class));
        when(studentProfileMapper.selectByUserId(2002L)).thenReturn(null);
        when(userMapper.updateUserType(2002L, UserRoleEnum.STUDENT.getValue())).thenReturn(1);
        when(jwtUtil.generateToken(2002L, phone, UserRoleEnum.STUDENT)).thenReturn("token2");

        LoginUserVO vo = userService.userLoginOrRegister(phone, "1234", UserRoleEnum.STUDENT, null);
        assertThat(vo.getId()).isEqualTo(2002L);
        assertThat(vo.getUserType()).isEqualTo(UserRoleEnum.STUDENT.getValue());
        assertThat(vo.getIsNew()).isTrue();

        ArgumentCaptor<User> inserted = ArgumentCaptor.forClass(User.class);
        verify(userMapper, times(1)).insert(inserted.capture());
        assertThat(inserted.getValue().getPhone()).isEqualTo(phone);
        assertThat(inserted.getValue().getName()).isNull();

        verify(studentProfileMapper, times(1)).insert(any(StudentProfile.class));
        verify(inviteService, times(1)).ensureInviteCode(2002L);
    }

    @Test
    void shouldEnsureInviteCodeWhenExistingUserLogsIn() {
        String phone = "13800009999";
        when(smsService.verifyCode(eq(phone), anyString(), anyString())).thenReturn(true);

        User existing = new User();
        existing.setId(9001L);
        existing.setPhone(phone);
        existing.setStatus(0);
        existing.setUserType(UserRoleEnum.STUDENT.getValue());
        when(userMapper.selectByPhone(phone)).thenReturn(existing);
        when(studentProfileMapper.selectByUserId(9001L)).thenReturn(new StudentProfile());
        when(userMapper.updateUserType(9001L, UserRoleEnum.STUDENT.getValue())).thenReturn(1);
        when(jwtUtil.generateToken(9001L, phone, UserRoleEnum.STUDENT)).thenReturn("token9001");

        LoginUserVO vo = userService.userLoginOrRegister(phone, "1234", UserRoleEnum.STUDENT, null);

        assertThat(vo.getId()).isEqualTo(9001L);
        verify(inviteService, times(1)).ensureInviteCode(9001L);
    }

    @Test
    void shouldRejectDisabledUserLogin() {
        String phone = "13800008888";
        when(smsService.verifyCode(eq(phone), anyString(), anyString())).thenReturn(true);

        User existing = new User();
        existing.setId(8800L);
        existing.setPhone(phone);
        existing.setStatus(1);
        existing.setUserType(UserRoleEnum.STUDENT.getValue());
        when(userMapper.selectByPhone(phone)).thenReturn(existing);

        assertThatThrownBy(() -> userService.userLoginOrRegister(phone, "1234", UserRoleEnum.STUDENT, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前账号已被禁用");
        verify(inviteService, never()).ensureInviteCode(anyLong());
    }

    @Test
    void shouldBeIdempotentWhenConcurrentInsertHitsDuplicateKey() {
        String phone = "13800005678";
        when(smsService.verifyCode(eq(phone), anyString(), anyString())).thenReturn(true);

        User existing = new User();
        existing.setId(3003L);
        existing.setPhone(phone);
        existing.setName("用户5678");

        when(userMapper.selectByPhone(phone)).thenReturn(null, existing);
        doThrow(new DuplicateKeyException("dup")).when(userMapper).insert(any(User.class));
        when(studentProfileMapper.selectByUserId(3003L)).thenReturn(null);
        when(userMapper.updateUserType(3003L, UserRoleEnum.STUDENT.getValue())).thenReturn(1);
        when(jwtUtil.generateToken(3003L, phone, UserRoleEnum.STUDENT)).thenReturn("token3");

        LoginUserVO vo = userService.userLoginOrRegister(phone, "1234", UserRoleEnum.STUDENT, null);
        assertThat(vo.getId()).isEqualTo(3003L);
        assertThat(vo.getToken()).isEqualTo("token3");
        assertThat(vo.getIsNew()).isFalse();

        verify(studentProfileMapper, times(1)).insert(any(StudentProfile.class));
    }

    @Test
    void shouldReturnExistingWhenConcurrentInsertHitsDuplicateKey() {
        String phone = "13800005678";
        when(smsService.verifyCode(eq(phone), anyString(), anyString())).thenReturn(true);

        User existing = new User();
        existing.setId(4004L);
        existing.setPhone(phone);
        existing.setName(null);

        when(userMapper.selectByPhone(phone)).thenReturn(null, existing);
        doThrow(new DuplicateKeyException("dup")).when(userMapper).insert(any(User.class));

        when(studentProfileMapper.selectByUserId(4004L)).thenReturn(null);
        when(userMapper.updateUserType(4004L, UserRoleEnum.STUDENT.getValue())).thenReturn(1);
        when(jwtUtil.generateToken(4004L, phone, UserRoleEnum.STUDENT)).thenReturn("token4");

        LoginUserVO vo = userService.userLoginOrRegister(phone, "1234", UserRoleEnum.STUDENT, null);
        assertThat(vo.getId()).isEqualTo(4004L);

        ArgumentCaptor<User> inserted = ArgumentCaptor.forClass(User.class);
        verify(userMapper, times(1)).insert(inserted.capture());
        assertThat(inserted.getValue().getName()).isNull();
    }

    @Test
    void shouldBindInviteCodeWhenRegisteringNewUser() {
        String phone = "13800004567";
        when(smsService.verifyCode(eq(phone), anyString(), anyString())).thenReturn(true);
        when(userMapper.selectByPhone(phone)).thenReturn(null);
        doAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(5005L);
            return 1;
        }).when(userMapper).insert(any(User.class));
        when(studentProfileMapper.selectByUserId(5005L)).thenReturn(null);
        when(userMapper.updateUserType(5005L, UserRoleEnum.STUDENT.getValue())).thenReturn(1);
        when(jwtUtil.generateToken(5005L, phone, UserRoleEnum.STUDENT)).thenReturn("token5");

        LoginUserVO vo = userService.userLoginOrRegister(phone, "1234", UserRoleEnum.STUDENT, "ABC123");

        assertThat(vo.getId()).isEqualTo(5005L);
        verify(inviteService).ensureInviteCode(5005L);
        verify(inviteService).bindInviteCodeIfNeeded(5005L, "ABC123");
    }

    @Test
    void updateUserInfoShouldRejectAvatarOutsideAllowlist() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("uid")).thenReturn("1001");

        User user = new User();
        user.setId(1001L);
        user.setPhone("13800006909");
        user.setUserType(UserRoleEnum.STUDENT.getValue());
        when(userMapper.selectById(1001L)).thenReturn(user);

        BaseUserInfo base = new BaseUserInfo();
        base.setAvatar("https://evil.example.com/a.png");
        UserUpdateRequest dto = new UserUpdateRequest();
        dto.setBaseUserInfo(base);

        assertThatThrownBy(() -> userService.updateUserInfo(dto, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("头像地址不合法");
        verify(userMapper, never()).updateUserBaseInfo(any(), anyLong());
    }

    @Test
    void updateUserInfoShouldAllowAvatarWithPublicBaseUrl() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("uid")).thenReturn("1001");

        User user = new User();
        user.setId(1001L);
        user.setPhone("13800006909");
        user.setUserType(UserRoleEnum.STUDENT.getValue());
        when(userMapper.selectById(1001L)).thenReturn(user);

        when(userMapper.updateUserBaseInfo(any(), anyLong())).thenReturn(1);

        BaseUserInfo base = new BaseUserInfo();
        base.setAvatar("https://assets.example.com/ai-tutor/avatars/1001/20260218/x.png");
        UserUpdateRequest dto = new UserUpdateRequest();
        dto.setBaseUserInfo(base);

        userService.updateUserInfo(dto, req);
        verify(userMapper, times(1)).updateUserBaseInfo(any(), eq(1001L));
    }

    @Test
    void updateUserInfoShouldUpdateTeacherDefaultGreeting() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("uid")).thenReturn("1001");

        User user = new User();
        user.setId(1001L);
        user.setPhone("13800006909");
        user.setUserType(UserRoleEnum.TEACHER.getValue());
        when(userMapper.selectById(1001L)).thenReturn(user);

        TeacherProfile profile = new TeacherProfile();
        profile.setUserId(1001L);
        profile.setRealName("张老师");
        profile.setEducation("本科");
        profile.setStatus(1);
        when(teacherProfileMapper.selectByUserId(1001L)).thenReturn(profile);

        when(userMapper.updateUserBaseInfo(any(), anyLong())).thenReturn(0);
        when(teacherProfileMapper.updateTeacherProfile(any(), anyLong())).thenReturn(1);

        TeacherExtInfo ext = new TeacherExtInfo();
        ext.setDefaultGreeting("你好");
        UserUpdateRequest dto = new UserUpdateRequest();
        dto.setTeacherExtInfo(ext);

        userService.updateUserInfo(dto, req);

        ArgumentCaptor<TeacherExtInfo> captor = ArgumentCaptor.forClass(TeacherExtInfo.class);
        verify(teacherProfileMapper, times(1)).updateTeacherProfile(captor.capture(), eq(1001L));
        assertThat(captor.getValue().getDefaultGreeting()).isEqualTo("你好");
    }

    @Test
    void updateUserInfoShouldMarkBasicCompletedWhenAvatarAndRealNamePresent() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("uid")).thenReturn("1001");

        User user = new User();
        user.setId(1001L);
        user.setPhone("13800006909");
        user.setUserType(UserRoleEnum.TEACHER.getValue());
        User latestUser = new User();
        latestUser.setId(1001L);
        latestUser.setPhone("13800006909");
        latestUser.setUserType(UserRoleEnum.TEACHER.getValue());
        latestUser.setAvatar("/avatars/avatar-1.svg");
        when(userMapper.selectById(1001L)).thenReturn(user, latestUser);
        when(userMapper.updateUserBaseInfo(any(), anyLong())).thenReturn(1);

        TeacherProfile profile = new TeacherProfile();
        profile.setUserId(1001L);
        profile.setRealName("张老师");
        profile.setStatus(1);
        when(teacherProfileMapper.selectByUserId(1001L)).thenReturn(profile, profile);
        when(teacherProfileMapper.updateTeacherProfile(any(), anyLong())).thenReturn(1);

        BaseUserInfo base = new BaseUserInfo();
        base.setAvatar("/avatars/avatar-1.svg");
        TeacherExtInfo ext = new TeacherExtInfo();
        ext.setRealName("张老师");

        UserUpdateRequest dto = new UserUpdateRequest();
        dto.setBaseUserInfo(base);
        dto.setTeacherExtInfo(ext);

        userService.updateUserInfo(dto, req);

        verify(teacherProfileMapper, times(1)).markBasicCompleted(1001L);
    }

    @Test
    void updateUserInfoShouldMarkResumeCompletedWhenAllResumeFieldsPresent() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("uid")).thenReturn("1001");

        User user = new User();
        user.setId(1001L);
        user.setPhone("13800006909");
        user.setUserType(UserRoleEnum.TEACHER.getValue());
        User latestUser = new User();
        latestUser.setId(1001L);
        latestUser.setPhone("13800006909");
        latestUser.setUserType(UserRoleEnum.TEACHER.getValue());
        latestUser.setAvatar("/avatars/avatar-1.svg");
        when(userMapper.selectById(1001L)).thenReturn(user, latestUser);
        when(userMapper.updateUserBaseInfo(any(), anyLong())).thenReturn(1);

        TeacherProfile existing = new TeacherProfile();
        existing.setUserId(1001L);
        existing.setStatus(1);
        TeacherProfile latest = new TeacherProfile();
        latest.setUserId(1001L);
        latest.setStatus(1);
        latest.setRealName("张老师");
        latest.setEducation("本科");
        latest.setCity("北京");
        latest.setHighestEduSchool("北京大学");
        latest.setIntroduction("自我介绍");
        latest.setSubject("数学,英语");

        when(teacherProfileMapper.selectByUserId(1001L)).thenReturn(existing, latest);
        when(teacherProfileMapper.updateTeacherProfile(any(), anyLong())).thenReturn(1);

        BaseUserInfo base = new BaseUserInfo();
        base.setAvatar("/avatars/avatar-1.svg");
        TeacherExtInfo ext = new TeacherExtInfo();
        ext.setRealName("张老师");
        ext.setEducation("本科");
        ext.setCity("北京");
        ext.setHighestEduSchool("北京大学");
        ext.setIntroduction("自我介绍");
        ext.setSubject("数学,英语");

        UserUpdateRequest dto = new UserUpdateRequest();
        dto.setBaseUserInfo(base);
        dto.setTeacherExtInfo(ext);

        userService.updateUserInfo(dto, req);

        verify(teacherProfileMapper, times(1)).markBasicCompleted(1001L);
        verify(teacherProfileMapper, times(1)).markResumeCompleted(1001L);
    }
}
