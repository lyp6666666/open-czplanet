# Gateway + Nacos + Identity Propagation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不修改前端请求路径的前提下，引入统一 API 网关，完成用户侧 JWT 鉴权上收、`uid/role` 注入、Nacos 注册发现、以及 appointment/im/payment 三个服务间 OpenFeign 调用改造。  
**Architecture:** 新增 `ai-tutor-gateway` 作为唯一入口，网关验 JWT 并注入签名身份头；业务服务统一验签并写入 `RequestHolder`，不再自行验 JWT。服务间请求不经过网关，直接通过 Nacos + OpenFeign 调用，并复用相同签名协议透传身份。  
**Tech Stack:** Spring Boot 3.2.4, Spring Cloud Gateway, Spring Cloud Alibaba Nacos Discovery/Config, Spring Cloud OpenFeign, JJWT, MyBatis/MyBatis-Plus, JUnit 5/Mockito.

---

## Scope Check

本实现覆盖网关、common 安全组件、appointment/im/payment 三服务改造，虽然跨模块较多，但属于单一目标链路（统一入口鉴权 + 身份传播 + 服务发现/调用），不再拆分子项目。

## File Structure Map

### New Module (Gateway)

- Create: `ai-tutor-gateway/pom.xml`  
职责：网关依赖与构建配置。
- Create: `ai-tutor-gateway/src/main/java/com/ai/tutor/gateway/AiTutorGatewayApplication.java`  
职责：网关应用入口。
- Create: `ai-tutor-gateway/src/main/java/com/ai/tutor/gateway/security/GatewayJwtProperties.java`  
职责：JWT 解析配置。
- Create: `ai-tutor-gateway/src/main/java/com/ai/tutor/gateway/security/GatewaySignProperties.java`  
职责：签名配置（secret、时窗、白名单）。
- Create: `ai-tutor-gateway/src/main/java/com/ai/tutor/gateway/security/JwtClaimsService.java`  
职责：JWT 校验与 claims 提取。
- Create: `ai-tutor-gateway/src/main/java/com/ai/tutor/gateway/security/GatewaySignService.java`  
职责：网关签名生成。
- Create: `ai-tutor-gateway/src/main/java/com/ai/tutor/gateway/security/GatewayAuthGlobalFilter.java`  
职责：网关认证过滤与头注入。
- Create: `ai-tutor-gateway/src/main/resources/application.yml`  
职责：路由、Nacos、端口、安全策略配置。

### Common Security Infrastructure

- Create: `ai-tutor-common/src/main/java/com/ai/tutor/common/security/IdentitySignProperties.java`  
职责：服务侧验签配置。
- Create: `ai-tutor-common/src/main/java/com/ai/tutor/common/security/IdentitySignatureUtils.java`  
职责：签名计算、常量时间比较。
- Create: `ai-tutor-common/src/main/java/com/ai/tutor/common/security/GatewayIdentityInterceptor.java`  
职责：验签 + 注入 `RequestHolder` + 写 request attribute。
- Create: `ai-tutor-common/src/main/java/com/ai/tutor/common/security/GatewayIdentityWebMvcConfigurer.java`  
职责：注册 `GatewayIdentityInterceptor`。
- Create: `ai-tutor-common/src/main/java/com/ai/tutor/common/security/FeignIdentityRequestInterceptor.java`  
职责：服务间 Feign 自动注入签名身份头。
- Create: `ai-tutor-common/src/main/java/com/ai/tutor/common/security/GatewayIdentityAutoConfiguration.java`  
职责：统一自动配置开关（`security.gateway-identity.enabled`）。

### Service Migration (Appointment / IM / Payment)

- Modify: `tutor-appointment-service/src/main/java/com/ai/tutor/appointment/config/WebConfig.java`  
职责：移除用户侧 `JwtInterceptor` 链，保留/适配角色鉴权逻辑。
- Modify: `tutor-appointment-service/src/main/java/com/ai/tutor/appointment/interceptor/RoleInterceptor.java`  
职责：改为读取 `RequestHolder.role`（数字）并映射。
- Modify: `tutor-appointment-service/src/main/java/com/ai/tutor/appointment/TutorAppointmentApplication.java`  
职责：启用 Feign。
- Modify: `tutor-appointment-service/src/main/java/com/ai/tutor/appointment/integration/HttpImFacade.java`  
职责：由 RestTemplate 改为 Feign 调用。
- Create: `tutor-appointment-service/src/main/java/com/ai/tutor/appointment/integration/feign/ImInternalFeignClient.java`  
职责：声明 IM 内部接口客户端。
- Modify: `tutor-appointment-service/src/main/resources/application.yml`  
职责：增加 `server.port=8081`、Nacos discovery、gateway identity 配置。

- Modify: `videoCall-IM-service/src/main/java/com/ai/tutor/videocallimservice/VideoCallImApplication.java`  
职责：启用 Feign。
- Modify: `videoCall-IM-service/src/main/java/com/ai/tutor/videocallimservice/integration/AppointmentInternalClient.java`  
职责：由 RestTemplate 改为 Feign 调用预约内部接口。
- Create: `videoCall-IM-service/src/main/java/com/ai/tutor/videocallimservice/integration/feign/AppointmentInternalFeignClient.java`  
职责：声明预约内部接口客户端。
- Modify: `videoCall-IM-service/src/main/resources/application.yml`  
职责：增加 `server.port=8082`、Nacos discovery、gateway identity 配置。

- Modify: `payment-service/src/main/java/com/ai/tutor/payment/PaymentServiceApplication.java`  
职责：启用 Feign。
- Modify: `payment-service/src/main/java/com/ai/tutor/payment/integration/HttpBrokerageOrderFacade.java`  
职责：由 RestTemplate 改为 Feign。
- Create: `payment-service/src/main/java/com/ai/tutor/payment/integration/feign/ImBrokerageOrderFeignClient.java`  
职责：声明 IM 内部支付订单接口客户端。
- Modify: `payment-service/src/main/resources/application.yml`  
职责：Nacos discovery 与 gateway identity 配置。

### Build + Docs

- Modify: `pom.xml`  
职责：新增网关模块，统一管理新依赖版本。
- Modify: `ai-tutor-common/pom.xml`  
职责：加入 Feign/Core 测试依赖（若缺）。
- Modify: `tutor-appointment-service/pom.xml`  
职责：加入 OpenFeign + Nacos Discovery 依赖。
- Modify: `videoCall-IM-service/pom.xml`  
职责：加入 OpenFeign + Nacos Discovery 依赖。
- Modify: `payment-service/pom.xml`  
职责：加入 OpenFeign + Nacos Discovery 依赖。
- Modify: `README-backend.md`  
职责：更新独立微服务 + 网关启动说明（替代 starter 作为默认运行方式）。

## Implementation Tasks

### Task 1: Bootstrap Gateway Module

**Files:**
- Create: `ai-tutor-gateway/pom.xml`
- Create: `ai-tutor-gateway/src/main/java/com/ai/tutor/gateway/AiTutorGatewayApplication.java`
- Create: `ai-tutor-gateway/src/main/resources/application.yml`
- Test: `ai-tutor-gateway/src/test/java/com/ai/tutor/gateway/GatewayApplicationContextTest.java`
- Modify: `pom.xml`

- [ ] **Step 1: Write the failing test**

```java
@SpringBootTest
class GatewayApplicationContextTest {
    @Autowired
    private ApplicationContext ctx;

    @Test
    void shouldLoadGatewayContext() {
        assertNotNull(ctx.getBean(RouteLocator.class));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -pl ai-tutor-gateway -am -Dtest=GatewayApplicationContextTest test`  
Expected: FAIL with module/project not found.

- [ ] **Step 3: Write minimal implementation**

```java
@SpringBootApplication
public class AiTutorGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiTutorGatewayApplication.class, args);
    }
}
```

Update root `pom.xml`:

```xml
<module>ai-tutor-gateway</module>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -pl ai-tutor-gateway -am -Dtest=GatewayApplicationContextTest test`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add pom.xml ai-tutor-gateway
git commit -m "feat(gateway): bootstrap gateway module and app entry"
```

### Task 2: Implement Gateway JWT + Signing Core

**Files:**
- Create: `ai-tutor-gateway/src/main/java/com/ai/tutor/gateway/security/GatewayJwtProperties.java`
- Create: `ai-tutor-gateway/src/main/java/com/ai/tutor/gateway/security/GatewaySignProperties.java`
- Create: `ai-tutor-gateway/src/main/java/com/ai/tutor/gateway/security/JwtClaimsService.java`
- Create: `ai-tutor-gateway/src/main/java/com/ai/tutor/gateway/security/GatewaySignService.java`
- Test: `ai-tutor-gateway/src/test/java/com/ai/tutor/gateway/security/JwtClaimsServiceTest.java`
- Test: `ai-tutor-gateway/src/test/java/com/ai/tutor/gateway/security/GatewaySignServiceTest.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void shouldParseUidAndRoleFromJwt() {
    JwtIdentity id = jwtClaimsService.parse(validToken);
    assertEquals(206L, id.uid());
    assertEquals(1, id.role());
}

@Test
void shouldGenerateStableSignature() {
    String sign = signService.sign(206L, 1, 1711111111111L, "GET", "/chat/room/page");
    assertEquals("expected-value-from-known-vector", sign);
}
```

- [ ] **Step 2: Run tests to verify failure**

Run: `./mvnw -pl ai-tutor-gateway -Dtest=JwtClaimsServiceTest,GatewaySignServiceTest test`  
Expected: FAIL with missing classes/beans.

- [ ] **Step 3: Write minimal implementation**

```java
public record JwtIdentity(Long uid, Integer role) {}

public String sign(Long uid, Integer role, long ts, String method, String path) {
    String payload = uid + "\n" + role + "\n" + ts + "\n" + method + "\n" + path;
    return hmacSha256Hex(secret, payload);
}
```

- [ ] **Step 4: Run tests to verify pass**

Run: `./mvnw -pl ai-tutor-gateway -Dtest=JwtClaimsServiceTest,GatewaySignServiceTest test`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add ai-tutor-gateway/src/main/java/com/ai/tutor/gateway/security ai-tutor-gateway/src/test/java/com/ai/tutor/gateway/security
git commit -m "feat(gateway): add jwt claims parser and signature service"
```

### Task 3: Implement Gateway Auth Filter + Route/Whitelist Behavior

**Files:**
- Create: `ai-tutor-gateway/src/main/java/com/ai/tutor/gateway/security/GatewayAuthGlobalFilter.java`
- Modify: `ai-tutor-gateway/src/main/resources/application.yml`
- Test: `ai-tutor-gateway/src/test/java/com/ai/tutor/gateway/security/GatewayAuthGlobalFilterTest.java`

- [ ] **Step 1: Write failing integration tests**

```java
@Test
void shouldRejectProtectedPathWithoutToken() {
    webTestClient.get().uri("/chat/room/page?pageSize=10").exchange().expectStatus().isUnauthorized();
}

@Test
void shouldAllowWhitelistedPathWithoutToken() {
    webTestClient.get().uri("/api/v1/public/home/config").exchange().expectStatus().isOk();
}
```

- [ ] **Step 2: Run tests to verify failure**

Run: `./mvnw -pl ai-tutor-gateway -Dtest=GatewayAuthGlobalFilterTest test`  
Expected: FAIL with current pass-through behavior.

- [ ] **Step 3: Implement minimal filter**

```java
if (isWhitelisted(path)) return chain.filter(exchange);
String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
JwtIdentity id = jwtClaimsService.parseBearer(auth);
long ts = System.currentTimeMillis();
String sign = signService.sign(id.uid(), id.role(), ts, method, path);
ServerHttpRequest mutated = request.mutate()
    .header("X-Uid", String.valueOf(id.uid()))
    .header("X-Role", String.valueOf(id.role()))
    .header("X-Ts", String.valueOf(ts))
    .header("X-Auth-Sign", sign)
    .build();
```

- [ ] **Step 4: Run tests to verify pass**

Run: `./mvnw -pl ai-tutor-gateway -Dtest=GatewayAuthGlobalFilterTest test`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add ai-tutor-gateway/src/main/java/com/ai/tutor/gateway/security/GatewayAuthGlobalFilter.java ai-tutor-gateway/src/main/resources/application.yml ai-tutor-gateway/src/test/java/com/ai/tutor/gateway/security/GatewayAuthGlobalFilterTest.java
git commit -m "feat(gateway): enforce jwt auth and signed identity header injection"
```

### Task 4: Add Common Identity Verification Infrastructure

**Files:**
- Create: `ai-tutor-common/src/main/java/com/ai/tutor/common/security/IdentitySignProperties.java`
- Create: `ai-tutor-common/src/main/java/com/ai/tutor/common/security/IdentitySignatureUtils.java`
- Create: `ai-tutor-common/src/main/java/com/ai/tutor/common/security/GatewayIdentityInterceptor.java`
- Create: `ai-tutor-common/src/main/java/com/ai/tutor/common/security/GatewayIdentityWebMvcConfigurer.java`
- Create: `ai-tutor-common/src/main/java/com/ai/tutor/common/security/FeignIdentityRequestInterceptor.java`
- Create: `ai-tutor-common/src/main/java/com/ai/tutor/common/security/GatewayIdentityAutoConfiguration.java`
- Modify: `ai-tutor-common/pom.xml`
- Test: `ai-tutor-common/src/test/java/com/ai/tutor/common/security/IdentitySignatureUtilsTest.java`
- Test: `ai-tutor-common/src/test/java/com/ai/tutor/common/security/GatewayIdentityInterceptorTest.java`

- [ ] **Step 1: Write failing tests**

```java
@Test
void shouldRejectWhenSignatureMismatch() {
    MockHttpServletRequest req = signedRequest("bad-sign");
    assertThrows(BusinessException.class, () -> interceptor.preHandle(req, resp, handler));
}

@Test
void shouldPopulateRequestHolderWhenSignatureValid() throws Exception {
    MockHttpServletRequest req = signedRequest(validSign);
    interceptor.preHandle(req, resp, handler);
    assertEquals(206L, RequestHolder.get().getUid());
    assertEquals(1, RequestHolder.get().getRole());
}
```

- [ ] **Step 2: Run tests to verify failure**

Run: `./mvnw -pl ai-tutor-common -Dtest=IdentitySignatureUtilsTest,GatewayIdentityInterceptorTest test`  
Expected: FAIL with missing classes.

- [ ] **Step 3: Implement minimal classes**

```java
public boolean verify(Long uid, Integer role, Long ts, String method, String path, String sign) {
    String expected = sign(uid, role, ts, method, path);
    return MessageDigest.isEqual(expected.getBytes(UTF_8), sign.getBytes(UTF_8));
}

RequestInfo info = Optional.ofNullable(RequestHolder.get()).orElseGet(RequestInfo::new);
info.setUid(uid);
info.setRole(role);
info.setIp(resolveIp(request));
RequestHolder.set(info);
request.setAttribute(RequestHolder.ATTRIBUTE_UID, String.valueOf(uid));
request.setAttribute(RequestHolder.ATTRIBUTE_ROLE, role);
```

- [ ] **Step 4: Run tests to verify pass**

Run: `./mvnw -pl ai-tutor-common -Dtest=IdentitySignatureUtilsTest,GatewayIdentityInterceptorTest test`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add ai-tutor-common/pom.xml ai-tutor-common/src/main/java/com/ai/tutor/common/security ai-tutor-common/src/test/java/com/ai/tutor/common/security
git commit -m "feat(common): add gateway identity verification and feign identity propagation"
```

### Task 5: Migrate Appointment Service to Gateway Identity

**Files:**
- Modify: `tutor-appointment-service/src/main/java/com/ai/tutor/appointment/config/WebConfig.java`
- Modify: `tutor-appointment-service/src/main/java/com/ai/tutor/appointment/interceptor/RoleInterceptor.java`
- Modify: `tutor-appointment-service/src/main/resources/application.yml`
- Modify: `tutor-appointment-service/pom.xml`
- Test: `tutor-appointment-service/src/test/java/com/ai/tutor/appointment/interceptor/RoleInterceptorTest.java`

- [ ] **Step 1: Write failing test for role mapping from numeric identity**

```java
@Test
void shouldAllowTeacherRouteWhenRoleValueIs1() {
    RequestInfo info = new RequestInfo();
    info.setRole(1);
    RequestHolder.set(info);
    assertDoesNotThrow(() -> interceptor.preHandle(req("/api/v1/tutor/services", "POST"), resp, handler));
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `./mvnw -pl tutor-appointment-service -Dtest=RoleInterceptorTest test`  
Expected: FAIL due interceptor still reading request attribute enum.

- [ ] **Step 3: Implement minimal migration**

```java
Integer roleValue = RequestHolder.get() == null ? null : RequestHolder.get().getRole();
UserRoleEnum role = roleValue == null ? null : UserRoleEnum.fromValue(roleValue);
```

Update `WebConfig`: remove `jwtInterceptor` registration for用户侧路由，仅保留 `roleInterceptor`（以及必要公共拦截器）。

- [ ] **Step 4: Run service tests**

Run: `./mvnw -pl tutor-appointment-service test`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add tutor-appointment-service/src/main/java/com/ai/tutor/appointment/config/WebConfig.java tutor-appointment-service/src/main/java/com/ai/tutor/appointment/interceptor/RoleInterceptor.java tutor-appointment-service/src/main/resources/application.yml tutor-appointment-service/pom.xml tutor-appointment-service/src/test/java/com/ai/tutor/appointment/interceptor/RoleInterceptorTest.java
git commit -m "refactor(appointment): switch auth context to gateway-signed identity"
```

### Task 6: Migrate IM Service to Gateway Identity + Discovery

**Files:**
- Modify: `videoCall-IM-service/src/main/java/com/ai/tutor/videocallimservice/VideoCallImApplication.java`
- Modify: `videoCall-IM-service/src/main/resources/application.yml`
- Modify: `videoCall-IM-service/pom.xml`
- Test: `videoCall-IM-service/src/test/java/com/ai/tutor/videocallimservice/VideoCallImApplicationTests.java`

- [ ] **Step 1: Write failing bootstrap test for Feign/discovery enablement**

```java
@Test
void shouldEnableFeignClients() {
    assertTrue(VideoCallImApplication.class.isAnnotationPresent(EnableFeignClients.class));
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `./mvnw -pl videoCall-IM-service -Dtest=VideoCallImApplicationTests test`  
Expected: FAIL before annotation/config change.

- [ ] **Step 3: Implement minimal change**

```java
@EnableFeignClients(basePackages = "com.ai.tutor.videocallimservice.integration.feign")
@SpringBootApplication(scanBasePackages = {"com.ai.tutor.videocallimservice", "com.ai.tutor.common"})
```

Add Nacos discovery + `server.port: ${SERVER_PORT:8082}` + `security.gateway-identity.enabled: true`.

- [ ] **Step 4: Run module tests**

Run: `./mvnw -pl videoCall-IM-service test`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add videoCall-IM-service/src/main/java/com/ai/tutor/videocallimservice/VideoCallImApplication.java videoCall-IM-service/src/main/resources/application.yml videoCall-IM-service/pom.xml videoCall-IM-service/src/test/java/com/ai/tutor/videocallimservice/VideoCallImApplicationTests.java
git commit -m "feat(im): enable nacos discovery and gateway identity mode"
```

### Task 7: Migrate Payment Service to Gateway Identity + Discovery

**Files:**
- Modify: `payment-service/src/main/java/com/ai/tutor/payment/PaymentServiceApplication.java`
- Modify: `payment-service/src/main/resources/application.yml`
- Modify: `payment-service/pom.xml`
- Test: `payment-service/src/test/java/com/ai/tutor/payment/PaymentServiceApplicationTests.java`

- [ ] **Step 1: Write failing test for Feign enablement**

```java
@Test
void shouldEnableFeignClients() {
    assertTrue(PaymentServiceApplication.class.isAnnotationPresent(EnableFeignClients.class));
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `./mvnw -pl payment-service -Dtest=PaymentServiceApplicationTests test`  
Expected: FAIL.

- [ ] **Step 3: Implement minimal changes**

```java
@EnableFeignClients(basePackages = "com.ai.tutor.payment.integration.feign")
@SpringBootApplication(scanBasePackages = {"com.ai.tutor.payment", "com.ai.tutor.common"})
```

Configure discovery + identity verification in `application.yml`.

- [ ] **Step 4: Run module tests**

Run: `./mvnw -pl payment-service test`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add payment-service/src/main/java/com/ai/tutor/payment/PaymentServiceApplication.java payment-service/src/main/resources/application.yml payment-service/pom.xml payment-service/src/test/java/com/ai/tutor/payment/PaymentServiceApplicationTests.java
git commit -m "feat(payment): enable nacos discovery and gateway identity mode"
```

### Task 8: Replace Appointment -> IM RestTemplate with OpenFeign

**Files:**
- Create: `tutor-appointment-service/src/main/java/com/ai/tutor/appointment/integration/feign/ImInternalFeignClient.java`
- Modify: `tutor-appointment-service/src/main/java/com/ai/tutor/appointment/integration/HttpImFacade.java`
- Test: `tutor-appointment-service/src/test/java/com/ai/tutor/appointment/integration/HttpImFacadeTest.java`

- [ ] **Step 1: Write failing test for facade delegation**

```java
@Test
void shouldCallFeignClientGetOrCreateRoom() {
    when(client.getOrCreateRoomWithUser(any())).thenReturn(success(1001L));
    Long roomId = facade.getOrCreateRoomWithUser(206L, 113L);
    assertEquals(1001L, roomId);
    verify(client).getOrCreateRoomWithUser(any());
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `./mvnw -pl tutor-appointment-service -Dtest=HttpImFacadeTest test`  
Expected: FAIL due missing feign client/delegation.

- [ ] **Step 3: Implement minimal Feign client + delegation**

```java
@FeignClient(name = "videoCall-IM-service")
public interface ImInternalFeignClient {
    @PostMapping("/internal/facade/im/rooms/with-user")
    BaseResponse<Long> getOrCreateRoomWithUser(@RequestBody ImRoomRequest request);
}
```

`HttpImFacade` 改为调用 `ImInternalFeignClient` 并复用 `unwrapData`。

- [ ] **Step 4: Run tests**

Run: `./mvnw -pl tutor-appointment-service -Dtest=HttpImFacadeTest test`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add tutor-appointment-service/src/main/java/com/ai/tutor/appointment/integration/HttpImFacade.java tutor-appointment-service/src/main/java/com/ai/tutor/appointment/integration/feign/ImInternalFeignClient.java tutor-appointment-service/src/test/java/com/ai/tutor/appointment/integration/HttpImFacadeTest.java
git commit -m "refactor(appointment): migrate im facade calls to openfeign"
```

### Task 9: Replace Payment -> IM RestTemplate with OpenFeign

**Files:**
- Create: `payment-service/src/main/java/com/ai/tutor/payment/integration/feign/ImBrokerageOrderFeignClient.java`
- Modify: `payment-service/src/main/java/com/ai/tutor/payment/integration/HttpBrokerageOrderFacade.java`
- Test: `payment-service/src/test/java/com/ai/tutor/payment/integration/HttpBrokerageOrderFacadeTest.java`

- [ ] **Step 1: Write failing test for payable order query delegation**

```java
@Test
void shouldCallFeignClientForPayableOrder() {
    when(client.getPayableOrder(1L, 206L)).thenReturn(success(info));
    BrokerageOrderPayInfo result = facade.getPayableOrder(1L, 206L);
    assertEquals(info.getOrderId(), result.getOrderId());
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `./mvnw -pl payment-service -Dtest=HttpBrokerageOrderFacadeTest test`  
Expected: FAIL.

- [ ] **Step 3: Implement minimal feign-based facade**

```java
@FeignClient(name = "videoCall-IM-service")
interface ImBrokerageOrderFeignClient {
    @GetMapping("/internal/facade/brokerage/orders/{orderId}/payable")
    BaseResponse<BrokerageOrderPayInfo> getPayableOrder(@PathVariable Long orderId, @RequestParam Long uid);
}
```

- [ ] **Step 4: Run tests**

Run: `./mvnw -pl payment-service -Dtest=HttpBrokerageOrderFacadeTest test`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add payment-service/src/main/java/com/ai/tutor/payment/integration/HttpBrokerageOrderFacade.java payment-service/src/main/java/com/ai/tutor/payment/integration/feign/ImBrokerageOrderFeignClient.java payment-service/src/test/java/com/ai/tutor/payment/integration/HttpBrokerageOrderFacadeTest.java
git commit -m "refactor(payment): migrate brokerage facade calls to openfeign"
```

### Task 10: Replace IM -> Appointment RestTemplate with OpenFeign

**Files:**
- Create: `videoCall-IM-service/src/main/java/com/ai/tutor/videocallimservice/integration/feign/AppointmentInternalFeignClient.java`
- Modify: `videoCall-IM-service/src/main/java/com/ai/tutor/videocallimservice/integration/AppointmentInternalClient.java`
- Test: `videoCall-IM-service/src/test/java/com/ai/tutor/videocallimservice/integration/AppointmentInternalClientTest.java`

- [ ] **Step 1: Write failing delegation test**

```java
@Test
void shouldDelegateGetUserBasicToFeign() {
    when(client.getUserBasicById(206L)).thenReturn(success(imUser));
    ImUser out = internalClient.getUserBasicById(206L);
    assertEquals(206L, out.getId());
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `./mvnw -pl videoCall-IM-service -Dtest=AppointmentInternalClientTest test`  
Expected: FAIL.

- [ ] **Step 3: Implement Feign client + wrapper**

```java
@FeignClient(name = "tutor-appointment-service")
public interface AppointmentInternalFeignClient {
    @GetMapping("/internal/facade/users/{uid}/basic")
    BaseResponse<ImUser> getUserBasicById(@PathVariable("uid") Long uid);
}
```

`AppointmentInternalClient` 保持现有方法签名，内部改为调用 Feign + `unwrapData`。

- [ ] **Step 4: Run tests**

Run: `./mvnw -pl videoCall-IM-service -Dtest=AppointmentInternalClientTest test`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add videoCall-IM-service/src/main/java/com/ai/tutor/videocallimservice/integration/AppointmentInternalClient.java videoCall-IM-service/src/main/java/com/ai/tutor/videocallimservice/integration/feign/AppointmentInternalFeignClient.java videoCall-IM-service/src/test/java/com/ai/tutor/videocallimservice/integration/AppointmentInternalClientTest.java
git commit -m "refactor(im): migrate appointment internal calls to openfeign"
```

### Task 11: Configure Nacos Registration + Gateway Routes End-to-End

**Files:**
- Modify: `ai-tutor-gateway/src/main/resources/application.yml`
- Modify: `tutor-appointment-service/src/main/resources/application.yml`
- Modify: `videoCall-IM-service/src/main/resources/application.yml`
- Modify: `payment-service/src/main/resources/application.yml`
- Test: `ai-tutor-gateway/src/test/java/com/ai/tutor/gateway/config/GatewayRoutesSmokeTest.java`

- [ ] **Step 1: Write failing route smoke test**

```java
@Test
void shouldResolveChatRouteToImService() {
    Route route = routeLocator.getRoutes()
        .filter(r -> "im-chat-route".equals(r.getId()))
        .blockFirst();
    assertNotNull(route);
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `./mvnw -pl ai-tutor-gateway -Dtest=GatewayRoutesSmokeTest test`  
Expected: FAIL due missing route ids/predicates.

- [ ] **Step 3: Add route/discovery config**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: im-chat-route
          uri: lb://videoCall-IM-service
          predicates:
            - Path=/chat/**
```

Add discovery for all services:

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}
```

- [ ] **Step 4: Run gateway + module tests**

Run:
- `./mvnw -pl ai-tutor-gateway -Dtest=GatewayRoutesSmokeTest test`
- `./mvnw -pl tutor-appointment-service,videoCall-IM-service,payment-service test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add ai-tutor-gateway/src/main/resources/application.yml tutor-appointment-service/src/main/resources/application.yml videoCall-IM-service/src/main/resources/application.yml payment-service/src/main/resources/application.yml ai-tutor-gateway/src/test/java/com/ai/tutor/gateway/config/GatewayRoutesSmokeTest.java
git commit -m "feat(config): add nacos discovery and gateway route mappings"
```

### Task 12: Docs + Verification + Cleanup

**Files:**
- Modify: `README-backend.md`
- Modify: `ai-tutor-starter/src/main/resources/application.yml` (mark non-default startup)
- Optional Modify: legacy `integration.*.base-url` config docs/comments

- [ ] **Step 1: Write failing docs check**

Create a checklist file `docs/superpowers/plans/checks/gateway-migration-checklist.md` with unchecked launch commands; treat missing commands as failure criteria.

```markdown
- [ ] gateway startup command exists
- [ ] appointment startup command exists
- [ ] im startup command exists
- [ ] payment startup command exists
```

- [ ] **Step 2: Run verification command (expected fail before docs update)**

Run: `rg -n "ai-tutor-gateway|tutor-appointment-service|videoCall-IM-service|payment-service" README-backend.md`  
Expected: missing one or more service startup instructions.

- [ ] **Step 3: Update docs and cleanup notes**

Add explicit startup order:
1. Nacos
2. appointment (8081)
3. im (8082)
4. payment (8083)
5. gateway (8080)

Add statement: `ai-tutor-starter` 非默认生产运行方式。

- [ ] **Step 4: Full verification run**

Run:

```bash
./mvnw -pl ai-tutor-common,tutor-appointment-service,videoCall-IM-service,payment-service,ai-tutor-gateway -am test
```

Expected: PASS across touched modules.

- [ ] **Step 5: Commit**

```bash
git add README-backend.md ai-tutor-starter/src/main/resources/application.yml docs/superpowers/plans/checks/gateway-migration-checklist.md
git commit -m "docs: switch default runtime guide to gateway-based microservices"
```

## Execution Notes

1. 任务执行时必须遵循 `@superpowers/test-driven-development`：每个任务先写失败测试，再最小实现，再验证通过。
2. 收尾前必须遵循 `@superpowers/verification-before-completion`：给出实际执行命令与通过证据。
3. 若任何一步发现历史脏改动冲突，先停下并询问用户，不回滚用户已有变更。

