package com.ai.tutor.appointment.integration.email.impl;

import com.ai.tutor.appointment.config.EmailNotificationProperties;
import com.ai.tutor.appointment.integration.email.EmailTemplateRenderer;
import com.ai.tutor.appointment.integration.email.dto.RenderedEmail;
import jakarta.annotation.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LocalHtmlEmailTemplateRenderer implements EmailTemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Resource
    private EmailNotificationProperties properties;

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @Override
    public RenderedEmail render(String templateCode, Map<String, Object> payload) {
        Map<String, Object> model = normalizeModel(templateCode, payload);
        String template = loadTemplate(templateCode);
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String value = escapeHtml(String.valueOf(model.getOrDefault(matcher.group(1), "")));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(buffer);
        return RenderedEmail.builder()
                .subject(defaultSubject(templateCode))
                .htmlBody(buffer.toString())
                .build();
    }

    private Map<String, Object> normalizeModel(String templateCode, Map<String, Object> payload) {
        Map<String, Object> model = new HashMap<>();
        if (payload != null) {
            model.putAll(payload);
        }
        if ("EMAIL_VERIFY_CODE".equals(templateCode)) {
            model.putIfAbsent("expireMinutes", 10);
        } else if ("LESSON_START_REMINDER".equals(templateCode)) {
            model.putIfAbsent("receiverName", "用户");
            model.putIfAbsent("courseName", valueOf(payload, "courseName", "lessonTitle", "title", "课程"));
            LocalDateTime startTime = parseDateTime(valueOf(payload, "startTime", null, null));
            model.putIfAbsent("lessonDate", startTime == null ? LocalDate.now().format(DATE_FMT) : startTime.format(DATE_FMT));
            model.putIfAbsent("lessonTime", startTime == null ? "" : startTime.format(TIME_FMT));
            model.putIfAbsent("counterpartName", valueOf(payload, "counterpartName", null, "授课对象"));
            model.putIfAbsent("prepareTips", valueOf(payload, "prepareTips", null, "请提前 5 分钟进入创智星球，检查网络和设备。"));
        } else if ("LESSON_SUMMARY".equals(templateCode) || "LESSON_SUMMARY_BACKFILL".equals(templateCode)) {
            model.putIfAbsent("receiverName", "用户");
            model.putIfAbsent("courseName", valueOf(payload, "courseName", "title", "课程"));
            LocalDateTime lessonTime = parseDateTime(valueOf(payload, "lessonStartTime", "startTime", null));
            model.putIfAbsent("lessonDate", lessonTime == null ? LocalDate.now().format(DATE_FMT) : lessonTime.format(DATE_FMT));
            model.putIfAbsent("lessonTime", lessonTime == null ? "" : lessonTime.format(TIME_FMT));
            model.putIfAbsent("summaryHighlight", valueOf(payload, "summaryHighlight", "brief", "本节重点请前往创智星球站内查看。"));
            model.putIfAbsent("homeworkAdvice", valueOf(payload, "homeworkAdvice", "homework", "请根据站内完整总结继续巩固练习。"));
        } else if ("UNREAD_MESSAGE_REMINDER".equals(templateCode)) {
            model.putIfAbsent("receiverName", "用户");
            model.putIfAbsent("senderName", valueOf(payload, "senderName", null, "站内联系人"));
            model.putIfAbsent("senderRole", valueOf(payload, "senderRole", null, "用户"));
            model.putIfAbsent("messageSummary", valueOf(payload, "messageSummary", "messageTypeSummary", "你收到一条未读消息"));
        } else if ("EMAIL_CHANGED_NOTICE".equals(templateCode)) {
            model.putIfAbsent("changeTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        return model;
    }

    private String valueOf(Map<String, Object> payload, String primary, String fallback, String defaultValue) {
        if (payload == null) {
            return defaultValue == null ? "" : defaultValue;
        }
        Object primaryValue = primary == null ? null : payload.get(primary);
        if (primaryValue != null && !String.valueOf(primaryValue).isBlank()) {
            return String.valueOf(primaryValue);
        }
        Object fallbackValue = fallback == null ? null : payload.get(fallback);
        if (fallbackValue != null && !String.valueOf(fallbackValue).isBlank()) {
            return String.valueOf(fallbackValue);
        }
        return defaultValue == null ? "" : defaultValue;
    }

    private String valueOf(Map<String, Object> payload, String first, String second, String third, String defaultValue) {
        String v = valueOf(payload, first, second, null);
        if (v != null && !v.isBlank()) {
            return v;
        }
        return valueOf(payload, third, null, defaultValue);
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ignore) {
            return null;
        }
    }

    private String loadTemplate(String templateCode) {
        return cache.computeIfAbsent(templateCode, this::readTemplate);
    }

    private String readTemplate(String templateCode) {
        String fileName = templateCode + ".html";
        try {
            Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
            for (int i = 0; i < 4 && current != null; i++) {
                Path file = current.resolve(properties.getSender().getTemplateDir()).resolve(fileName);
                if (Files.exists(file)) {
                    return Files.readString(file, StandardCharsets.UTF_8);
                }
                current = current.getParent();
            }
            ClassPathResource resource = new ClassPathResource("email/templates/tencent/" + fileName);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("email template not found: " + templateCode, e);
        }
    }

    private String defaultSubject(String templateCode) {
        String normalized = templateCode == null ? "" : templateCode.toUpperCase(Locale.ROOT);
        if ("EMAIL_VERIFY_CODE".equals(normalized)) {
            return "邮箱验证通知";
        }
        if ("UNREAD_MESSAGE_REMINDER".equals(normalized)) {
            return "未读消息待查看";
        }
        if ("LESSON_START_REMINDER".equals(normalized)) {
            return "课程即将开始提醒";
        }
        if ("LESSON_SUMMARY".equals(normalized)) {
            return "课后总结已生成";
        }
        if ("LESSON_SUMMARY_BACKFILL".equals(normalized)) {
            return "你有一份可查看的最新课后总结";
        }
        if ("EMAIL_CHANGED_NOTICE".equals(normalized)) {
            return "邮箱变更通知";
        }
        return "平台通知";
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
