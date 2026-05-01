package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.config.CustomerServiceProperties;
import com.ai.tutor.appointment.mapper.CustomerServiceConfigMapper;
import com.ai.tutor.appointment.model.entity.CustomerServiceConfig;
import com.ai.tutor.appointment.model.vo.CustomerServiceConfigVO;
import com.ai.tutor.appointment.service.CustomerServiceConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

@Service
public class CustomerServiceConfigServiceImpl implements CustomerServiceConfigService {

    private static final Set<String> CHANNEL_TYPES = Set.of("WECHAT_PERSONAL", "WECHAT_WORK");

    @Resource
    private CustomerServiceConfigMapper mapper;
    @Resource
    private CustomerServiceProperties properties;

    @Override
    public CustomerServiceConfigVO config() {
        CustomerServiceConfig config = mapper.selectSingleton();
        if (config == null) {
            config = defaults();
            try {
                mapper.insertSingleton(config);
            } catch (Exception ignored) {
                config = mapper.selectSingleton();
                if (config == null) {
                    config = defaults();
                }
            }
        }
        return toVo(normalize(config));
    }

    private CustomerServiceConfig defaults() {
        CustomerServiceConfig config = new CustomerServiceConfig();
        config.setEnabled(Boolean.TRUE.equals(properties.getEnabled()));
        config.setChannelType(normalizeChannelType(properties.getChannelType()));
        config.setDisplayName(trimOrDefault(properties.getDisplayName(), "创智星球客服"));
        config.setWechatNo(trimNullable(properties.getWechatNo()));
        config.setQqNo(trimNullable(properties.getQqNo()));
        config.setQrCodeObjectKey(trimNullable(properties.getQrCodeObjectKey()));
        config.setServiceTime(trimOrDefault(properties.getServiceTime(), "09:00 - 22:00"));
        config.setDescription(trimNullable(properties.getDescription()));
        return config;
    }

    private CustomerServiceConfig normalize(CustomerServiceConfig config) {
        if (config == null) {
            return defaults();
        }
        config.setEnabled(Boolean.TRUE.equals(config.getEnabled()));
        config.setChannelType(normalizeChannelType(config.getChannelType()));
        config.setDisplayName(trimOrDefault(config.getDisplayName(), "客服"));
        config.setWechatNo(trimNullable(config.getWechatNo()));
        config.setQqNo(trimNullable(config.getQqNo()));
        config.setQrCodeObjectKey(trimNullable(config.getQrCodeObjectKey()));
        config.setServiceTime(trimOrDefault(config.getServiceTime(), "09:00 - 22:00"));
        config.setDescription(trimNullable(config.getDescription()));
        return config;
    }

    private CustomerServiceConfigVO toVo(CustomerServiceConfig config) {
        return CustomerServiceConfigVO.builder()
                .enabled(config.getEnabled())
                .channelType(config.getChannelType())
                .displayName(config.getDisplayName())
                .wechatNo(config.getWechatNo())
                .qqNo(config.getQqNo())
                .qrCodeUrl(buildPublicAssetUrl(config.getQrCodeObjectKey()))
                .serviceTime(config.getServiceTime())
                .description(config.getDescription())
                .build();
    }

    private static String normalizeChannelType(String raw) {
        String v = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        return CHANNEL_TYPES.contains(v) ? v : "WECHAT_WORK";
    }

    private static String trimOrDefault(String raw, String defaultValue) {
        String v = raw == null ? "" : raw.trim();
        return v.isEmpty() ? defaultValue : v;
    }

    private static String trimNullable(String raw) {
        String v = raw == null ? "" : raw.trim();
        return StringUtils.hasText(v) ? v : null;
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
