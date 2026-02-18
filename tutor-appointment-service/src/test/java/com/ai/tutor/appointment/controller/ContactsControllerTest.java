package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.common.integration.ImFacade;
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

/**
 * 联系人接口 HTTP 级别测试（MockMvc）。
 */
@SpringBootTest(classes = ContactsControllerTest.TestConfig.class)
@AutoConfigureMockMvc
class ContactsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImFacade imFacade;

    @MockBean
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        RequestInfo info = new RequestInfo();
        info.setUid(1001L);
        RequestHolder.set(info);
    }

    @AfterEach
    void tearDown() {
        RequestHolder.remove();
    }

    @Test
    void recentShouldReturnUsers() throws Exception {
        when(imFacade.listRecentContactUids(eq(1001L), eq(2))).thenReturn(List.of(1002L, 1003L));
        User u1 = new User();
        u1.setId(1002L);
        u1.setName("张三");
        u1.setUserType(2);
        User u2 = new User();
        u2.setId(1003L);
        u2.setName("李四");
        u2.setUserType(1);
        when(userMapper.selectByIds(anyList())).thenReturn(List.of(u1, u2));

        mockMvc.perform(get("/api/v1/contacts/recent").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1002))
                .andExpect(jsonPath("$.data[1].id").value(1003));
    }

    @Test
    void searchShouldMatchPathAndReturnUsers() throws Exception {
        User u = new User();
        u.setId(1002L);
        u.setName("张三");
        u.setUserType(2);
        when(userMapper.searchByKeyword(eq("张"), eq(10))).thenReturn(List.of(u));

        mockMvc.perform(get("/api/v1/contacts/search").param("q", "张").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].name").value("张三"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            SqlInitializationAutoConfiguration.class
    })
    @Import(ContactsController.class)
    static class TestConfig {
    }
}

