package com.ai.tutor.admin.controller;

import com.ai.tutor.admin.AdminTestApplication;
import com.ai.tutor.admin.model.vo.AdminHomeCarouselItemVO;
import com.ai.tutor.admin.service.AdminHomeCarouselService;
import com.ai.tutor.admin.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AdminTestApplication.class)
@AutoConfigureMockMvc
public class AdminHomeCarouselControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminHomeCarouselService adminHomeCarouselService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    public void testList() throws Exception {
        AdminHomeCarouselItemVO item = AdminHomeCarouselItemVO.builder()
                .id(1L)
                .title("春季提分计划")
                .subtitle("覆盖重点科目")
                .imageUrl("/api/v1/public/assets/banners/demo.jpg")
                .sortOrder(1)
                .createTime(LocalDateTime.now())
                .build();

        when(adminHomeCarouselService.list()).thenReturn(List.of(item));
        when(jwtUtil.validateToken(any())).thenReturn(true);

        mockMvc.perform(get("/api/admin/home/carousel")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("春季提分计划"))
                .andExpect(jsonPath("$.data[0].imageUrl").value("/api/v1/public/assets/banners/demo.jpg"));
    }

    @Test
    public void testCreateMultipartUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "banner.jpg",
                "image/jpeg",
                "fake-image".getBytes()
        );

        AdminHomeCarouselItemVO created = AdminHomeCarouselItemVO.builder()
                .id(9L)
                .title("首页推荐")
                .subtitle("AI全程护航")
                .imageUrl("/api/v1/public/assets/banners/20260424/banner.jpg")
                .sortOrder(1)
                .createTime(LocalDateTime.now())
                .build();

        when(adminHomeCarouselService.create(eq("首页推荐"), eq("AI全程护航"), eq("/guide/student"), any(), anyLong()))
                .thenReturn(created);
        when(jwtUtil.validateToken(any())).thenReturn(true);

        mockMvc.perform(multipart("/api/admin/home/carousel")
                        .file(file)
                        .param("title", "首页推荐")
                        .param("subtitle", "AI全程护航")
                        .param("linkUrl", "/guide/student")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(9))
                .andExpect(jsonPath("$.data.imageUrl").value("/api/v1/public/assets/banners/20260424/banner.jpg"));
    }
}
