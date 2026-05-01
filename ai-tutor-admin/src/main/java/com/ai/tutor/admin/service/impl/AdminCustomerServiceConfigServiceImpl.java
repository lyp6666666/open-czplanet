package com.ai.tutor.admin.service.impl;

import com.ai.tutor.admin.config.CustomerServiceProperties;
import com.ai.tutor.admin.mapper.AdminCustomerServiceConfigMapper;
import com.ai.tutor.admin.model.dto.AdminCustomerServiceConfigRequest;
import com.ai.tutor.admin.model.entity.CustomerServiceConfig;
import com.ai.tutor.admin.model.vo.AdminCustomerServiceConfigVO;
import com.ai.tutor.admin.service.AdminCustomerServiceConfigService;
import com.ai.tutor.admin.storage.MinioProperties;
import com.ai.tutor.admin.storage.UploadProperties;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminCustomerServiceConfigServiceImpl implements AdminCustomerServiceConfigService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Set<String> CHANNEL_TYPES = Set.of("WECHAT_PERSONAL", "WECHAT_WORK");

    @Resource
    private AdminCustomerServiceConfigMapper mapper;
    @Resource
    private CustomerServiceProperties properties;
    @Resource
    private MinioClient minioClient;
    @Resource
    private MinioProperties minioProperties;
    @Resource
    private UploadProperties uploadProperties;

    @Override
    public AdminCustomerServiceConfigVO config() {
        CustomerServiceConfig config = currentConfig();
        return toVo(config);
    }

    @Override
    public AdminCustomerServiceConfigVO save(AdminCustomerServiceConfigRequest request, Long adminUid) {
        ThrowUtils.throwIf(adminUid == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "客服配置不能为空");

        CustomerServiceConfig config = currentConfig();
        config.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        config.setChannelType(normalizeChannelType(request.getChannelType()));
        config.setDisplayName(trimOrDefault(request.getDisplayName(), "客服"));
        config.setWechatNo(trimNullable(request.getWechatNo(), 80));
        config.setQqNo(trimNullable(request.getQqNo(), 32));
        config.setQrCodeObjectKey(trimNullable(request.getQrCodeObjectKey(), 255));
        config.setServiceTime(trimOrDefault(request.getServiceTime(), "09:00 - 22:00"));
        config.setDescription(trimNullable(request.getDescription(), 255));
        config.setUpdateAdminId(adminUid);

        upsert(config);
        return config();
    }

    @Override
    public AdminCustomerServiceConfigVO uploadQrCode(MultipartFile file, Long adminUid) {
        ThrowUtils.throwIf(adminUid == null, ErrorCode.NOT_LOGIN_ERROR);
        String objectKey = uploadImage(file);
        CustomerServiceConfig config = currentConfig();
        config.setQrCodeObjectKey(objectKey);
        config.setUpdateAdminId(adminUid);
        upsert(config);
        return config();
    }

    private CustomerServiceConfig currentConfig() {
        CustomerServiceConfig row = mapper.selectSingleton();
        if (row != null) {
            return normalize(row);
        }
        CustomerServiceConfig defaults = defaults();
        mapper.insertSingleton(defaults);
        return normalize(mapper.selectSingleton());
    }

    private void upsert(CustomerServiceConfig config) {
        if (mapper.selectSingleton() == null) {
            mapper.insertSingleton(config);
            return;
        }
        int updated = mapper.updateSingleton(config);
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR, "客服配置保存失败");
    }

    private CustomerServiceConfig defaults() {
        CustomerServiceConfig config = new CustomerServiceConfig();
        config.setEnabled(Boolean.TRUE.equals(properties.getEnabled()));
        config.setChannelType(normalizeChannelType(properties.getChannelType()));
        config.setDisplayName(trimOrDefault(properties.getDisplayName(), "创智星球客服"));
        config.setWechatNo(trimNullable(properties.getWechatNo(), 80));
        config.setQqNo(trimNullable(properties.getQqNo(), 32));
        config.setQrCodeObjectKey(trimNullable(properties.getQrCodeObjectKey(), 255));
        config.setServiceTime(trimOrDefault(properties.getServiceTime(), "09:00 - 22:00"));
        config.setDescription(trimNullable(properties.getDescription(), 255));
        return config;
    }

    private CustomerServiceConfig normalize(CustomerServiceConfig config) {
        if (config == null) {
            return defaults();
        }
        config.setEnabled(Boolean.TRUE.equals(config.getEnabled()));
        config.setChannelType(normalizeChannelType(config.getChannelType()));
        config.setDisplayName(trimOrDefault(config.getDisplayName(), "客服"));
        config.setWechatNo(trimNullable(config.getWechatNo(), 80));
        config.setQqNo(trimNullable(config.getQqNo(), 32));
        config.setQrCodeObjectKey(trimNullable(config.getQrCodeObjectKey(), 255));
        config.setServiceTime(trimOrDefault(config.getServiceTime(), "09:00 - 22:00"));
        config.setDescription(trimNullable(config.getDescription(), 255));
        return config;
    }

    private AdminCustomerServiceConfigVO toVo(CustomerServiceConfig config) {
        return AdminCustomerServiceConfigVO.builder()
                .enabled(config.getEnabled())
                .channelType(config.getChannelType())
                .displayName(config.getDisplayName())
                .wechatNo(config.getWechatNo())
                .qqNo(config.getQqNo())
                .qrCodeUrl(buildPublicAssetUrl(config.getQrCodeObjectKey()))
                .qrCodeObjectKey(config.getQrCodeObjectKey())
                .serviceTime(config.getServiceTime())
                .description(config.getDescription())
                .updateTime(config.getUpdateTime())
                .build();
    }

    private String uploadImage(MultipartFile file) {
        ThrowUtils.throwIf(!minioProperties.isEnabled(), ErrorCode.OPERATION_ERROR, "对象存储未启用");
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "请先选择二维码图片");
        String contentType = normalizeContentType(file.getContentType());
        ThrowUtils.throwIf(contentType == null || !uploadProperties.getAllowedContentTypes().contains(contentType), ErrorCode.PARAMS_ERROR, "不支持的图片类型");
        long maxSizeBytes = uploadProperties.getMaxSizeBytes();
        ThrowUtils.throwIf(maxSizeBytes > 0 && file.getSize() > maxSizeBytes, ErrorCode.PARAMS_ERROR, "文件过大");

        String ext = guessExt(contentType);
        ThrowUtils.throwIf(ext == null, ErrorCode.PARAMS_ERROR, "不支持的图片格式");
        String objectKey = "customer-service/" + LocalDate.now().format(DAY_FMT) + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;

        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "二维码上传失败");
        }
        return objectKey;
    }

    private static String normalizeChannelType(String raw) {
        String v = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        return CHANNEL_TYPES.contains(v) ? v : "WECHAT_WORK";
    }

    private static String trimOrDefault(String raw, String defaultValue) {
        String v = raw == null ? "" : raw.trim();
        return v.isEmpty() ? defaultValue : v;
    }

    private static String trimNullable(String raw, int maxLength) {
        String v = raw == null ? "" : raw.trim();
        ThrowUtils.throwIf(v.length() > maxLength, ErrorCode.PARAMS_ERROR, "字段长度不能超过 " + maxLength);
        return StringUtils.hasText(v) ? v : null;
    }

    private static String normalizeContentType(String raw) {
        return raw == null ? null : raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String guessExt(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            case "image/svg+xml" -> "svg";
            default -> null;
        };
    }

    private static String buildPublicAssetUrl(String objectKey) {
        String key = objectKey == null ? "" : objectKey.trim();
        if (key.isEmpty()) {
            return null;
        }
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        return "/api/v1/public/assets/" + key;
    }
}
