package com.ai.tutor.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "gateway.sign")
public class IdentitySignProperties {

    private String secret;

    private long clockSkewMs = 60_000L;

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
        if (whitelistPaths == null) {
            this.whitelistPaths = new ArrayList<>();
        } else {
            this.whitelistPaths = whitelistPaths;
        }
    }
}
