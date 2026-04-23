package com.ai.tutor.common.email;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TencentCloudSesClientTest {

    @Test
    void shouldPostSignedTemplateRequestAndParseSuccessResponse() throws Exception {
        AtomicReference<String> authHeader = new AtomicReference<>();
        AtomicReference<String> actionHeader = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            authHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            actionHeader.set(exchange.getRequestHeaders().getFirst("X-TC-Action"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes()));
            String response = "{\"Response\":{\"MessageId\":\"msg-123\",\"RequestId\":\"req-123\"}}";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        server.start();
        try {
            TencentCloudSesClient client = new TencentCloudSesClient();
            TencentCloudSesResponse response = client.sendTemplateEmail(TencentCloudSesRequest.builder()
                    .endpoint("http://127.0.0.1:" + server.getAddress().getPort() + "/")
                    .region("ap-guangzhou")
                    .secretId("id")
                    .secretKey("key")
                    .fromEmail("no-reply@example.com")
                    .fromName("创智星球")
                    .replyToEmail("support@example.com")
                    .toEmail("user@example.com")
                    .subject("邮箱验证通知")
                    .templateId(173982L)
                    .templateDataJson("{\"code\":\"123456\"}")
                    .connectTimeoutMs(3000)
                    .readTimeoutMs(5000)
                    .build());
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getProviderMessageId()).isEqualTo("msg-123");
            assertThat(response.getRequestId()).isEqualTo("req-123");
            assertThat(authHeader.get()).startsWith("TC3-HMAC-SHA256 ");
            assertThat(actionHeader.get()).isEqualTo("SendEmail");
            assertThat(requestBody.get()).contains("\"TemplateID\":173982");
        } finally {
            server.stop(0);
        }
    }
}
