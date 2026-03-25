package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.entity.OrganizationProfile;
import com.ai.tutor.appointment.service.OrganizationPublicService;
import com.ai.tutor.common.handler.GlobalExceptionHandler;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
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

@SpringBootTest(classes = OrganizationPublicControllerTest.TestConfig.class)
@AutoConfigureMockMvc
class OrganizationPublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganizationPublicService organizationPublicService;

    @Test
    void getShouldReturnOrganizationProfile() throws Exception {
        OrganizationProfile profile = OrganizationProfile.builder()
                .id(10L)
                .userId(1001L)
                .orgName("星火教育")
                .build();
        when(organizationPublicService.getByOrgUserId(1001L)).thenReturn(profile);

        mockMvc.perform(get("/api/v1/public/organization/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orgName").value("星火教育"));
    }

    @Test
    void getShouldReturnNotFoundWhenProfileMissing() throws Exception {
        when(organizationPublicService.getByOrgUserId(999L))
                .thenThrow(new BusinessException(ErrorCode.NOT_FOUND_ERROR));

        mockMvc.perform(get("/api/v1/public/organization/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND_ERROR.getCode()));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            SqlInitializationAutoConfiguration.class
    })
    @Import({OrganizationPublicController.class, GlobalExceptionHandler.class})
    static class TestConfig {
    }
}
