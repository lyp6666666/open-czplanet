package com.ai.tutor.admin.controller;

import com.ai.tutor.admin.storage.MinioProperties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class AssetPublicController {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @GetMapping("/api/v1/public/assets/{*objectKey}")
    public ResponseEntity<InputStreamResource> getAsset(@PathVariable("objectKey") String objectKey) {
        String key = normalizeObjectKey(objectKey);
        if (key.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(key)
                            .build()
            );
            InputStream in = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(key)
                            .build()
            );

            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            String contentType = stat.contentType();
            if (contentType != null && !contentType.isBlank()) {
                try {
                    mediaType = MediaType.parseMediaType(contentType);
                } catch (Exception ignored) {
                    mediaType = MediaType.APPLICATION_OCTET_STREAM;
                }
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .contentLength(stat.size())
                    .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                    .header(HttpHeaders.ETAG, stat.etag())
                    .body(new InputStreamResource(in));
        } catch (ErrorResponseException ex) {
            if (ex.errorResponse() != null && "NoSuchKey".equalsIgnoreCase(ex.errorResponse().code())) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.internalServerError().build();
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private static String normalizeObjectKey(String objectKey) {
        String key = objectKey == null ? "" : objectKey.trim();
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        if (key.contains("..")) {
            return "";
        }
        return key;
    }
}
