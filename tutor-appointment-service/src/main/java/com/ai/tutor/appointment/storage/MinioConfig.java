package com.ai.tutor.appointment.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO Client 初始化与基础校验。
 */
@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    private final MinioProperties minioProperties;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    @PostConstruct
    public void ensureBucket() {
        if (!minioProperties.isEnabled()) {
            return;
        }
        if (minioProperties.getEndpoint() == null || minioProperties.getAccessKey() == null || minioProperties.getSecretKey() == null) {
            return;
        }
        try {
            MinioClient client = minioClient();
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getBucket()).build());
            if (!exists && minioProperties.isAutoCreateBucket()) {
                client.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucket()).build());
            }
        } catch (Exception ignored) {
            // 启动阶段不强制失败：避免开发环境因 MinIO 未启动导致整个服务不可用。
            // 真正上传时仍会进行 enabled+配置校验，并在失败时返回明确错误。
        }
    }
}

