package com.ai.tutor.common.security;

import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.utils.RequestHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;

import java.util.Objects;

public class FeignIdentityRequestInterceptor implements RequestInterceptor {

    private final IdentitySignatureUtils signatureUtils;

    public FeignIdentityRequestInterceptor(IdentitySignatureUtils signatureUtils) {
        this.signatureUtils = Objects.requireNonNull(signatureUtils, "signatureUtils");
    }

    @Override
    public void apply(RequestTemplate template) {
        RequestInfo info = RequestHolder.get();
        if (info == null || info.getUid() == null || info.getRole() == null) {
            return;
        }
        long ts = System.currentTimeMillis();
        String method = template.method();
        String requestTarget = resolveRequestTarget(template);
        String sign = signatureUtils.sign(info.getUid(), info.getRole(), ts, method, requestTarget);

        template.header("X-Uid", String.valueOf(info.getUid()));
        template.header("X-Role", String.valueOf(info.getRole()));
        template.header("X-Ts", String.valueOf(ts));
        template.header("X-Auth-Sign", sign);
    }

    private String resolveRequestTarget(RequestTemplate template) {
        String path = template.path();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        String queryLine = template.queryLine();
        if (queryLine == null || queryLine.isEmpty()) {
            return path;
        }
        if (queryLine.charAt(0) == '?') {
            return path + queryLine;
        }
        return path + "?" + queryLine;
    }
}
