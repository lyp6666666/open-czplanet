package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.vo.home.HomeGuestVOs;
import com.ai.tutor.appointment.service.HomeGuestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = HomeGuestControllerTest.TestConfig.class)
@AutoConfigureMockMvc
class HomeGuestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HomeGuestService homeGuestService;

    @Test
    void configShouldReturnOk() throws Exception {
        when(homeGuestService.getHomeConfig("北京"))
                .thenReturn(new HomeGuestVOs.HomeConfigVO("北京", true, null, List.of(), null));

        mockMvc.perform(get("/api/v1/public/home/config").param("city", "北京"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.defaultCity").value("北京"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            SqlInitializationAutoConfiguration.class
    })
    @Import(HomeGuestController.class)
    static class TestConfig {
    }
}
