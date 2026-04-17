package com.ai.tutor.admin.service.impl;

import com.ai.tutor.admin.mapper.AdminHomeCarouselMapper;
import com.ai.tutor.admin.model.entity.HomeCarouselConfig;
import com.ai.tutor.admin.model.vo.AdminHomeCarouselItemVO;
import com.ai.tutor.admin.service.AdminHomeCarouselService;
import com.ai.tutor.admin.storage.MinioProperties;
import com.ai.tutor.admin.storage.UploadProperties;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AdminHomeCarouselServiceImpl implements AdminHomeCarouselService {

    private static final int MAX_ITEMS = 5;
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Resource
    private AdminHomeCarouselMapper adminHomeCarouselMapper;
    @Resource
    private MinioClient minioClient;
    @Resource
    private MinioProperties minioProperties;
    @Resource
    private UploadProperties uploadProperties;

    @Override
    public List<AdminHomeCarouselItemVO> list() {
        return adminHomeCarouselMapper.selectAll().stream().map(this::toVo).toList();
    }

    @Override
    public AdminHomeCarouselItemVO create(String title, String subtitle, String linkUrl, MultipartFile file, Long adminUid) {
        ThrowUtils.throwIf(adminUid == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(adminHomeCarouselMapper.countAll() >= MAX_ITEMS, ErrorCode.OPERATION_ERROR, "轮播图最多同时配置 5 张");

        HomeCarouselConfig config = new HomeCarouselConfig();
        config.setTitle(normalizeTitle(title));
        config.setSubtitle(normalizeSubtitle(subtitle));
        config.setLinkUrl(normalizeLinkUrl(linkUrl));
        config.setLinkType(config.getLinkUrl() == null ? "NONE" : (config.getLinkUrl().startsWith("/") ? "ROUTE" : "URL"));
        config.setImageObjectKey(uploadBanner(file));
        config.setSortOrder((int) adminHomeCarouselMapper.countAll() + 1);
        config.setCreateAdminId(adminUid);
        config.setUpdateAdminId(adminUid);
        adminHomeCarouselMapper.insert(config);
        return toVo(config);
    }

    @Override
    public void delete(Long id, Long adminUid) {
        ThrowUtils.throwIf(adminUid == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "轮播图不存在");
        HomeCarouselConfig current = adminHomeCarouselMapper.selectById(id);
        ThrowUtils.throwIf(current == null, ErrorCode.PARAMS_ERROR, "轮播图不存在");
        adminHomeCarouselMapper.deleteById(id);
        removeObjectQuietly(current.getImageObjectKey());
        resequence(adminUid);
    }

    private void resequence(Long adminUid) {
        List<HomeCarouselConfig> rows = adminHomeCarouselMapper.selectAll();
        int sort = 1;
        for (HomeCarouselConfig row : rows) {
            if (row.getSortOrder() != sort) {
                adminHomeCarouselMapper.updateSortOrder(row.getId(), sort, adminUid);
            }
            sort += 1;
        }
    }

    private AdminHomeCarouselItemVO toVo(HomeCarouselConfig config) {
        return AdminHomeCarouselItemVO.builder()
                .id(config.getId())
                .title(config.getTitle())
                .subtitle(config.getSubtitle())
                .imageUrl(buildPublicAssetUrl(config.getImageObjectKey()))
                .objectKey(config.getImageObjectKey())
                .linkType(config.getLinkType())
                .linkUrl(config.getLinkUrl())
                .sortOrder(config.getSortOrder())
                .createTime(config.getCreateTime())
                .updateTime(config.getUpdateTime())
                .build();
    }

    private String uploadBanner(MultipartFile file) {
        ThrowUtils.throwIf(!minioProperties.isEnabled(), ErrorCode.OPERATION_ERROR, "对象存储未启用");
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "请先选择图片");
        String contentType = normalizeContentType(file.getContentType());
        ThrowUtils.throwIf(contentType == null, ErrorCode.PARAMS_ERROR, "无法识别文件类型");
        ThrowUtils.throwIf(!uploadProperties.getAllowedContentTypes().contains(contentType), ErrorCode.PARAMS_ERROR, "不支持的文件类型");
        ThrowUtils.throwIf(file.getSize() > uploadProperties.getMaxSizeBytes(), ErrorCode.PARAMS_ERROR, "文件过大");

        String ext = guessExt(contentType);
        ThrowUtils.throwIf(ext == null, ErrorCode.PARAMS_ERROR, "不支持的图片格式");
        String objectKey = "banners/" + LocalDate.now().format(DAY_FMT) + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;

        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "上传失败");
        }
        return objectKey;
    }

    private void removeObjectQuietly(String objectKey) {
        String key = objectKey == null ? "" : objectKey.trim();
        if (key.isEmpty() || !minioProperties.isEnabled()) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(key)
                    .build());
        } catch (Exception ignored) {
        }
    }

    private static String normalizeTitle(String raw) {
        String v = raw == null ? "" : raw.trim();
        ThrowUtils.throwIf(v.isEmpty(), ErrorCode.PARAMS_ERROR, "请输入主标题");
        ThrowUtils.throwIf(v.length() > 80, ErrorCode.PARAMS_ERROR, "主标题不能超过 80 个字符");
        return v;
    }

    private static String normalizeSubtitle(String raw) {
        String v = raw == null ? "" : raw.trim();
        ThrowUtils.throwIf(v.length() > 160, ErrorCode.PARAMS_ERROR, "副标题不能超过 160 个字符");
        return v.isEmpty() ? null : v;
    }

    private static String normalizeLinkUrl(String raw) {
        String v = raw == null ? "" : raw.trim();
        ThrowUtils.throwIf(v.length() > 255, ErrorCode.PARAMS_ERROR, "跳转地址不能超过 255 个字符");
        if (v.isEmpty()) {
            return null;
        }
        boolean isRoute = v.startsWith("/");
        boolean isHttp = v.startsWith("http://") || v.startsWith("https://");
        ThrowUtils.throwIf(!isRoute && !isHttp, ErrorCode.PARAMS_ERROR, "跳转地址需以 /、http:// 或 https:// 开头");
        return v;
    }

    private static String normalizeContentType(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.trim().toLowerCase(Locale.ROOT);
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
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        return "/api/v1/public/assets/" + key;
    }
}
