# Tasks

- [x] 1. 现状保护与基线
  - [x] 1.1 为当前 realtime session、transcript segment、audio chunk、finalize 流程补齐回归测试基线。
  - [x] 1.2 增加一组固定课堂 transcript fixture，用于比较旧总结和新 Agent 输出。
  - [x] 1.3 记录当前首段纪要延迟、LLM 调用次数、minutesOutline 重复率作为优化前指标。

- [x] 2. Memory 分层建设
  - [x] 2.1 新增 turn/event/episode/summary_patch 的 schema 和 repository。
  - [x] 2.2 扩展 `RealtimeStateStore`，支持 recent turns、recent events、episode、summary projection、memory version。
  - [x] 2.3 实现 Turn Aggregator，将碎片 transcript 合并为 speaker turn。
  - [x] 2.4 实现 Teaching Event Store，每个 event 必须带 evidence。
  - [x] 2.5 保持现有 `ai/state` 对前端兼容，新增字段放入 rawState 或可选字段。

- [x] 3. Prompt 拆分与专职 Agent
  - [x] 3.1 抽象 `RealtimeAgentLLMClient`，支持 template/mock 和真实 LLM。
  - [x] 3.2 实现 Teaching Event Extractor，第一阶段规则优先，LLM 作为可配置增强。
  - [x] 3.3 实现 Topic & Stage Tracker，输出 continue/transition/close_current 决策。
  - [x] 3.4 实现 Summary Patch Writer Prompt，只输出 patch schema，不输出完整 minutesOutline。
  - [x] 3.5 保留旧 `summarize_stage` fallback，并加 feature flag 控制新旧链路。

- [x] 4. 语义触发与 Orchestrator
  - [x] 4.1 实现固定触发兼容策略：时间 + 新增段数仍作为兜底。
  - [x] 4.2 实现首段快速触发：累计 3-5 个有效 turn 后生成第一段纪要。
  - [x] 4.3 实现主题切换、阶段收束、连续提问、明确作业、明确重点、信息密度触发。
  - [x] 4.4 实现 Orchestrator 输出 decision、triggerReasons、modelTier、contextPlan。
  - [x] 4.5 增加触发冷却时间和成本上限，防止 LLM 高频调用。

- [x] 5. 增量 Patch 与质量守卫
  - [x] 5.1 定义 patch schema：append_section/update_section/append_item/merge_section/mark_question/mark_homework/noop。
  - [x] 5.2 实现 patch apply，由代码维护 minutesOutline 和 section id。
  - [x] 5.3 实现重复检测，避免相似标题、相似 item 反复追加。
  - [x] 5.4 实现 Quality Guard，校验 schema、证据、长度、技术词、角色归因、作业可信度。
  - [x] 5.5 实现 guard repairable/reject 分支，拒绝时保留旧 projection 并记录原因。
  - [x] 5.6 持久化 patch 审计，支持排查“为什么这段总结出现/未出现”。

- [x] 6. LangGraph 编排升级
  - [x] 6.1 将现有 graph 替换为 normalize_turn -> extract_events -> commit_memory -> orchestrate 的前半段。
  - [x] 6.2 增加 build_context -> generate_patch -> quality_guard -> apply_patch -> persist_snapshot 分支。
  - [x] 6.3 确保任一 Agent 失败不会影响 transcript 保存和课堂主链路。
  - [x] 6.4 发布 transcript/event/summary patch 事件，保持后续 WebSocket/SSE 扩展空间。

- [x] 7. 上下文检索与课后复用
  - [x] 7.1 实现 Context Builder：最近 turns + 最近 events + 当前 episode + summary projection。
  - [x] 7.2 新增 `MemoryRetriever` 抽象，为后续向量检索预留接口。
  - [x] 7.3 课后报告生成优先读取 episode/student learning state，缺失时回退旧 report 输入。
  - [x] 7.4 设计 Student Learning State 的增量更新逻辑，先覆盖掌握点、疑问点、错因、下节课建议。

- [x] 8. 可观测性与配置
  - [x] 8.1 增加 Agent 指标：trigger、patch、guard reject、LLM calls、first summary latency。
  - [x] 8.2 增加配置项：新链路开关、首段触发阈值、语义触发冷却、patch 最大 items、guard 严格度。
  - [x] 8.3 增加日志 trace：lessonId、segmentRange、triggerReasons、patchId、guardStatus。

- [x] 9. 测试与验收
  - [x] 9.1 单测：Turn Aggregator、Event Extractor、Trigger Orchestrator、Patch Apply、Quality Guard。
  - [x] 9.2 API 测试：transcript segment 进入后可产生 event、trigger、patch，并保持 ai/state 兼容。
  - [x] 9.3 失败测试：LLM 输出非法 JSON、无 evidence、重复 item、角色归因错误时不污染 projection。
  - [x] 9.4 回归测试：ASR 降级、LLM 降级、finalize、课后 result 查询不回退。
  - [x] 9.5 离线 fixture 对比：新链路首段纪要更快，重复更少，历史 section 不被重写丢失。
  - [x] 9.6 将 `test-cases.md` 中 P0/P1 用例落成自动化测试，并作为实现完成的验收门槛。
