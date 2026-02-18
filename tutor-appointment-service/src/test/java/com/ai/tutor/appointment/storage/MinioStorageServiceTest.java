package com.ai.tutor.appointment.storage;

import com.ai.tutor.exception.BusinessException;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MinioStorageServiceTest {

    private MinioClient minioClient;
    private MinioProperties minioProperties;
    private UploadProperties uploadProperties;
    private MinioStorageService service;

    @BeforeEach
    void setUp() {
        minioClient = mock(MinioClient.class);
        minioProperties = new MinioProperties();
        uploadProperties = new UploadProperties();

        minioProperties.setEnabled(true);
        minioProperties.setBucket("ai-tutor-assets");
        minioProperties.setPublicBaseUrl("https://assets.example.com/ai-tutor");

        service = new MinioStorageService(minioClient, minioProperties, uploadProperties);
    }

    @Test
    void shouldRejectWhenStorageDisabled() {
        minioProperties.setEnabled(false);
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});
        assertThatThrownBy(() -> service.uploadImage(AssetBiz.AVATAR, 1L, file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("对象存储未启用");
    }

    @Test
    void shouldRejectUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", new byte[]{1, 2, 3});
        assertThatThrownBy(() -> service.uploadImage(AssetBiz.AVATAR, 1L, file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的文件类型");
    }

    @Test
    void shouldUploadAndReturnUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});
        UploadResult r = service.uploadImage(AssetBiz.AVATAR, 1001L, file);

        assertThat(r.getObjectKey()).startsWith("avatars/1001/");
        assertThat(r.getUrl()).startsWith("https://assets.example.com/ai-tutor/avatars/1001/");
        assertThat(r.getContentType()).isEqualTo("image/png");
        assertThat(r.getSize()).isEqualTo(3);

        ArgumentCaptor<io.minio.PutObjectArgs> captor = ArgumentCaptor.forClass(io.minio.PutObjectArgs.class);
        verify(minioClient, times(1)).putObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("ai-tutor-assets");
        assertThat(captor.getValue().object()).isEqualTo(r.getObjectKey());
    }
}

