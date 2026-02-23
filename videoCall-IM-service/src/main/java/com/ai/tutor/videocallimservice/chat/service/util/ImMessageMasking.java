package com.ai.tutor.videocallimservice.chat.service.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImMessageMasking {

    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+?86[-\\s]?)?1[3-9]\\d{9}(?!\\d)");
    private static final Pattern WECHAT_ID = Pattern.compile("(?i)(?:w[\\W_]*x(?:[\\W_]*i[\\W_]*d)?|weixin|wechat|微[\\W_]*信|v[\\W_]*信)\\s*[:：]?\\s*[a-z][a-z0-9_-]{4,19}");
    private static final Pattern QQ_ID = Pattern.compile("(?i)q[\\W_]*q\\s*[:：]?\\s*\\d{5,12}");
    private static final Pattern WECHAT_TOKEN = Pattern.compile("(?i)(?:w[\\W_]*x(?:[\\W_]*i[\\W_]*d)?|weixin|wechat|微[\\W_]*信|v[\\W_]*信)");
    private static final Pattern QQ_TOKEN = Pattern.compile("(?i)q[\\W_]*q");

    private ImMessageMasking() {}

    public static MaskResult mask(String raw) {
        String s = raw == null ? "" : raw;
        boolean hit = false;
        MaskApply r;

        r = apply(s, PHONE);
        s = r.text;
        hit = hit || r.hit;

        r = apply(s, WECHAT_ID);
        s = r.text;
        hit = hit || r.hit;

        r = apply(s, QQ_ID);
        s = r.text;
        hit = hit || r.hit;

        r = apply(s, WECHAT_TOKEN);
        s = r.text;
        hit = hit || r.hit;

        r = apply(s, QQ_TOKEN);
        s = r.text;
        hit = hit || r.hit;

        return new MaskResult(s, hit);
    }

    private static MaskApply apply(String raw, Pattern pattern) {
        if (raw == null || raw.isEmpty()) {
            return new MaskApply(raw == null ? "" : raw, false);
        }
        Matcher m = pattern.matcher(raw);
        if (!m.find()) {
            return new MaskApply(raw, false);
        }
        StringBuffer sb = new StringBuffer();
        do {
            m.appendReplacement(sb, "***");
        } while (m.find());
        m.appendTail(sb);
        return new MaskApply(sb.toString(), true);
    }

    public static final class MaskResult {
        public final String maskedText;
        public final boolean masked;

        public MaskResult(String maskedText, boolean masked) {
            this.maskedText = maskedText;
            this.masked = masked;
        }
    }

    private static final class MaskApply {
        private final String text;
        private final boolean hit;

        private MaskApply(String text, boolean hit) {
            this.text = text;
            this.hit = hit;
        }
    }
}
