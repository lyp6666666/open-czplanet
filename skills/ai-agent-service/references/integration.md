# 集成边界

## 当前主链路位置

```text
Java 微服务
  -> Gateway /internal/ai/**
  -> ai-agent-service
```

## 当前调用约束

- 仅内部服务调用
- 不直接作为前端公开 API
- 当前应由 Java 服务先完成：
  - 权限校验
  - 业务上下文整理
  - 任务触发时机控制

## 现阶段推荐调用方

- `live-class-service`
  - 课后报告生成
- `videoCall-IM-service`
  - IM 沟通摘要

## 当前不建议

- 前端直接调用 `ai-agent-service`
- 在 `ai-agent-service` 内重复实现登录鉴权
- 直接让 AI 服务负责主业务状态机
