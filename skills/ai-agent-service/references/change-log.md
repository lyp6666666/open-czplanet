# ai-agent-service 维护记录

## 2026-04-20

- 创建第一版 `ai-agent-service`
- 实现：
  - 课后报告生成任务
  - IM 沟通摘要任务
  - Redis/RQ 任务队列
  - 模板 LLM provider
- 同步：
  - `sqlDoc/migrations/20260420_ai_agent_service.sql`
  - `sqlDoc/huoyue.sql`
  - 网关 `/internal/ai/**` 路由
  - Nacos 模板 `docs/nacos/templates/ai-agent-service-dev.yaml`
- 约束：
  - 当前仅内部服务调用
  - 后续接入 LangChain / LangGraph

## 2026-04-20

- 实现实时课堂 AI 的 P1/P2 骨架
- 新增：
  - LangGraph 实时处理工作流
  - LangChain 火山方舟 LLM 接入
  - 腾讯云实时 ASR provider 适配
  - 课堂会话 / transcript / 阶段摘要表
- 配置：
  - 火山方舟 EP：`ep-20260420222959-g4vrz`
  - 腾讯云 ASR 配置项已加入 Nacos 模板
