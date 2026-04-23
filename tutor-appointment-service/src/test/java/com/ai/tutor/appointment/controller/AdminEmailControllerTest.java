package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.vo.admin.EmailTaskDetailVO;
import com.ai.tutor.appointment.model.vo.admin.EmailTaskRowVO;
import com.ai.tutor.appointment.model.vo.admin.PageResult;
import com.ai.tutor.appointment.model.vo.email.InternalUserEmailsVO;
import com.ai.tutor.appointment.service.EmailAccountService;
import com.ai.tutor.appointment.service.EmailAdminService;
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

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AdminEmailControllerTest.TestConfig.class)
@AutoConfigureMockMvc
class AdminEmailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmailAdminService emailAdminService;
    @MockBean
    private EmailAccountService emailAccountService;

    @Test
    void pageTasksShouldReturnPagedRows() throws Exception {
        when(emailAdminService.pageTasks(1, 20, 1001L, null, null, null, null)).thenReturn(
                PageResult.<EmailTaskRowVO>builder()
                        .records(List.of(EmailTaskRowVO.builder().id(1L).templateCode("EMAIL_VERIFY_CODE").status("SENT").build()))
                        .total(1)
                        .page(1)
                        .size(20)
                        .build()
        );

        mockMvc.perform(get("/admin/email/tasks").param("userId", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].id").value(1))
                .andExpect(jsonPath("$.data.records[0].templateCode").value("EMAIL_VERIFY_CODE"));
    }

    @Test
    void retryTaskShouldReturnTrue() throws Exception {
        when(emailAdminService.retryTask(9L)).thenReturn(true);
        mockMvc.perform(post("/admin/email/tasks/9/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void taskDetailShouldReturnData() throws Exception {
        when(emailAdminService.getTaskDetail(8L)).thenReturn(EmailTaskDetailVO.builder()
                .task(EmailTaskRowVO.builder().id(8L).status("FAILED").build())
                .build());
        mockMvc.perform(get("/admin/email/tasks/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.task.id").value(8))
                .andExpect(jsonPath("$.data.task.status").value("FAILED"));
    }

    @Test
    void userEmailsShouldReturnInternalView() throws Exception {
        InternalUserEmailsVO vo = InternalUserEmailsVO.builder().userId(1001L).build();
        when(emailAccountService.getInternalUserEmails(1001L)).thenReturn(vo);
        mockMvc.perform(get("/admin/email/users/1001/emails"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(1001));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            SqlInitializationAutoConfiguration.class
    })
    @Import(AdminEmailController.class)
    static class TestConfig {
    }
}
