package com.ai.tutor.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "gateway.jwt")
public class GatewayJwtProperties {

    private List<String> secrets = new ArrayList<>();

    private String issuer;

    public List<String> getSecrets() {
        return secrets;
    }

    public void setSecrets(List<String> secrets) {
        if (secrets == null) {
            this.secrets = new ArrayList<>();
        } else {
            this.secrets = secrets;
        }
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
