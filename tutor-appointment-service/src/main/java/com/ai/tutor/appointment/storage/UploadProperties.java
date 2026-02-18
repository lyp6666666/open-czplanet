package com.ai.tutor.appointment.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 上传约束配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage.upload")
public class UploadProperties {

    /**
     * 允许的图片类型（Content-Type）。
     */
    private List<String> allowedContentTypes = new ArrayList<>(List.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/svg+xml"
    ));

    /**
     * 默认最大上传大小（字节）。
     */
    private long maxSizeBytes = 5L * 1024 * 1024;

    /**
     * 头像最大上传大小（字节）。
     */
    private long avatarMaxSizeBytes = 2L * 1024 * 1024;
}

