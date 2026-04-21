package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.service.TeacherVerificationService;
import com.ai.tutor.common.handler.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TeacherVerificationControllerTest.TestConfig.class)
@AutoConfigureMockMvc
class TeacherVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TeacherVerificationService teacherVerificationService;

    @Test
    void submitEducationShouldWorkForApiV1Prefix() throws Exception {
        mockMvc.perform(post("/api/v1/teacher/verification/education/submit")
                        .requestAttr("uid", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Req(List.of("http://x/a.png")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        verify(teacherVerificationService, times(1)).submitEducation(eq(1001L), eq(List.of("http://x/a.png")));
    }

    @Test
    void submitEducationShouldWorkForLegacyPrefix() throws Exception {
        mockMvc.perform(post("/teacher/verification/education/submit")
                        .requestAttr("uid", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Req(List.of("http://x/a.png")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        verify(teacherVerificationService, times(1)).submitEducation(eq(1001L), eq(List.of("http://x/a.png")));
    }

    @Test
    void submitRealnameShouldOnlyAcceptIdPhotoMode() throws Exception {
        mockMvc.perform(post("/api/v1/teacher/verification/realname/submit")
                        .requestAttr("uid", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"ID_PHOTO\",\"idFrontUrl\":\"http://x/front.png\",\"idBackUrl\":\"http://x/back.png\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        verify(teacherVerificationService, times(1))
                .submitRealnameIdPhoto(eq(1001L), eq("http://x/front.png"), eq("http://x/back.png"));
    }

    @Test
    void submitRealnameShouldRejectNameIdnoMode() throws Exception {
        mockMvc.perform(post("/api/v1/teacher/verification/realname/submit")
                        .requestAttr("uid", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"NAME_IDNO\",\"realName\":\"张老师\",\"idNo\":\"110101199001011234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));

        verifyNoMoreInteractions(teacherVerificationService);
    }

    private static class Req {
        public List<String> proofUrls;

        public Req(List<String> proofUrls) {
            this.proofUrls = proofUrls;
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            SqlInitializationAutoConfiguration.class
    })
    @Import({TeacherVerificationController.class, GlobalExceptionHandler.class})
    static class TestConfig {
    }
}
