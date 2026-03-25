package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.ai.tutor.appointment.model.entity.TeacherProfile;
import com.ai.tutor.appointment.model.vo.UserCardVO;
import com.ai.tutor.appointment.model.vo.UserSimpleVO;
import com.ai.tutor.appointment.service.UserReadService;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = UserControllerCardTest.TestConfig.class)
@AutoConfigureMockMvc
class UserControllerCardTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SmsServiceImpl smsService;
    @MockBean
    private UserService userService;
    @MockBean
    private UserReadService userReadService;
    @MockBean
    private UserSettingsService userSettingsService;
    @MockBean
    private MinioProperties minioProperties;

    @Test
    void cardShouldReturnTeacherProfile() throws Exception {
        TeacherProfile profile = new TeacherProfile();
        profile.setUserId(1001L);
        profile.setDefaultGreeting("你好，欢迎咨询");
        UserCardVO card = UserCardVO.builder()
                .user(UserSimpleVO.builder()
                        .id(1001L)
                        .name("李老师")
                        .avatar("/avatars/default-avatar.svg")
                        .userType(UserRoleEnum.TEACHER.getValue())
                        .build())
                .teacherProfile(profile)
                .build();
        when(userReadService.getUserCard(1000L, 1001L)).thenReturn(card);

        mockMvc.perform(get("/user/card")
                        .requestAttr("uid", "1000")
                        .param("uid", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.user.id").value(1001))
                .andExpect(jsonPath("$.data.teacherProfile.defaultGreeting").value("你好，欢迎咨询"));
        verify(userReadService).getUserCard(1000L, 1001L);
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
