package com.ai.tutor.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public class HttpJson {

    private final HttpClient client;
    private final ObjectMapper mapper;

    public HttpJson() {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.mapper = new ObjectMapper();
    }

    public JsonNode get(URI uri, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(uri).GET().timeout(Duration.ofSeconds(15));
        if (headers != null) {
            headers.forEach(b::header);
        }
        HttpResponse<byte[]> resp = client.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
        return parse(resp.body());
    }

    public JsonNode postJson(URI uri, Map<String, String> headers, Object body) throws IOException, InterruptedException {
        byte[] bytes = mapper.writeValueAsBytes(body);
        HttpRequest.Builder b = HttpRequest.newBuilder(uri)
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json");
        if (headers != null) {
            headers.forEach(b::header);
        }
        HttpResponse<byte[]> resp = client.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
        return parse(resp.body());
    }

    private JsonNode parse(byte[] body) throws IOException {
        if (body == null || body.length == 0) {
            return mapper.createObjectNode();
        }
        return mapper.readTree(new String(body, StandardCharsets.UTF_8));
    }
}

