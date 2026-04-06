package com.ai.tutor.e2e;

import com.fasterxml.jackson.databind.JsonNode;

public class BaseResp {

    public final int code;
    public final String message;
    public final JsonNode data;

    public BaseResp(int code, String message, JsonNode data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static BaseResp from(JsonNode root) {
        int code = root.path("code").asInt(-1);
        String message = root.path("message").asText("");
        JsonNode data = root.get("data");
        return new BaseResp(code, message, data);
    }
}

