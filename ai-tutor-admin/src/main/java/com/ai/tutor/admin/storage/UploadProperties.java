package com.ai.tutor.admin.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "storage.upload")
public class UploadProperties {
    private List<String> allowedContentTypes = new ArrayList<>(List.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/svg+xml"
    ));

    /**
     * 管理端轮播图不做应用层大小限制。
     * 小于等于 0 代表不限制。
     */
    private long maxSizeBytes = -1L;
}
