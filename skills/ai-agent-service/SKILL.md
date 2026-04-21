---
name: "ai-agent-service-context"
description: "当任务涉及 ai-agent-service 的实现、配置、接入、数据库、任务队列、主链路状态或后续 LangChain/LangGraph 演进时使用。先阅读 references/service-overview.md，再根据改动类型查看 references/integration.md、references/runtime.md 和 references/change-log.md。任何 schema 变更必须同步 sqlDoc/huoyue.sql、迁移 SQL，并在共享远程开发机 111.228.20.88 上应用验证。"
---

# ai-agent-service 上下文

这个 skill 只服务于 `ai-agent-service` 相关工作。

## 服务定位

- 这是平台内部 AI Agent 微服务，不直接面向终端用户。
- 第一版能力：
  - 课后报告生成
  - IM 沟通摘要
- 当前调用方式：
  - 由 Java 微服务通过内部接口调用
  - 通过网关内部路径 `/internal/ai/**` 转发
- 第一版默认 LLM provider：
  - `template`
- 第一版任务队列：
  - Redis + RQ
- 第一版不包含：
  - ASR
  - 实时音频处理
  - 向量库
  - LangChain / LangGraph 运行时

## 开发规则

- 这是一个可持续演进为多 Agent 服务的基础骨架。
- 新能力优先沿用：
  - `llm/`
  - `pipelines/`
  - `tasks/`
  - `repositories/`
- 不要把业务逻辑直接塞进 API 层。
- 对外接口保持内部风格，不直接暴露前端专用契约。
- 任何数据库变更必须同步：
  - `sqlDoc/migrations/*.sql`
  - `sqlDoc/huoyue.sql`
- 任何数据库变更也必须同步到：
  - `111.228.20.88`

## 阅读顺序

1. `references/service-overview.md`
2. `references/integration.md`
3. `references/runtime.md`
4. `references/change-log.md`

## 参考文件

- 服务概览：
  `references/service-overview.md`
- 主链路集成与边界：
  `references/integration.md`
- 运行配置与部署说明：
  `references/runtime.md`
- 持续维护记录：
  `references/change-log.md`
