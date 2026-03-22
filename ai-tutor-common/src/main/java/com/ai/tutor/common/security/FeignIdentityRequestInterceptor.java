package com.ai.tutor.common.security;

import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.utils.RequestHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

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
        Map<String, Collection<String>> queries = template.queries();
        if (queries == null || queries.isEmpty()) {
            return path;
        }
        LinkedHashMap<String, Collection<String>> sorted = canonicalizeQueries(queries);
        template.queries(null);
        template.queries(sorted);
        String queryString = buildQueryString(sorted);
        if (queryString.isEmpty()) {
            return path;
        }
        return path + "?" + queryString;
    }

    private LinkedHashMap<String, Collection<String>> canonicalizeQueries(Map<String, Collection<String>> queries) {
        TreeMap<String, Collection<String>> sortedKeys = new TreeMap<>(queries);
        LinkedHashMap<String, Collection<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Collection<String>> entry : sortedKeys.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                continue;
            }
            List<String> values = new ArrayList<>();
            Collection<String> rawValues = entry.getValue();
            if (rawValues != null) {
                for (String value : rawValues) {
                    if (value != null) {
                        values.add(value);
                    }
                }
                values.sort(String::compareTo);
            }
            result.put(key, values);
        }
        return result;
    }

    private String buildQueryString(LinkedHashMap<String, Collection<String>> queries) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Collection<String>> entry : queries.entrySet()) {
            String key = entry.getKey();
            Collection<String> values = entry.getValue();
            if (values == null || values.isEmpty()) {
                continue;
            }
            for (String value : values) {
                if (builder.length() > 0) {
                    builder.append('&');
                }
                builder.append(key).append('=').append(value);
            }
        }
        return builder.toString();
    }
}
