package com.ai.tutor.admin.controller;

import com.ai.tutor.admin.AdminTestApplication;
import com.ai.tutor.admin.storage.MinioProperties;
import com.ai.tutor.admin.utils.JwtUtil;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.StatObjectResponse;
import okhttp3.Headers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AdminTestApplication.class)
@AutoConfigureMockMvc
class AssetPublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MinioClient minioClient;

    @MockBean
    private MinioProperties minioProperties;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void getAssetShouldReturnImageContent() throws Exception {
        byte[] bytes = "abc".getBytes(StandardCharsets.UTF_8);
        Headers headers = new Headers.Builder()
                .add("Content-Type", "image/jpeg")
                .add("Content-Length", String.valueOf(bytes.length))
                .add("ETag", "etag-1")
                .add("Last-Modified", "Sun, 19 Apr 2026 00:34:02 GMT")
                .build();
        StatObjectResponse stat = new StatObjectResponse(headers, "region", "ai-tutor-assets", "uploads/other/20260418/a.jpg");
        GetObjectResponse object = new GetObjectResponse(
                headers,
                "region",
                "ai-tutor-assets",
                "uploads/other/20260418/a.jpg",
                new ByteArrayInputStream(bytes)
        );

        when(minioProperties.getBucket()).thenReturn("ai-tutor-assets");
        when(minioClient.statObject(any())).thenReturn(stat);
        when(minioClient.getObject(any())).thenReturn(object);

        mockMvc.perform(get("/api/v1/public/assets/uploads/other/20260418/a.jpg"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"etag-1\""))
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(bytes));
    }

    @Test
    void getAssetShouldRejectPathTraversal() throws Exception {
        mockMvc.perform(get("/api/v1/public/assets/../secret.txt"))
                .andExpect(status().isNotFound());
    }
}
