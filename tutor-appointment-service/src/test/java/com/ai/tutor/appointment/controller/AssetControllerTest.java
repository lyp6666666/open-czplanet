package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.storage.AssetBiz;
import com.ai.tutor.appointment.storage.StorageService;
import com.ai.tutor.appointment.storage.UploadResult;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.utils.RequestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AssetControllerTest {

    private StorageService storageService;
    private AssetController controller;

    @BeforeEach
    void setUp() {
        storageService = mock(StorageService.class);
        controller = new AssetController();
        ReflectionTestUtils.setField(controller, "storageService", storageService);

        RequestInfo info = new RequestInfo();
        info.setUid(1001L);
        RequestHolder.set(info);
    }

    @AfterEach
    void tearDown() {
        RequestHolder.remove();
    }

    @Test
    void shouldRejectWhenNotLogin() {
        RequestHolder.remove();
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
        assertThatThrownBy(() -> controller.upload(file, "avatar"))
                .isInstanceOf(BusinessException.class);
        verify(storageService, never()).uploadImage(any(), any(), any());
    }

    @Test
    void shouldCallStorageService() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
        when(storageService.uploadImage(eq(AssetBiz.AVATAR), eq(1001L), any()))
                .thenReturn(UploadResult.builder().objectKey("avatars/1001/x.png").url("u").build());

        BaseResponse<UploadResult> resp = controller.upload(file, "avatar");
        assertThat(resp.getCode()).isEqualTo(0);
        assertThat(resp.getData()).isNotNull();
        assertThat(resp.getData().getObjectKey()).isEqualTo("avatars/1001/x.png");
        verify(storageService, times(1)).uploadImage(eq(AssetBiz.AVATAR), eq(1001L), any());
    }
}

