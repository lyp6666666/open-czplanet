package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.storage.AssetBiz;
import com.ai.tutor.appointment.storage.StorageService;
import com.ai.tutor.appointment.storage.UploadResult;
import com.ai.tutor.common.handler.GlobalExceptionHandler;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AssetControllerWebTest.TestConfig.class)
@AutoConfigureMockMvc
class AssetControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorageService storageService;

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
    void uploadShouldReturnOk() throws Exception {
        when(storageService.uploadImage(eq(AssetBiz.OTHER), eq(1001L), any()))
                .thenReturn(UploadResult.builder().objectKey("other/1001/x.png").url("http://x").contentType("image/png").size(1L).build());

        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});

        mockMvc.perform(multipart("/api/v1/assets/upload")
                        .file(file)
                        .param("biz", "other"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.url").value("http://x"));

        verify(storageService, times(1)).uploadImage(eq(AssetBiz.OTHER), eq(1001L), any());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            SqlInitializationAutoConfiguration.class
    })
    @Import({AssetController.class, GlobalExceptionHandler.class})
    static class TestConfig {
    }
}

