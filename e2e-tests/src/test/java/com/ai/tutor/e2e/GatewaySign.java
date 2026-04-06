package com.ai.tutor.e2e;

import com.ai.tutor.common.security.IdentitySignProperties;
import com.ai.tutor.common.security.IdentitySignatureUtils;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

public class GatewaySign {

    private final IdentitySignatureUtils signatureUtils;

    public GatewaySign(String secret) {
        IdentitySignProperties props = new IdentitySignProperties();
        props.setSecret(secret);
        this.signatureUtils = new IdentitySignatureUtils(props);
    }

    public Map<String, String> headers(long uid, int role, String method, URI uri) {
        long ts = System.currentTimeMillis();
        String requestTarget = requestTarget(uri);
        String sign = signatureUtils.sign(uid, role, ts, method, requestTarget);
        Map<String, String> out = new LinkedHashMap<>();
        out.put("X-Uid", String.valueOf(uid));
        out.put("X-Role", String.valueOf(role));
        out.put("X-Ts", String.valueOf(ts));
        out.put("X-Auth-Sign", sign);
        return out;
    }

    private static String requestTarget(URI uri) {
        String path = uri.getRawPath();
        if (path == null) {
            path = "";
        }
        String query = uri.getRawQuery();
        if (query != null && !query.isEmpty()) {
            return path + "?" + query;
        }
        return path;
    }
}

