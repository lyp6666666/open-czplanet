package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.ai.tutor.appointment.mapper.StudentJobPostingMapper;
import com.ai.tutor.appointment.mapper.StudentProfileMapper;
import com.ai.tutor.appointment.mapper.TeacherProfileMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.entity.TeacherProfile;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.service.UserService;
import com.ai.tutor.appointment.service.UserSettingsService;
import com.ai.tutor.appointment.service.impl.SmsServiceImpl;
import com.ai.tutor.appointment.storage.MinioProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = UserControllerMeTest.TestConfig.class)
@AutoConfigureMockMvc
class UserControllerMeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SmsServiceImpl smsService;
    @MockBean
    private UserService userService;
    @MockBean
    private UserMapper userMapper;
    @MockBean
    private TeacherProfileMapper teacherProfileMapper;
    @MockBean
    private StudentProfileMapper studentProfileMapper;
    @MockBean
    private StudentJobPostingMapper studentJobPostingMapper;
    @MockBean
    private UserSettingsService userSettingsService;
    @MockBean
    private MinioProperties minioProperties;

    @Test
    void meShouldReturnDefaultGreetingForTeacher() throws Exception {
        User user = new User();
        user.setId(1001L);
        user.setUserType(UserRoleEnum.TEACHER.getValue());
        user.setName("张老师");
        user.setPhone("13800006909");
        when(userMapper.selectById(1001L)).thenReturn(user);

        TeacherProfile profile = new TeacherProfile();
        profile.setUserId(1001L);
        profile.setDefaultGreeting("你好");
        when(teacherProfileMapper.selectByUserId(1001L)).thenReturn(profile);

        mockMvc.perform(get("/user/me").requestAttr("uid", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.teacherProfile.defaultGreeting").value("你好"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            SqlInitializationAutoConfiguration.class
    })
    @Import(UserController.class)
    static class TestConfig {
    }
}
