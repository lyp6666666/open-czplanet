package com.ai.tutor.appointment.storage;

import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import io.minio.PutObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * MinIO 存储实现。
 * 约定：只负责“上传与生成可访问 URL”，不绑定具体业务字段落库。
 */
@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final UploadProperties uploadProperties;

    @Override
    public UploadResult uploadImage(AssetBiz biz, Long uid, MultipartFile file) {
        ThrowUtils.throwIf(!minioProperties.isEnabled(), ErrorCode.OPERATION_ERROR, "对象存储未启用");
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "文件不能为空");
        ThrowUtils.throwIf(biz == null, ErrorCode.PARAMS_ERROR, "biz 不能为空");

        String contentType = normalizeContentType(file.getContentType());
        ThrowUtils.throwIf(contentType == null, ErrorCode.PARAMS_ERROR, "无法识别文件类型");
        ThrowUtils.throwIf(!uploadProperties.getAllowedContentTypes().contains(contentType), ErrorCode.PARAMS_ERROR, "不支持的文件类型");

        long maxSize = biz == AssetBiz.AVATAR ? uploadProperties.getAvatarMaxSizeBytes() : uploadProperties.getMaxSizeBytes();
        ThrowUtils.throwIf(file.getSize() > maxSize, ErrorCode.PARAMS_ERROR, "文件过大");

        String ext = guessExt(contentType);
        ThrowUtils.throwIf(ext == null, ErrorCode.PARAMS_ERROR, "不支持的图片格式");

        String objectKey = buildObjectKey(biz, uid, ext);
        String url = joinUrl(minioProperties.getPublicBaseUrl(), objectKey);

        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(contentType)
                    .headers(Map.of("Cache-Control", cacheControlForBiz(biz)))
                    .build());
        } catch (Exception e) {
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "上传失败");
        }

        return UploadResult.builder()
                .objectKey(objectKey)
                .url(url)
                .contentType(contentType)
                .size(file.getSize())
                .build();
    }

    private static String normalizeContentType(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String cacheControlForBiz(AssetBiz biz) {
        if (biz == AssetBiz.BANNER) {
            return "public, max-age=604800";
        }
        if (biz == AssetBiz.AVATAR) {
            return "public, max-age=3600";
        }
        return "public, max-age=86400";
    }

    private static String guessExt(String contentType) {
        if ("image/jpeg".equals(contentType)) {
            return "jpg";
        }
        if ("image/png".equals(contentType)) {
            return "png";
        }
        if ("image/webp".equals(contentType)) {
            return "webp";
        }
        if ("image/gif".equals(contentType)) {
            return "gif";
        }
        if ("image/svg+xml".equals(contentType)) {
            return "svg";
        }
        return null;
    }

    private static String buildObjectKey(AssetBiz biz, Long uid, String ext) {
        String day = LocalDate.now().format(DAY_FMT);
        String id = UUID.randomUUID().toString().replace("-", "");
        if (biz == AssetBiz.AVATAR) {
            return "avatars/" + (uid == null ? "anonymous" : uid) + "/" + day + "/" + id + "." + ext;
        }
        if (biz == AssetBiz.BANNER) {
            return "banners/" + day + "/" + id + "." + ext;
        }
        if (biz == AssetBiz.POST) {
            return "uploads/post/" + day + "/" + id + "." + ext;
        }
        return "uploads/other/" + day + "/" + id + "." + ext;
    }

    private static String joinUrl(String base, String path) {
        if (base == null || base.isBlank()) {
            return path;
        }
        String b = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String p = path.startsWith("/") ? path.substring(1) : path;
        return b + "/" + p;
    }
}
