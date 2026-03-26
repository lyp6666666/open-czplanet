---
title: 验收检查表（移除 ai-tutor-starter）
status: draft
date: 2026-03-25
---

# 构建检查

- [ ] `sh ./mvnw test` 通过
- [ ] `sh ./mvnw -pl ai-tutor-gateway,tutor-appointment-service,videoCall-IM-service,payment-service,ai-tutor-admin -am test` 通过
- [ ] 父工程 `pom.xml` 不再包含 `ai-tutor-starter` module

# Nacos Config（每服务）

对每个服务分别验证（`ai-tutor-gateway/tutor-appointment-service/videoCall-IM-service/payment-service/ai-tutor-admin`）：

- [ ] 启动日志包含 Nacos config 拉取/订阅日志（至少出现服务专用 DataId）
- [ ] Nacos 缺失配置时仍能启动（optional import + 本地默认值兜底）
- [ ] Nacos 配置变更后可按预期刷新（仅对标注 refreshEnabled 的配置项）

# Nacos Discovery（每服务）

对每个服务分别验证：

- [ ] 启动日志包含 `REGISTER-SERVICE` 或 `nacos registry ... register finished`
- [ ] Nacos 控制台（对应命名空间）服务列表可见：
  - [ ] 服务名与 `spring.application.name` 一致
  - [ ] 实例数 > 0，健康实例数 > 0

# 运行时最小验证

- [ ] 网关可启动且路由配置加载成功（至少看到 routes 初始化日志）
- [ ] 预约服务可启动（不因 MinIO/DB/Redis 缺失配置直接失败）
- [ ] IM 服务可启动
- [ ] 支付服务可启动
- [ ] Admin 服务可启动

# 一键启动脚本

- [ ] 提供 `up`：一键启动 docker 依赖与所有服务
- [ ] 提供 `down/stop`：一键停止服务（并可选停止 docker）
- [ ] 日志输出到 `.logs/`（不打印敏感信息）

# 文档与沉淀

- [ ] `common-used.md` 更新：微服务启动命令与脚本用法
- [ ] `ai-platform-context` skill 的 Change Log 已追加本次改造摘要

