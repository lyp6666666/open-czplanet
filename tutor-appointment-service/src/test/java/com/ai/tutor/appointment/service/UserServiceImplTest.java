package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.enums.RedisKeyPrefix;
import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.ai.tutor.appointment.mapper.StudentProfileMapper;
import com.ai.tutor.appointment.mapper.TeacherProfileMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.entity.StudentProfile;
import com.ai.tutor.appointment.model.entity.TeacherProfile;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.model.vo.LoginUserVO;
import com.ai.tutor.appointment.service.impl.UserServiceImpl;
import com.ai.tutor.appointment.utils.JwtUtil;
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

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
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

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
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

        LoginUserVO vo = userService.userLoginOrRegister(phone, "1234", UserRoleEnum.TEACHER);
        assertThat(vo.getId()).isEqualTo(1001L);
        assertThat(vo.getToken()).isEqualTo("token");
        assertThat(vo.getUserType()).isEqualTo(UserRoleEnum.TEACHER.getValue());

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

        LoginUserVO vo = userService.userLoginOrRegister(phone, "1234", UserRoleEnum.STUDENT);
        assertThat(vo.getId()).isEqualTo(2002L);
        assertThat(vo.getUserType()).isEqualTo(UserRoleEnum.STUDENT.getValue());

        ArgumentCaptor<User> inserted = ArgumentCaptor.forClass(User.class);
        verify(userMapper, times(1)).insert(inserted.capture());
        assertThat(inserted.getValue().getPhone()).isEqualTo(phone);
        assertThat(inserted.getValue().getName()).isEqualTo("用户1234");

        verify(studentProfileMapper, times(1)).insert(any(StudentProfile.class));
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

        LoginUserVO vo = userService.userLoginOrRegister(phone, "1234", UserRoleEnum.STUDENT);
        assertThat(vo.getId()).isEqualTo(3003L);
        assertThat(vo.getToken()).isEqualTo("token3");

        verify(studentProfileMapper, times(1)).insert(any(StudentProfile.class));
    }

    @Test
    void shouldRetryWithDifferentNameWhenUniqNameConflicts() {
        String phone = "13800005678";
        when(smsService.verifyCode(eq(phone), anyString(), anyString())).thenReturn(true);

        when(userMapper.selectByPhone(phone)).thenReturn(null);
        doThrow(new DuplicateKeyException("dup"))
                .doAnswer(inv -> {
                    User u = inv.getArgument(0);
                    u.setId(4004L);
                    return 1;
                })
                .when(userMapper)
                .insert(any(User.class));

        when(studentProfileMapper.selectByUserId(4004L)).thenReturn(null);
        when(userMapper.updateUserType(4004L, UserRoleEnum.STUDENT.getValue())).thenReturn(1);
        when(jwtUtil.generateToken(4004L, phone, UserRoleEnum.STUDENT)).thenReturn("token4");

        LoginUserVO vo = userService.userLoginOrRegister(phone, "1234", UserRoleEnum.STUDENT);
        assertThat(vo.getId()).isEqualTo(4004L);

        ArgumentCaptor<User> inserted = ArgumentCaptor.forClass(User.class);
        verify(userMapper, times(2)).insert(inserted.capture());
        assertThat(inserted.getAllValues().get(1).getName()).startsWith("用户5678-");
    }
}
