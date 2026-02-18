package com.ai.tutor.appointment.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * MinIO 配置。
 * 约定：
 * 1) endpoint 用于后端 SDK 访问（通常为内网地址）；
 * 2) publicBaseUrl 用于对外访问 URL 拼接（通常为 CDN/网关域名）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage.minio")
public class MinioProperties {

    /**
     * 是否启用对象存储能力。
     */
    private boolean enabled = false;

    /**
     * MinIO endpoint，例如：http://127.0.0.1:9000
     */
    private String endpoint;

    private String accessKey;

    private String secretKey;

    /**
     * 默认 bucket（建议 public read，用于对外图片访问）。
     */
    private String bucket = "ai-tutor-assets";

    /**
     * 对外资源访问前缀，例如：https://assets.example.com/ai-tutor
     */
    private String publicBaseUrl;

    /**
     * 启动时是否自动创建 bucket（仅建议在 dev 环境开启）。
     */
    private boolean autoCreateBucket = true;

    /**
     * 允许写入 user.avatar 的 URL 前缀白名单。
     * 目的：避免恶意外链/XSS/钓鱼链接写入用户资料。
     */
    private List<String> allowedAvatarUrlPrefixes = new ArrayList<>();
}

