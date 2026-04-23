package com.ai.tutor.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "gateway.sign")
public class IdentitySignProperties {

    private String secret;

    private long clockSkewMs = 60_000L;

    private List<String> whitelistPaths = defaultWhitelistPaths();

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
        ArrayList<String> merged = defaultWhitelistPaths();
        if (whitelistPaths == null) {
            this.whitelistPaths = merged;
            return;
        }
        for (String path : whitelistPaths) {
            if (path != null && !merged.contains(path)) {
                merged.add(path);
            }
        }
        this.whitelistPaths = merged;
    }

    private static ArrayList<String> defaultWhitelistPaths() {
        return new ArrayList<>(List.of(
                "/internal/live/**"
        ));
    }
}
