package com.ai.tutor.common.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TencentCloudSesClient {

    private static final String SERVICE = "ses";
    private static final String VERSION = "2020-10-02";
    private static final String ACTION = "SendEmail";
    private static final String CONTENT_TYPE = "application/json; charset=utf-8";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TencentCloudSesResponse sendTemplateEmail(TencentCloudSesRequest request) {
        try {
            URI endpointUri = buildEndpointUri(request.getEndpoint());
            String host = endpointUri.getHost();
            String payload = objectMapper.writeValueAsString(buildPayload(request));
            long timestamp = Instant.now().getEpochSecond();
            String date = DATE_FMT.format(Instant.ofEpochSecond(timestamp));
            String authorization = buildAuthorization(request.getSecretId(), request.getSecretKey(), host, payload, timestamp, date);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(Math.max(1000, request.getConnectTimeoutMs())))
                    .build();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(endpointUri)
                    .timeout(Duration.ofMillis(Math.max(1000, request.getReadTimeoutMs())))
                    .header("Authorization", authorization)
                    .header("Content-Type", CONTENT_TYPE)
                    .header("X-TC-Action", ACTION)
                    .header("X-TC-Version", VERSION)
                    .header("X-TC-Region", request.getRegion())
                    .header("X-TC-Timestamp", String.valueOf(timestamp))
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return parseResponse(response.body(), response.statusCode());
        } catch (Exception e) {
            return TencentCloudSesResponse.builder()
                    .success(false)
                    .errorCode("HTTP_CLIENT_ERROR")
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private Map<String, Object> buildPayload(TencentCloudSesRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("FromEmailAddress", formatFromEmail(request.getFromEmail(), request.getFromName()));
        payload.put("Destination", new String[]{request.getToEmail()});
        payload.put("Subject", request.getSubject());
        if (request.getReplyToEmail() != null && !request.getReplyToEmail().isBlank()) {
            payload.put("ReplyToAddresses", request.getReplyToEmail());
        }
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("TemplateID", request.getTemplateId());
        template.put("TemplateData", request.getTemplateDataJson());
        payload.put("Template", template);
        return payload;
    }

    private TencentCloudSesResponse parseResponse(String body, int statusCode) throws Exception {
        JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
        JsonNode resp = root.path("Response");
        JsonNode error = resp.path("Error");
        if (statusCode >= 200 && statusCode < 300 && error.isMissingNode()) {
            return TencentCloudSesResponse.builder()
                    .success(true)
                    .providerMessageId(resp.path("MessageId").asText(null))
                    .requestId(resp.path("RequestId").asText(null))
                    .build();
        }
        return TencentCloudSesResponse.builder()
                .success(false)
                .providerMessageId(resp.path("MessageId").asText(null))
                .requestId(resp.path("RequestId").asText(null))
                .errorCode(error.path("Code").asText(statusCode >= 200 && statusCode < 300 ? null : String.valueOf(statusCode)))
                .errorMessage(error.path("Message").asText(body))
                .build();
    }

    private URI buildEndpointUri(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return URI.create("https://ses.tencentcloudapi.com");
        }
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            return URI.create(endpoint);
        }
        return URI.create("https://" + endpoint);
    }

    private String buildAuthorization(String secretId, String secretKey, String host, String payload, long timestamp, String date) throws Exception {
        String canonicalHeaders = "content-type:" + CONTENT_TYPE + "\n"
                + "host:" + host + "\n"
                + "x-tc-action:" + ACTION.toLowerCase() + "\n";
        String signedHeaders = "content-type;host;x-tc-action";
        String canonicalRequest = "POST\n/\n\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + sha256Hex(payload);
        String credentialScope = date + "/" + SERVICE + "/tc3_request";
        String stringToSign = "TC3-HMAC-SHA256\n"
                + timestamp + "\n"
                + credentialScope + "\n"
                + sha256Hex(canonicalRequest);
        byte[] secretDate = hmacSha256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmacSha256(secretDate, SERVICE);
        byte[] secretSigning = hmacSha256(secretService, "tc3_request");
        String signature = HexFormat.of().formatHex(hmacSha256(secretSigning, stringToSign));
        return "TC3-HMAC-SHA256 Credential=" + secretId + "/" + credentialScope
                + ", SignedHeaders=" + signedHeaders
                + ", Signature=" + signature;
    }

    private byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256Hex(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String formatFromEmail(String fromEmail, String fromName) {
        if (fromName == null || fromName.isBlank()) {
            return fromEmail;
        }
        return fromName + " <" + fromEmail + ">";
    }
}
