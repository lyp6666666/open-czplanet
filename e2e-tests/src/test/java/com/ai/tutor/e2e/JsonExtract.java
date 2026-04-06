package com.ai.tutor.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonExtract {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Long extractLong(JsonNode body, String... keys) {
        if (body == null || body.isMissingNode() || body.isNull()) {
            return null;
        }
        if (body.isObject()) {
            if (keys != null) {
                for (String k : keys) {
                    if (k == null || k.isBlank()) continue;
                    JsonNode v = body.get(k);
                    if (v != null && v.canConvertToLong()) {
                        long id = v.asLong();
                        if (id != 0) return id;
                    }
                }
            }
            return null;
        }
        if (body.isTextual()) {
            String s = body.asText();
            if (s == null || s.isBlank()) return null;
            try {
                JsonNode parsed = MAPPER.readTree(s);
                return extractLong(parsed, keys);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    public static Long extractApplicationId(JsonNode body) {
        return extractLong(body, "applicationId", "application_id", "eventId", "event_id");
    }

    public static Long extractEventId(JsonNode body) {
        return extractLong(body, "eventId", "event_id");
    }
}
