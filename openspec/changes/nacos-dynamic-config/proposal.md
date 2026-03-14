## Why

当前服务的部分业务配置（首页轮播图、短信验证码是否真实发送、是否启用真实支付）需要在不重启服务的情况下在线上快速试验与切换。引入线上 Nacos（2.2.3）作为统一配置中心，可以实现配置集中管理与运行时动态刷新，同时为开发/生产环境提供清晰隔离。

## What Changes

- 接入 Nacos 配置中心（Spring Cloud Alibaba Nacos Config），服务启动时从 Nacos 拉取配置并支持运行时刷新
- 将轮播图、短信真实发送开关、真实支付开关等配置收敛为结构化配置项（ConfigurationProperties），并在业务代码中统一读取
- 引入环境隔离方案（开发/生产），并约定 Nacos 中的 namespace / group / dataId 组织方式
- 补充一份可操作的线上 Nacos 配置指南（包含创建命名空间、创建配置、发布与验证热更新）

## Capabilities

### New Capabilities

- `nacos-external-config`: 使用 Nacos 托管应用配置，并在运行时刷新到 Spring 容器
- `runtime-business-toggles`: 支持轮播图、短信、支付等业务开关动态生效（无重启）

### Modified Capabilities

- （无）

## Impact

- 影响模块：`ai-tutor-starter`（启动配置、配置读取方式、相关业务逻辑）
- 新增依赖：Spring Cloud Alibaba Nacos Config（以及对应的 Spring Cloud 依赖管理）
- 运维依赖：线上 Nacos（106.52.179.248:8848）可用性将影响应用启动/配置刷新
- 兼容性注意：需要确认当前 Spring Boot / Spring Cloud 版本，以选择匹配的 Spring Cloud Alibaba 版本与配置方式（bootstrap / config import）
