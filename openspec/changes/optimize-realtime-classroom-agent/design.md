# Design: Realtime Classroom Agent Optimization

## 1. Current State

当前课堂 AI 的实时路径是：

```text
audio chunk
  -> ASR runtime
  -> transcript segment
  -> save ai_lesson_transcript
  -> LangGraph extract_signals
  -> Redis transcript list + state
  -> should_run_llm by time + segment count
  -> summarize_stage
  -> overwrite/update minutesOutline
  -> save ai_lesson_stage_summary
```

现有实现优点是链路简单、状态少、对前端契约友好。主要瓶颈是“一个大 Prompt + 一个粗触发器 + 一个平面 state”。优化时必须保留主链路的稳定性，把新能力先作为内部编排增强，不让课堂音视频和前端展示被 AI 失败拖垮。

## 2. Target Architecture

目标链路：

```text
ASR transcript segment
  -> Raw Transcript Store
  -> Turn Aggregator
  -> Teaching Event Extractor
  -> Shared Memory Store
  -> Orchestrator
       -> no-op / lightweight memory update
       -> semantic trigger
       -> summary patch generation
       -> quality guard
       -> patch apply
  -> Realtime Summary Projection
  -> Stage Summary Snapshot
  -> Frontend ai/state
```

核心原则：

1. LLM 不直接重写完整纪要树。
2. Prompt 按职责拆分，输出都有稳定 schema。
3. Orchestrator 是唯一调度者，避免每个节点自行决定是否调用强模型。
4. Memory 是共享事实源，Agent 只读写同一份结构化状态。
5. Quality Guard 失败时宁可不更新，也不把不可信内容展示给课堂用户。

## 3. Prompt 拆分

### 3.1 Turn Normalizer

职责：把碎片化 transcript segment 合并成更适合教学理解的 speaker turn。

输入：

- 最近新增 segment
- 当前 open turn
- speaker
- 时间间隔

输出：

```json
{
  "action": "append_to_open_turn|close_turn|create_turn",
  "turn": {
    "turnId": "turn-12",
    "speaker": "teacher",
    "startSegment": 21,
    "endSegment": 25,
    "text": "完整发言内容",
    "roleHint": "explanation|question|instruction|classroom_management|unknown"
  }
}
```

实现建议：

- 第一阶段优先用规则实现，不必上 LLM。
- 同 speaker 且间隔短、文本不是明显阶段切换时合并。
- 学生短问句可以单独成 turn，避免被老师解释合并掉。

### 3.2 Teaching Event Extractor

职责：从 turn 中抽取教学事件，不直接写展示文案。

事件类型：

- `concept_explained`
- `example_started`
- `example_solved`
- `student_question`
- `misconception_detected`
- `teacher_emphasis`
- `homework_assigned`
- `stage_transition`
- `practice_or_drill`
- `classroom_management`

输出：

```json
{
  "events": [
    {
      "eventId": "evt-42",
      "type": "student_question",
      "topic": "一次函数图像",
      "text": "为什么 k 越大图像越陡？",
      "confidence": 0.86,
      "evidence": {
        "turnIds": ["turn-12"],
        "segmentRange": [31, 32],
        "quotes": ["为什么 k 越大图像越陡"]
      }
    }
  ]
}
```

实现建议：

- 规则引擎继续保留，但输出升级成 event candidates。
- 小模型或 LLM 只在规则不确定、长 turn 或主题切换候选时介入。
- 每个事件必须带 evidence，后续 patch 必须引用 evidence。

### 3.3 Topic & Stage Tracker

职责：维护当前课堂阶段和主题，不负责写纪要。

输出：

```json
{
  "stageDecision": "continue|transition|close_current|unknown",
  "currentTopic": "一次函数图像",
  "stageType": "review|teach|practice|qa|homework|closing|unknown",
  "reason": "学生提问后老师开始讲解新例题",
  "confidence": 0.78
}
```

实现建议：

- 由 Orchestrator 调用。
- 检测到 `stage_transition`、主题变化、长时间同主题沉淀、作业/收尾关键词时提高触发权重。

### 3.4 Summary Patch Writer

职责：基于 Memory 和触发原因生成增量 patch。

输入：

- 当前 summary projection
- 当前 episode memory
- 最近 events
- 最近 turns
- trigger reason

输出示例：

```json
{
  "patchId": "patch-88",
  "patchType": "update_section",
  "targetSectionId": "section-3",
  "summary": "本阶段围绕一次函数图像斜率展开，重点解释 k 值变化与图像倾斜程度的关系。",
  "appendItems": [
    {
      "title": "斜率影响倾斜",
      "detail": "老师通过图像变化说明 k 越大，直线越陡。",
      "evidenceEventIds": ["evt-42", "evt-43"]
    }
  ],
  "studentQuestions": [
    {
      "text": "为什么 k 越大图像越陡？",
      "evidenceEventIds": ["evt-42"]
    }
  ],
  "homeworkCandidates": [],
  "keyPoints": [
    {
      "text": "k 值决定一次函数图像的倾斜程度。",
      "evidenceEventIds": ["evt-43"]
    }
  ]
}
```

patch 类型：

- `append_section`：新增课堂阶段。
- `update_section`：更新当前阶段 summary 或追加 items。
- `append_item`：仅追加小标题。
- `merge_section`：合并重复阶段。
- `mark_question`：只更新学生问题。
- `mark_homework`：只更新作业候选。
- `noop`：内容不足，不更新展示。

### 3.5 Quality Guard

职责：patch 落地前校验。

校验规则：

- JSON schema 合法。
- patch 引用的 section/event/turn 必须存在。
- 新增内容必须有 evidence。
- 标题、summary、detail 长度合规。
- 不允许出现 ASR、LLM、模型、置信度等技术词。
- 不允许无依据新增作业。
- 不允许把 teacher 的解释误归类为 student question。
- 不允许与最近 N 个 item 高相似重复。
- 不允许一次 patch 删除大量历史内容。

输出：

```json
{
  "accepted": true,
  "severity": "pass|repairable|reject",
  "reasons": [],
  "repairedPatch": {}
}
```

第一阶段可以规则校验为主；后续可增加 LLM judge，但 judge 不应阻塞课堂主链路。

## 4. Semantic Trigger Strategy

现有固定触发继续保留，作为兜底。新触发体系由 Orchestrator 综合打分。

### 4.1 Trigger Signals

- 首段快速触发：
  - 第一节课开始后累计 3-5 个有效 turn，生成第一段轻量纪要。
- 主题切换触发：
  - Topic Tracker 判断 `transition` 且 confidence 达阈值。
- 阶段收束触发：
  - 出现“总结一下”“这题就到这里”“接下来”等收束/切换信号。
- 学生连续提问触发：
  - 最近窗口内学生问题达到 2 个以上。
- 重点/作业显式触发：
  - 出现明确 `teacher_emphasis` 或 `homework_assigned` event。
- 信息密度触发：
  - 最近新增 event 数达到阈值，例如 5 个。
- 固定兜底触发：
  - 默认 300 秒 + 新增 8 段 transcript。

### 4.2 Trigger Decision

Orchestrator 输出：

```json
{
  "decision": "skip|extract_only|generate_patch|repair_patch",
  "triggerReasons": ["topic_transition", "student_questions"],
  "modelTier": "none|small|strong",
  "contextPlan": {
    "recentTurns": 12,
    "recentEvents": 20,
    "includeCurrentEpisode": true,
    "includeHistoricalEpisodes": false
  }
}
```

策略：

- 普通 segment：只入库和轻量抽取。
- 有语义事件但不足以总结：更新 Memory，不调用 Summary Patch Writer。
- 明确阶段变化：调用强模型生成 patch。
- 首段纪要：可调用小模型或模板生成短 patch。
- 固定兜底：调用强模型，但如果信息密度低可降级为 noop。

## 5. Shared Memory Design

### 5.1 Raw Transcript

继续使用现有 `ai_lesson_transcript`，建议增强字段：

- `segment_id`
- `participant_id`
- `asr_confidence`
- `source`
- `received_at`

兼容做法：先不改旧字段，通过新增 nullable 字段或 JSON 扩展。

### 5.2 Speaker Turn

新增 `ai_lesson_turn`：

- `id`
- `lesson_id`
- `turn_id`
- `speaker`
- `start_segment`
- `end_segment`
- `text`
- `role_hint`
- `metadata_json`
- `created_at`
- `updated_at`

Redis 也保留最近 turns：

```text
ai:lesson:{lesson_id}:turns
```

### 5.3 Teaching Event

新增 `ai_lesson_teaching_event`：

- `id`
- `lesson_id`
- `event_id`
- `event_type`
- `topic`
- `text`
- `confidence`
- `evidence_json`
- `status`
- `created_at`

Redis 保留最近 events：

```text
ai:lesson:{lesson_id}:events
```

### 5.4 Episode Memory

新增 `ai_lesson_episode`：

- `id`
- `lesson_id`
- `episode_id`
- `title`
- `stage_type`
- `topic`
- `start_segment`
- `end_segment`
- `summary`
- `key_points_json`
- `student_questions_json`
- `homework_json`
- `evidence_json`
- `status`
- `created_at`
- `updated_at`

Episode 是实时总结和课后报告之间的桥。

### 5.5 Summary Projection

Redis state 继续保留前端需要的投影：

- `minutesOutline`
- `activeSectionTitle`
- `latestStageSummary`
- `studentQuestions`
- `homeworkCandidates`
- `keyPoints`

新增内部字段：

- `memoryVersion`
- `summaryVersion`
- `lastPatchId`
- `lastTriggerReasons`
- `currentEpisodeId`
- `currentStageType`
- `guardRejectedCount`

### 5.6 Student Learning State

新增或先以 JSON 存在 Redis：

```json
{
  "masteredPoints": [],
  "uncertainPoints": [],
  "misconceptions": [],
  "questionPatterns": [],
  "teacherInterventions": [],
  "nextLessonHints": []
}
```

第一阶段只在课中低频更新，课后报告优先消费。

### 5.7 Vector Memory

可作为第二阶段：

- 对 turn/event/episode 做 embedding。
- 实时总结上下文由“最近窗口 + 当前 episode + 相关历史检索”组成。
- 如果暂不引入向量库，可先保留抽象接口 `MemoryRetriever`。

## 6. LangGraph Orchestration

目标 Graph：

```text
normalize_turn
  -> extract_events
  -> commit_memory
  -> orchestrate
      -> publish
      -> build_context
          -> generate_patch
          -> quality_guard
          -> apply_patch
          -> persist_snapshot
          -> publish
```

节点职责：

- `normalize_turn`：维护 turn。
- `extract_events`：规则 + 小模型抽取 event。
- `commit_memory`：写 Redis 和 DB。
- `orchestrate`：基于 Memory 和触发策略决定后续路径。
- `build_context`：只取必要上下文，避免把完整 transcript 塞给 LLM。
- `generate_patch`：调用 Summary Patch Writer。
- `quality_guard`：校验和可修复清洗。
- `apply_patch`：由代码应用 patch 到 projection，不让 LLM 直接控制最终树。
- `persist_snapshot`：保存 stage summary、episode 和 patch 审计。
- `publish`：发布 transcript/event/summary 事件。

失败策略：

- ASR 和转写失败：维持现有降级。
- event 抽取失败：保存 raw transcript，跳过 event。
- patch 失败：只记录 guard reject，不影响已有 projection。
- DB 写 episode 失败：Redis projection 可以展示，但记录告警；后续补偿。

## 7. Patch Apply Rules

系统负责应用 patch：

- section id 由系统生成，不由 LLM 自由决定。
- `append_section` 最多保留最近 12 个 section，老 section 可归档到 episode。
- `update_section` 只能更新 target section 的 summary、endSegment、items。
- `append_item` 需要去重，相似 item 不重复追加。
- `merge_section` 只能合并相邻或高度相似 section。
- `mark_question` 和 `mark_homework` 只更新对应列表，不影响纪要树。
- 所有 patch 都写入 `ai_lesson_summary_patch` 或至少写入 stage summary JSON 审计。

## 8. Compatibility

前端继续读取：

- `latestStageSummary`
- `minutesOutline`
- `studentQuestions`
- `homeworkCandidates`
- `keyPoints`
- `activeSectionTitle`

新增字段如果透传：

- `currentStageType`
- `lastTriggerReasons`
- `summaryVersion`
- `guardStatus`

live-class-service 可先无需改动；如果 Java DTO 限制字段，需要只通过 `rawState` 透传增强字段。

## 9. Rollout Plan

### Phase 1: Internal Memory + Patch Framework

- 新增 turn/event/episode/patch 数据结构。
- 保留旧 `summarize_stage` 作为 fallback。
- 实现 patch schema、apply、guard 单测。

### Phase 2: Semantic Trigger + Orchestrator

- 引入 Orchestrator。
- 语义触发与固定触发并行。
- 默认仍保守触发，避免成本暴涨。

### Phase 3: Multi-Agent Prompt Split

- Summary Patch Writer 替换旧大 Prompt。
- Teaching Event Extractor 可先规则为主，再加小模型。
- Topic Tracker 独立输出 stage decision。

### Phase 4: Report Reuse + Evaluation

- 课后报告读取 episode/student learning state。
- 建立离线 eval：同一段课堂 transcript 对比旧链路和新链路。
- 观察指标：首段纪要延迟、patch reject rate、重复率、人工可读性、LLM 调用成本。

## 10. Metrics

必须记录：

- `realtime_agent_turns_total`
- `realtime_agent_events_total`
- `realtime_agent_trigger_total{reason}`
- `realtime_agent_patch_total{type,status}`
- `realtime_agent_guard_reject_total{reason}`
- `realtime_agent_llm_calls_total{agent,model_tier}`
- `realtime_agent_first_summary_latency_seconds`
- `realtime_agent_context_tokens_estimated{agent}`

这些指标用于判断优化是否真的提升实时性和稳定性，而不是只增加复杂度。
