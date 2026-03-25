package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.vo.UserSimpleVO;
import com.ai.tutor.appointment.service.ContactQueryService;
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

import static org.mockito.Mockito.verify;
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
    private ContactQueryService contactQueryService;

    @BeforeEach
    void setUp() {
        RequestInfo info = new RequestInfo();
        info.setUid(1001L);
        info.setRole(1);
        RequestHolder.set(info);
    }

    @AfterEach
    void tearDown() {
        RequestHolder.remove();
    }

    @Test
    void recentShouldReturnUsers() throws Exception {
        UserSimpleVO u1 = UserSimpleVO.builder().id(1002L).name("张三").userType(2).build();
        UserSimpleVO u2 = UserSimpleVO.builder().id(1003L).name("李四").userType(1).build();
        when(contactQueryService.recentContacts(1001L, 2)).thenReturn(List.of(u1, u2));

        mockMvc.perform(get("/api/v1/contacts/recent").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1002))
                .andExpect(jsonPath("$.data[1].id").value(1003));
        verify(contactQueryService).recentContacts(1001L, 2);
    }

    @Test
    void searchShouldMatchPathAndReturnUsers() throws Exception {
        UserSimpleVO u = UserSimpleVO.builder().id(1002L).name("张三").userType(2).build();
        when(contactQueryService.searchContacts(1001L, 1, "张", 10)).thenReturn(List.of(u));

        mockMvc.perform(get("/api/v1/contacts/search").param("q", "张").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].name").value("张三"));
        verify(contactQueryService).searchContacts(1001L, 1, "张", 10);
    }

    @Test
    void searchShouldFilterSelfAndNonTargetRole() throws Exception {
        UserSimpleVO student = UserSimpleVO.builder().id(1002L).name("张三").userType(2).build();
        when(contactQueryService.searchContacts(1001L, 1, "1", 50)).thenReturn(List.of(student));

        mockMvc.perform(get("/api/v1/contacts/search").param("q", "1").param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(1002));
        verify(contactQueryService).searchContacts(1001L, 1, "1", 50);
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
