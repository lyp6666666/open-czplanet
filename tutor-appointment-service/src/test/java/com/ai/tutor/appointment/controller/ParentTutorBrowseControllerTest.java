package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.model.vo.parent.ParentTutorVOs;
import com.ai.tutor.appointment.service.ParentTutorBrowseService;
import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.utils.RequestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ParentTutorBrowseControllerTest.TestConfig.class)
@AutoConfigureMockMvc
class ParentTutorBrowseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ParentTutorBrowseService parentTutorBrowseService;

    @BeforeEach
    void setUp() {
        RequestInfo info = new RequestInfo();
        info.setUid(2001L);
        RequestHolder.set(info);
    }

    @AfterEach
    void tearDown() {
        RequestHolder.remove();
    }

    @Test
    void pageShouldReturnOk() throws Exception {
        ParentTutorVOs.TutorCardVO item = new ParentTutorVOs.TutorCardVO(
                1001L,
                "张老师",
                null,
                "北京",
                "本科",
                3,
                "120",
                List.of("数学"),
                List.of("响应快"),
                "简介"
        );
        when(parentTutorBrowseService.pageTutors(eq(2001L), any(), any(), any(), any(), any(), any()))
                .thenReturn(new CursorPageResponse<>(10L, true, List.of(item)));

        mockMvc.perform(get("/api/v1/parent/tutors/page")
                        .param("q", "数学")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].userId").value(1001))
                .andExpect(jsonPath("$.data.list[0].displayName").value("张老师"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            SqlInitializationAutoConfiguration.class
    })
    @Import(ParentTutorBrowseController.class)
    static class TestConfig {
    }
}
