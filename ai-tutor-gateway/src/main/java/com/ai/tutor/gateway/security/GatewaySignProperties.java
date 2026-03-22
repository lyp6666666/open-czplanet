package com.ai.tutor.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "gateway.sign")
public class GatewaySignProperties {

    private String secret;

    private long clockSkewMs;

    private List<String> whitelistPaths = new ArrayList<>();

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getClockSkewMs() {
        return clockSkewMs;
    }

    public void setClockSkewMs(long clockSkewMs) {
        this.clockSkewMs = clockSkewMs;
    }

    public List<String> getWhitelistPaths() {
        return whitelistPaths;
    }

    public void setWhitelistPaths(List<String> whitelistPaths) {
        this.whitelistPaths = whitelistPaths;
    }
}
