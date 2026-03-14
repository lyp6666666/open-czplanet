package com.ai.tutor.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
@Component("adminJwtProperties")
@ConfigurationProperties(prefix = "admin.jwt")
public class JwtProperties {

    private List<String> secrets = new ArrayList<>();

    private Duration expiration = Duration.ofHours(24);

    private String issuer;
}
