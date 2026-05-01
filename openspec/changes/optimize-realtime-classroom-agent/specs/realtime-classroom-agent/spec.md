## ADDED Requirements

### Requirement: 实时课堂 Agent MUST 拆分专职任务
系统 MUST 将实时课堂总结从单一大 Prompt 拆分为多个职责明确的 Agent 任务，包括转写归并、教学事件抽取、主题阶段追踪、触发编排、增量纪要生成、质量守卫和 Memory 提交。

#### Scenario: 转写片段进入后形成可复用教学上下文
- **WHEN** ASR 产生一条新的 transcript segment
- **THEN** 系统 MUST 保存原始 transcript
- **AND** 系统 MUST 尝试将其归并为 speaker turn
- **AND** 系统 MUST 从 turn 或 segment 中抽取 teaching event 候选
- **AND** 系统 MUST 将 turn 与 event 写入共享 Memory

#### Scenario: 专职任务失败不影响主链路
- **WHEN** 教学事件抽取、主题追踪或纪要 patch 生成失败
- **THEN** 系统 MUST 保留原始 transcript
- **AND** 系统 MUST NOT 中断课堂音视频主链路
- **AND** 系统 MUST NOT 用失败结果覆盖已有实时纪要

### Requirement: 系统 MUST 支持语义触发与固定触发协同
系统 MUST 保留时间和新增段数的固定触发作为兜底，并新增基于课堂语义事件的触发机制。

#### Scenario: 固定触发作为兜底
- **WHEN** 距离上次总结达到配置时间且新增 transcript 或 turn 达到配置阈值
- **THEN** Orchestrator MUST 允许触发一次总结决策

#### Scenario: 主题切换触发
- **WHEN** Topic & Stage Tracker 判断课堂从一个主题或阶段切换到另一个主题或阶段
- **THEN** Orchestrator SHOULD 触发 Summary Patch Writer 生成阶段收束或新增阶段 patch

#### Scenario: 首段快速触发
- **WHEN** 一节课开始后已经累计配置数量的有效 speaker turns 且尚未生成第一段纪要
- **THEN** Orchestrator SHOULD 触发第一段轻量纪要

#### Scenario: 低价值内容不触发强模型
- **WHEN** 最近内容主要是课堂管理、寒暄、静音等待或信息密度不足
- **THEN** Orchestrator SHOULD 选择 skip 或 extract_only
- **AND** 系统 SHOULD NOT 调用强模型生成正式纪要

### Requirement: LLM MUST 输出增量 patch 而不是完整重写纪要树
系统 MUST 要求 LLM 输出受控 patch schema，由系统代码负责应用 patch 到实时纪要投影。

#### Scenario: 新阶段追加
- **WHEN** patch 类型为 `append_section`
- **THEN** 系统 MUST 由代码生成或校验 section id
- **AND** 系统 MUST 将新 section 追加到 `minutesOutline`
- **AND** 系统 MUST 保留已有有效 section

#### Scenario: 当前阶段更新
- **WHEN** patch 类型为 `update_section`
- **THEN** 系统 MUST 只允许更新目标 section 的 summary、endSegment 和 items
- **AND** 系统 MUST NOT 让 LLM 删除无关历史 section

#### Scenario: Patch 无有效信息
- **WHEN** patch 类型为 `noop`
- **THEN** 系统 MUST 不改变 `minutesOutline`
- **AND** 系统 MAY 更新内部触发审计状态

### Requirement: Agent MUST 共享同一个 Memory Store
所有实时课堂 Agent MUST 读写统一 Memory Store，Memory MUST 至少区分 Raw Transcript、Speaker Turn、Teaching Event、Episode Memory、Realtime Summary Projection 和 Student Learning State。

#### Scenario: Summary Patch Writer 构造上下文
- **WHEN** Orchestrator 决定生成 patch
- **THEN** Context Builder MUST 从共享 Memory 中选择最近 turns、最近 events、当前 episode 和当前 summary projection
- **AND** Context Builder MUST NOT 默认把整节课 transcript 全量塞入 LLM 上下文

#### Scenario: 课后报告复用 Memory
- **WHEN** 课堂结束并生成课后报告
- **THEN** 系统 SHOULD 优先使用 episode memory 和 student learning state
- **AND** 当新 Memory 缺失时 MUST 回退现有课后报告输入链路

### Requirement: Orchestrator MUST 决定调用路径和模型层级
系统 MUST 引入 Orchestrator，根据 Memory、触发信号、冷却时间和成本预算决定是否调用 Agent 以及调用哪个模型层级。

#### Scenario: 普通片段只做轻量处理
- **WHEN** 新增 transcript 没有明显教学事件或触发信号
- **THEN** Orchestrator SHOULD 输出 `extract_only` 或 `skip`
- **AND** 系统 SHOULD 只更新 transcript、turn、event 或基础 state

#### Scenario: 明确触发信号调用总结
- **WHEN** 出现主题切换、阶段收束、连续学生问题、明确作业或明确重点
- **THEN** Orchestrator SHOULD 输出 `generate_patch`
- **AND** 输出 MUST 包含 triggerReasons、modelTier 和 contextPlan

#### Scenario: 触发冷却防止高频调用
- **WHEN** 最近一次强模型 patch 生成仍处于冷却窗口内
- **THEN** Orchestrator SHOULD 延迟或降级本次总结
- **AND** 系统 MUST 记录被冷却抑制的触发原因

### Requirement: Quality Guard MUST 在 patch 落地前校验输出
系统 MUST 在 patch 应用前运行质量守卫，防止非法、重复、无证据或角色归因错误的内容污染实时纪要。

#### Scenario: Patch 缺少证据被拒绝
- **WHEN** patch 新增的重点、问题、作业或纪要 item 没有引用可追溯 evidence
- **THEN** Quality Guard MUST 拒绝该 patch 或移除无证据字段
- **AND** 系统 MUST 保留旧 summary projection

#### Scenario: Patch 包含重复 item
- **WHEN** patch 新增 item 与当前 section 近期 item 高度相似
- **THEN** Quality Guard SHOULD 去重或拒绝重复部分

#### Scenario: Patch 包含技术词
- **WHEN** patch 面向课堂展示的字段包含 ASR、LLM、模型、置信度等技术词
- **THEN** Quality Guard MUST 修复或拒绝该 patch

#### Scenario: 角色归因错误
- **WHEN** patch 将老师讲解误作为学生问题，或将学生问题误作为老师重点
- **THEN** Quality Guard SHOULD 拒绝该字段
- **AND** 系统 MUST 记录 guard rejection reason

### Requirement: 实时总结 MUST 保持现有前端契约兼容
系统 MUST 继续提供现有实时总结展示所需字段，包括 `minutesOutline`、`latestStageSummary`、`studentQuestions`、`homeworkCandidates`、`keyPoints` 和 `activeSectionTitle`。

#### Scenario: 新 Agent 生成 patch 后前端可继续展示
- **WHEN** patch 成功应用到 summary projection
- **THEN** `ai/state` 返回的数据 MUST 包含可供现有前端展示的 `minutesOutline`
- **AND** 前端无需理解 patch schema 即可展示最新纪要

#### Scenario: 新 Agent 关闭时回退旧链路
- **WHEN** 新 Agent feature flag 关闭
- **THEN** 系统 MUST 使用现有阶段总结链路
- **AND** 现有 realtime API 行为 MUST 不回退
