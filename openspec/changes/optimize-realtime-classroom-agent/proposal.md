# Proposal: Optimize Realtime Classroom Agent

## Why

当前课堂实时总结已经打通了“浏览器麦克风旁路采集 -> ASR -> transcript segment -> Redis state -> LangGraph 阶段总结 -> 前端轮询展示”的主链路，但智能体仍然偏线性：

- 一个大 Prompt 同时负责主题判断、阶段总结、纪要树更新、学生问题、作业候选和课堂重点提取。
- LLM 触发主要依赖固定时间窗口和新增段数，无法理解课堂语义节奏。
- LLM 输出直接重写 `minutesOutline`，历史纪要容易被改坏、重复或丢失。
- 当前 LangGraph 只有规则抽取、存储、总结、发布几个节点，没有独立 Orchestrator、质量守卫和共享 Memory 分层。
- 上下文主要是 Redis 最近 transcript + 当前 state，缺少 turn、teaching event、episode、student learning state 等面向教学的长期结构化记忆。

业务目标是把实时总结从“低频阶段纪要”升级成“课堂理解 Agent”：能更快响应课堂阶段变化，更稳定维护右侧纪要树，并为课后总结、学生画像和后续跨课节智能能力提供可信上下文。

## What Changes

- 将单一 `summarize_stage` Prompt 拆分为多个专职任务：
  - Transcript/Turn Normalizer
  - Teaching Event Extractor
  - Topic & Stage Tracker
  - Trigger Orchestrator
  - Summary Patch Writer
  - Quality Guard
  - Memory Committer
- 在固定触发基础上增加语义触发：
  - 首段快速触发
  - 主题切换触发
  - 阶段收束触发
  - 学生连续提问触发
  - 作业/重点显式信号触发
  - 时间与段数兜底触发
- 将 LLM 输出从完整 `minutesOutline` 改为增量 patch：
  - `append_section`
  - `update_section`
  - `append_item`
  - `merge_section`
  - `mark_question`
  - `mark_homework`
  - `noop`
- 将 LangGraph 编排升级为多 Agent 共享 Memory：
  - Orchestrator 决定本次 segment 是否只入库、轻量抽取、触发 patch、触发质量修复或只发布状态。
  - 各 Agent 读写统一 Memory Store，不各自维护状态副本。
  - Quality Guard 在 patch 落地前校验 JSON、证据、重复、长度、幻觉和教学角色归因。
- 优化上下文存储为分层 Memory：
  - Raw Transcript
  - Speaker Turn
  - Teaching Event
  - Episode Memory
  - Realtime Summary Projection
  - Student Learning State
  - Optional Vector Memory
- 保持现有前端 `ai/state` 展示契约尽量兼容，新增字段只作为增强，不破坏当前课堂页。

## Out of Scope

- 不重做 ASR provider 本身，不改变 LiveKit 音视频链路。
- 不在本变更中实现跨课程长期画像的完整产品化展示。
- 不要求前端从轮询立即切换到 WebSocket/SSE；实时推送可作为后续优化。
- 不改变课后报告当前对外接口，课后报告只复用新的 Memory 作为更高质量输入。

## Impact

- 主要影响：`ai-agent-service/app/realtime/*`
- 次要影响：`ai-agent-service/app/storage/database.py`、`app/repositories/realtime_repository.py`、`app/schemas/realtime.py`
- 兼容影响：`live-class-service` 可继续按现有 `ai/state` 字段读取；需要时可透传新增 state 字段。
- 测试影响：需新增 agent 编排、触发策略、patch 应用、质量守卫、Memory 分层的单测和 API 回归测试。
