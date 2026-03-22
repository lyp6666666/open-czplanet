# 网关统一鉴权与 Nacos 注册路由设计（微服务化）

日期：2026-03-22  
状态：已确认（待实施）  
作者：Codex（与用户共创）

## 1. 背景与目标

当前工程以 `ai-tutor-starter` 聚合启动为主，多个业务模块运行在同一进程内，`RequestHolder` 主要由预约模块拦截器注入。用户希望完成以下演进：

1. 前端暂不改动任何请求路径。
2. 新增统一网关进行路由与身份注入。
3. 所有业务服务通过 Nacos 注册发现。
4. 服务之间改为 OpenFeign 调用。
5. 用户侧鉴权统一上收至网关，业务服务不再自行验 JWT。
6. 管理端（`/api/admin/**`）本次保持现状，不并入该鉴权链路。

## 2. 已确认范围

### 2.1 In Scope

1. 新增独立网关模块，统一 JWT 校验、路由和身份头注入。
2. appointment / im / payment 服务接入 Nacos Discovery。
3. appointment / im / payment 服务接入统一“签名头验签 + RequestHolder 注入”机制。
4. 服务间调用改为 OpenFeign（通过 Nacos 服务名直连，非经网关转发）。
5. 现有 `RestTemplate + base-url` 跨服务调用逐步替换为 Feign。
6. 保持前端路径兼容：`/user/**`、`/api/**`、`/chat/**`、`/org/**` 等不变。

### 2.2 Out of Scope

1. `ai-tutor-admin` 鉴权链路改造（暂不改）。
2. 前端接口路径和鉴权方式改造（暂不改）。
3. 本次不引入复杂重放防护存储（如 Redis nonce），先采用短时窗签名策略。

## 3. 现状问题

1. 服务边界不清：聚合启动导致“模块分离、运行未分离”。
2. 鉴权位置分散：JWT 校验逻辑在业务服务拦截器内，不利于统一治理。
3. 远程调用不统一：存在 `base-url` 硬编码/配置方式，缺乏服务发现与弹性能力。
4. 安全模型不一致：内部调用依赖 `X-Internal-Token` 或上下文假设，无法统一身份传播模型。

## 4. 目标架构

## 4.1 组件视图

1. `ai-tutor-gateway`（新增）  
职责：入口网关、JWT 校验、用户身份注入、路由转发、签名生成。

2. `ai-tutor-common`（增强）  
职责：提供统一身份签名工具、服务侧验签组件、Feign 身份透传组件、自动配置。

3. 业务服务  
- `tutor-appointment-service`
- `videoCall-IM-service`
- `payment-service`  
职责：不再自行验 JWT；统一校验网关/服务间签名头并注入 `RequestHolder`。

4. `ai-tutor-admin`（保持现状）  
继续使用现有 `AdminAuthInterceptor`。

5. Nacos  
职责：配置中心 + 注册中心（服务注册发现、动态路由配置可选）。

## 4.2 请求主链路（用户侧）

1. 客户端请求网关，携带 `Authorization: Bearer <jwt>`。
2. 网关校验 JWT 并解析：`userId`、`role`（code）。
3. 网关将 role 映射为数字角色并注入头：
   - `X-Uid`
   - `X-Role`（1/2/3）
   - `X-Ts`
   - `X-Auth-Sign`
4. 网关按路由规则转发到目标业务服务。
5. 业务服务统一验签 + 时窗校验，通过后写入 `RequestHolder`。
6. 控制器/服务层继续按既有方式读取 `RequestHolder.get().getUid()/getRole()`。

## 4.3 服务间调用链路（Feign）

1. 服务 A 发起 Feign 调用服务 B（服务名发现）。
2. Feign 拦截器读取当前线程 `RequestHolder` 身份并注入同样签名头。
3. 服务 B 使用同一验签逻辑处理，统一身份上下文。

## 5. 路由与路径兼容设计

## 5.1 前端路径保持不变

前端仍请求原路径；路由在网关内部完成。

## 5.2 首版路由建议

1. `tutor-appointment-service`
   - `/user/**`
   - `/api/v1/**`
   - `/appointment/**`
   - `/org/**`
2. `videoCall-IM-service`
   - `/chat/**`
3. `payment-service`
   - `/payment/**`

## 5.3 网关白名单（不验 JWT）

1. `/user/loginOrRegister`
2. `/user/sendcode`
3. `/api/v1/public/**`
4. `/payment/notify/**`
5. `/swagger-ui/**`
6. `/v3/api-docs/**`
7. `/actuator/**`（按环境可进一步收紧）

## 6. 身份与签名协议

## 6.1 头协议

1. `X-Uid`: Long
2. `X-Role`: Integer（1/2/3）
3. `X-Ts`: 毫秒时间戳
4. `X-Auth-Sign`: HMAC-SHA256 签名值（hex 或 base64，统一一种）

## 6.2 签名原文

建议原文：

`uid + "\n" + role + "\n" + ts + "\n" + method + "\n" + path`

说明：method/path 纳入签名，避免头在不同 API 间被重放复用。

## 6.3 验签规则

1. 必填头完整，否则拒绝。
2. `|now - ts| <= allowedSkewMs`（默认 60000ms）。
3. 按同一密钥重算签名，常量时间比较。
4. 失败返回 401/403。

## 6.4 密钥管理

1. 配置键建议：`gateway.sign.secret`。
2. 所有参与方（gateway + 业务服务）通过 Nacos 下发同一密钥。
3. 支持密钥轮换（后续可扩展双 key 验证窗口）。

## 7. 模块与代码改造设计

## 7.1 新增模块：`ai-tutor-gateway`

依赖建议：

1. `spring-cloud-starter-gateway`
2. `spring-cloud-starter-alibaba-nacos-discovery`
3. `spring-cloud-starter-alibaba-nacos-config`
4. `jjwt`（或复用公共 jwt 校验能力）

核心能力：

1. `GatewayAuthFilter`：验 JWT、构造身份、签名注入。
2. 路由配置：静态 yml + Nacos 动态配置（可二选一逐步演进）。
3. 统一日志：traceId + uid + routeId。

## 7.2 `ai-tutor-common` 增强

新增公共能力（建议命名）：

1. `GatewaySignProperties`
2. `GatewaySignatureUtils`
3. `GatewayIdentityWebInterceptor`（服务侧验签并写入 `RequestHolder`）
4. `FeignIdentityRequestInterceptor`（服务间透传并签名）
5. `GatewayIdentityAutoConfiguration`（可开关）

## 7.3 业务服务改造

appointment / im / payment：

1. 移除或停用现有 JWT 拦截器（用户侧路径）。
2. 注册 `GatewayIdentityWebInterceptor` 到 WebMvc。
3. 接入 OpenFeign 与 discovery。
4. 替换跨服务 `RestTemplate` 调用为 Feign 客户端。
5. 保持 Controller/Service 主业务逻辑尽量不变（降低回归风险）。

## 7.4 Admin 服务策略

`ai-tutor-admin` 继续使用当前独立鉴权，不参与本期用户侧统一模型。

## 8. Nacos 与配置策略

每个服务新增：

1. `spring.cloud.nacos.discovery.server-addr`
2. `spring.cloud.nacos.discovery.namespace/group`
3. `spring.application.name`（确保唯一稳定）

公共配置建议：

1. `gateway.sign.secret`
2. `gateway.sign.allowed-skew-ms`
3. 网关路由与白名单配置（按环境分组）

## 9. OpenFeign 设计

## 9.1 调用策略

1. 以服务名声明 Feign Client（如 `name = "videoCall-IM-service"`）。
2. 禁止继续使用 `http://localhost:xxxx` 形式 base-url 作为主路径。

## 9.2 身份透传

1. Feign 请求拦截器从 `RequestHolder` 读取 uid/role。
2. 构造 `X-Uid/X-Role/X-Ts/X-Auth-Sign`。
3. 下游统一验签，避免“仅网关入口有身份、服务间丢身份”问题。

## 10. 迁移计划（分阶段）

## 10.1 Phase A：基础设施

1. 新增 `ai-tutor-gateway` 骨架与路由。
2. common 增加签名与验签组件。
3. 各业务服务启用 Nacos discovery（先不切流）。

## 10.2 Phase B：鉴权切换

1. 网关启用 JWT 校验与身份头注入。
2. 业务服务启用签名验签拦截器。
3. 下游原 JWT 拦截器改为禁用（或仅保留 admin）。

## 10.3 Phase C：调用切换

1. 替换 appointment -> im、payment -> im 等远程调用到 Feign。
2. 移除旧 `RestTemplate base-url` 主路径。

## 10.4 Phase D：运行模式收敛

1. 生产/预发切换为“网关 + 多服务独立进程”。
2. `ai-tutor-starter` 退为本地开发兼容或后续下线。

## 11. 测试与验收

## 11.1 功能回归

1. 登录、验证码、公共首页接口。
2. 聊天主链路（建房、发消息、会话列表、已读）。
3. 支付下单与回调。
4. 服务间 facade 行为一致性。

## 11.2 安全验证

1. 无 JWT 请求网关应被拒绝（白名单除外）。
2. 伪造 `X-Uid/X-Role` 直打业务服务应被拒绝。
3. 过期 `X-Ts` 应被拒绝。
4. 篡改 method/path 后签名应不通过。

## 11.3 观测要求

1. 网关日志含路由命中、uid、traceId。
2. 服务日志含验签结果（脱敏后）与 traceId 传递。
3. 关键指标：401/403 比例、路由 5xx、Feign 调用失败率。

## 12. 风险与回滚

## 12.1 主要风险

1. 路由配置遗漏导致接口不可达。
2. 时钟偏差导致误拒绝。
3. 某些内部调用未透传身份导致 401/403。

## 12.2 回滚策略

1. 网关保留开关：可临时旁路鉴权（仅应急环境）。
2. 服务保留短期开关：兼容旧链路（灰度期可用）。
3. Nacos 配置可热回退到上一个版本。

## 13. 设计结论

采用“网关统一鉴权 + 服务侧签名验签 + Nacos 注册发现 + OpenFeign 服务直连”的方案，满足以下约束：

1. 前端路径零改动。
2. 用户侧身份治理集中化。
3. 微服务运行形态清晰（彻底脱离聚合进程依赖）。
4. 服务间调用标准化，便于后续扩展与治理。

