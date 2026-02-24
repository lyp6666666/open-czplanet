package com.ai.tutor.appointment.utils;

import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class CityCatalog {
    private static final String NATIONAL = "全国";
    private static final String RESOURCE_PATH = "data/cities.txt";
    private static final Set<String> SUPPORTED = Collections.unmodifiableSet(load());

    private CityCatalog() {
    }

    public static String national() {
        return NATIONAL;
    }

    public static String normalizeCityForFilter(String raw) {
        String v = normalizeRaw(raw);
        if (v == null) {
            return null;
        }
        if (NATIONAL.equals(v)) {
            return null;
        }
        return v;
    }

    public static String normalizeCityForStorage(String classMode, String rawCity) {
        String mode = normalizeRaw(classMode);
        String city = normalizeRaw(rawCity);
        if (mode == null) {
            return city;
        }
        String m = mode.toLowerCase();
        if ("online".equals(m)) {
            if (city == null) {
                return NATIONAL;
            }
            ThrowUtils.throwIf(!isSupportedCity(city), ErrorCode.PARAMS_ERROR, "城市不合法");
            return city;
        }
        if ("offline".equals(m) || "both".equals(m)) {
            ThrowUtils.throwIf(city == null, ErrorCode.PARAMS_ERROR, "线下授课必须选择城市");
            ThrowUtils.throwIf(NATIONAL.equals(city), ErrorCode.PARAMS_ERROR, "线下授课必须选择具体城市");
            ThrowUtils.throwIf(!isSupportedCity(city), ErrorCode.PARAMS_ERROR, "城市不合法");
            return city;
        }
        return city;
    }

    public static boolean isSupportedCity(String raw) {
        String v = normalizeRaw(raw);
        if (v == null) {
            return false;
        }
        if (NATIONAL.equals(v)) {
            return true;
        }
        return SUPPORTED.contains(v);
    }

    private static String normalizeRaw(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim();
        return v.isEmpty() ? null : v;
    }

    private static Set<String> load() {
        Set<String> set = new HashSet<>();
        set.add(NATIONAL);
        try (InputStream in = CityCatalog.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                return set;
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String v = line.trim();
                    if (!v.isEmpty()) {
                        set.add(v);
                    }
                }
            }
        } catch (Exception e) {
            return set;
        }
        return set;
    }
}

