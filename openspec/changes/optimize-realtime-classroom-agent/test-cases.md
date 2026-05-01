# Test Cases: Realtime Classroom Agent Optimization

## 1. 测试目标

这些测试用例用于验证实时课堂 Agent 优化后的行为是否符合预期，重点覆盖：

1. 单一大 Prompt 已被拆为可独立验证的专职任务。
2. 语义触发和原固定触发能协同工作。
3. LLM 只能输出增量 patch，不能重写完整 `minutesOutline`。
4. 多 Agent 通过 Orchestrator 共享同一 Memory。
5. Quality Guard 能阻止无证据、重复、非法或角色归因错误的内容污染实时总结。
6. 新链路保持现有 `ai/state` 前端契约兼容，失败时可回退。

测试实现建议：

- Python 单测优先放在 `ai-agent-service/tests/` 下。
- 使用 `AI_AGENT_REDIS_URL=memory://local`、`AI_AGENT_LLM_PROVIDER=template/mock` 保证确定性。
- 对 LLM 输出通过 fake client 固定返回，避免真实模型不稳定。
- 端到端 API 测试继续使用 FastAPI `TestClient`。

## 2. Fixture 设计

### 2.1 课堂 transcript fixture: `math_function_lesson`

用于验证正常课堂阶段流转。

```json
[
  {"seq": 1, "speaker": "teacher", "text": "我们先回顾上节课的一次函数。"},
  {"seq": 2, "speaker": "teacher", "text": "一次函数的一般形式是 y 等于 kx 加 b。"},
  {"seq": 3, "speaker": "teacher", "text": "这里的重点是 k 决定图像的倾斜程度。"},
  {"seq": 4, "speaker": "student", "text": "为什么 k 越大图像越陡？"},
  {"seq": 5, "speaker": "teacher", "text": "因为横坐标变化相同时，纵坐标变化越大，直线看起来就越陡。"},
  {"seq": 6, "speaker": "teacher", "text": "接下来我们看一个例题。"},
  {"seq": 7, "speaker": "teacher", "text": "已知直线经过两个点，先求 k，再求 b。"},
  {"seq": 8, "speaker": "student", "text": "我这里不会代入第二个点。"},
  {"seq": 9, "speaker": "teacher", "text": "注意先把第一个点代入求出关系式，再用第二个点校验。"},
  {"seq": 10, "speaker": "teacher", "text": "课后作业是完成讲义第十二到十四页，并整理今天这道错题。"}
]
```

期望高层结果：

- 至少形成两个教学阶段：一次函数概念/图像理解、例题求解步骤。
- 学生问题包含 “为什么 k 越大图像越陡？”。
- 作业候选包含讲义 12-14 页和整理错题。
- `minutesOutline` 不应被整体重写丢失旧阶段。

### 2.2 低价值 transcript fixture: `classroom_management_only`

```json
[
  {"seq": 1, "speaker": "teacher", "text": "能听到吗？"},
  {"seq": 2, "speaker": "student", "text": "可以。"},
  {"seq": 3, "speaker": "teacher", "text": "你把摄像头打开一下。"},
  {"seq": 4, "speaker": "student", "text": "稍等。"}
]
```

期望高层结果：

- 可以保存 transcript 和 turn。
- 可以抽取 `classroom_management` event。
- Orchestrator 不应调用强模型生成正式总结。
- `minutesOutline` 不应新增“知识点”类 section。

### 2.3 Guard 失败 fixture: `bad_patch_outputs`

用于 fake LLM 返回异常 patch：

- 缺少 evidence。
- 重复 item。
- 包含 “ASR/LLM/模型/置信度” 技术词。
- 把 teacher 解释误标为 student question。
- 试图删除或重写全部 `minutesOutline`。

## 3. 单元测试用例

### TC-U01: Turn Aggregator 合并同一 speaker 连续讲解

前置：

- 当前 open turn 为空。
- 输入 seq 1-3 均为 teacher，时间间隔短，内容为一次函数讲解。

步骤：

1. 依次提交 seq 1、2、3。
2. 调用 Turn Aggregator。

期望：

- 生成一个 teacher turn。
- `startSegment=1`，`endSegment=3`。
- turn text 包含三条讲解内容。
- `roleHint=explanation` 或 `unknown`，但不能是 `question`。

### TC-U02: Turn Aggregator 不把学生短问句合并进老师 turn

前置：

- open turn 为 teacher，`endSegment=3`。

输入：

```json
{"seq": 4, "speaker": "student", "text": "为什么 k 越大图像越陡？"}
```

期望：

- teacher open turn 被关闭。
- 新增 student turn。
- student turn `roleHint=question`。
- student turn text 只包含学生问题，不包含老师讲解。

### TC-U03: Teaching Event Extractor 抽取学生问题并带 evidence

输入：

```json
{
  "turnId": "turn-2",
  "speaker": "student",
  "startSegment": 4,
  "endSegment": 4,
  "text": "为什么 k 越大图像越陡？",
  "roleHint": "question"
}
```

期望：

- 输出一个 `student_question` event。
- `topic` 可为 “一次函数” 或空，但 event text 必须保留问题原意。
- `evidence.turnIds` 包含 `turn-2`。
- `evidence.segmentRange=[4,4]`。
- confidence 大于配置阈值。

### TC-U04: Teaching Event Extractor 抽取老师重点

输入：

```json
{
  "turnId": "turn-1",
  "speaker": "teacher",
  "startSegment": 1,
  "endSegment": 3,
  "text": "这里的重点是 k 决定图像的倾斜程度。",
  "roleHint": "explanation"
}
```

期望：

- 输出 `teacher_emphasis` 或 `concept_explained` event。
- 不能输出 `student_question`。
- evidence 引用 `turn-1`。

### TC-U05: Teaching Event Extractor 抽取作业

输入：

```json
{
  "turnId": "turn-7",
  "speaker": "teacher",
  "startSegment": 10,
  "endSegment": 10,
  "text": "课后作业是完成讲义第十二到十四页，并整理今天这道错题。",
  "roleHint": "instruction"
}
```

期望：

- 输出 `homework_assigned` event。
- event text 包含“讲义第十二到十四页”和“整理错题”。
- evidence 存在。

### TC-U06: Topic & Stage Tracker 判断主题切换

前置 Memory：

- 当前 stageType 为 `teach`。
- currentTopic 为 “一次函数图像”。
- 最近 event 包含 `stage_transition`，文本 “接下来我们看一个例题”。

期望：

- `stageDecision=transition` 或 `close_current`。
- `stageType=practice` 或 `teach`，但 reason 必须说明进入例题。
- confidence 大于语义触发阈值。

### TC-U07: Topic & Stage Tracker 对同主题继续讲解不切换

前置 Memory：

- 当前 topic 为 “一次函数图像”。
- 最近 event 都是 `concept_explained`，没有阶段切换词。

期望：

- `stageDecision=continue`。
- 不产生 `topic_transition` trigger reason。

## 4. Orchestrator 触发测试

### TC-O01: 首段快速触发

前置：

- lesson 新建。
- `minutesOutline=[]`。
- `lastPatchId` 为空。
- 已累计 4 个有效 turn。
- 未到固定 300 秒窗口。

期望：

- Orchestrator 输出 `decision=generate_patch`。
- `triggerReasons` 包含 `first_summary`。
- `modelTier=small` 或 `strong`，由配置决定。
- `contextPlan` 包含 recentTurns，但不包含整节课 transcript。

### TC-O02: 固定触发兜底

前置：

- `lastLlmSummaryTs=now-301`。
- `lastLlmSegmentCount=2`。
- `segmentCount=10`。
- 最近没有明显语义事件。

期望：

- Orchestrator 输出允许生成总结。
- `triggerReasons` 包含 `fixed_interval`。
- 如果信息密度不足，可输出 `noop` patch，但必须记录触发审计。

### TC-O03: 主题切换语义触发优先于固定窗口

前置：

- `lastLlmSummaryTs=now-60`，未到固定窗口。
- 最近 event 包含 `stage_transition`，Topic Tracker 输出 `transition`。
- 不在强模型冷却窗口内。

期望：

- Orchestrator 输出 `generate_patch`。
- `triggerReasons` 包含 `topic_transition` 或 `stage_transition`。
- 不要求满足固定 300 秒。

### TC-O04: 低价值课堂管理不触发强模型

输入 fixture：`classroom_management_only`。

期望：

- Orchestrator 输出 `extract_only` 或 `skip`。
- `modelTier=none`。
- LLM fake client 调用次数为 0。
- `minutesOutline` 不新增 section。

### TC-O05: 冷却窗口抑制高频触发

前置：

- 最近 30 秒内已经成功生成 strong model patch。
- 又出现一个 `teacher_emphasis` event。
- strong model cooldown 配置为 90 秒。

期望：

- Orchestrator 不调用 strong model。
- 可选择 `extract_only` 或延迟触发。
- state 中记录 `suppressedTriggerReasons` 包含 `teacher_emphasis`。

## 5. Patch Apply 测试

### TC-P01: append_section 只追加新 section

前置 projection：

```json
{
  "minutesOutline": [
    {"id": "section-1", "title": "一次函数回顾", "summary": "回顾一次函数形式。", "items": []}
  ]
}
```

patch：

```json
{
  "patchType": "append_section",
  "section": {
    "title": "例题求解步骤",
    "summary": "开始讲解通过两点求一次函数表达式。",
    "items": [
      {"title": "先求 k", "detail": "通过两个点的坐标变化求出斜率。", "evidenceEventIds": ["evt-6"]}
    ]
  }
}
```

期望：

- `minutesOutline` 变为 2 个 section。
- 原 `section-1` 原样保留。
- 新 section id 由系统生成，例如 `section-2`。
- `activeSectionTitle=例题求解步骤`。

### TC-P02: update_section 不允许删除历史 section

前置 projection 有 `section-1`、`section-2`。

patch：

```json
{
  "patchType": "update_section",
  "targetSectionId": "section-2",
  "summary": "补充例题代入步骤。",
  "appendItems": [
    {"title": "代入第二个点", "detail": "用第二个点校验并求出 b。", "evidenceEventIds": ["evt-8"]}
  ]
}
```

期望：

- `section-1` 不变。
- `section-2.summary` 更新。
- `section-2.items` 追加一项。
- section 总数仍为 2。

### TC-P03: mark_question 只更新问题列表

patch：

```json
{
  "patchType": "mark_question",
  "studentQuestions": [
    {"text": "为什么 k 越大图像越陡？", "evidenceEventIds": ["evt-4"]}
  ]
}
```

期望：

- `studentQuestions` 新增该问题。
- `minutesOutline` 不变化。
- `latestStageSummary` 不变化。

### TC-P04: noop 不改变 projection

前置 projection 任意。

patch：

```json
{"patchType": "noop", "reason": "信息密度不足"}
```

期望：

- projection 深比较完全一致。
- state 中可更新 `lastTriggerReasons` 或 patch 审计。

### TC-P05: 相似 item 去重

前置：

- 当前 section 已有 item：`{"title": "斜率影响倾斜", "detail": "k 值决定直线倾斜程度。"}`

patch 追加：

```json
{"title": "k 决定倾斜", "detail": "k 值会决定一次函数图像的倾斜程度。"}
```

期望：

- 不重复追加，或合并为一条更完整 item。
- guard 或 apply 结果记录 duplicate reason。

## 6. Quality Guard 测试

### TC-G01: 缺少 evidence 的 patch 被拒绝

patch：

```json
{
  "patchType": "append_item",
  "targetSectionId": "section-1",
  "appendItems": [
    {"title": "斜率重点", "detail": "k 决定图像倾斜。"}
  ]
}
```

期望：

- Guard 输出 `accepted=false` 或移除该 item。
- projection 不被污染。
- reject reason 包含 `missing_evidence`。

### TC-G02: 引用不存在 event 的 patch 被拒绝

patch 引用 `evidenceEventIds=["evt-not-found"]`。

期望：

- Guard 拒绝。
- reject reason 包含 `invalid_evidence_reference`。

### TC-G03: 包含技术词的展示字段被修复或拒绝

patch：

```json
{
  "patchType": "append_section",
  "section": {
    "title": "ASR识别阶段",
    "summary": "LLM 根据转写置信度总结课堂。",
    "items": []
  }
}
```

期望：

- Guard 修复为非技术表达，或直接拒绝。
- 最终 `minutesOutline` 不出现 “ASR”“LLM”“模型”“置信度”。

### TC-G04: 老师解释不能被标成学生问题

Memory evidence：

- `evt-5` speaker 为 teacher，type 为 `concept_explained`。

patch：

```json
{
  "patchType": "mark_question",
  "studentQuestions": [
    {"text": "横坐标变化相同时纵坐标变化越大", "evidenceEventIds": ["evt-5"]}
  ]
}
```

期望：

- Guard 拒绝该 studentQuestions 字段。
- reject reason 包含 `speaker_role_mismatch`。

### TC-G05: patch 试图重写整棵树被拒绝

patch：

```json
{
  "patchType": "replace_outline",
  "minutesOutline": []
}
```

期望：

- schema 校验失败。
- projection 保持不变。
- reject reason 包含 `unsupported_patch_type`。

## 7. Context Builder 测试

### TC-C01: 构造上下文不包含整节 transcript

前置：

- Memory 中有 200 条 transcript。
- 有最近 12 个 turns、20 个 events、当前 episode、projection。

步骤：

1. Orchestrator 输出 `contextPlan.recentTurns=12`、`recentEvents=20`。
2. 调用 Context Builder。

期望：

- 上下文中 turns 数量不超过 12。
- events 数量不超过 20。
- 包含当前 episode 和 projection。
- 不包含 200 条完整 transcript。

### TC-C02: 当前 episode 缺失时仍可构造上下文

前置：

- 没有 current episode。
- 有 recent turns/events/projection。

期望：

- Context Builder 成功。
- context 中 `currentEpisode=null`。
- Orchestrator 可选择 `append_section` 而不是失败。

## 8. Graph 编排集成测试

### TC-I01: 一条 transcript 进入后完整写入 Memory

步骤：

1. 创建 realtime session。
2. 提交一条 teacher transcript segment：“这里的重点是 k 决定图像倾斜程度。”

期望：

- API 返回 200。
- raw transcript 被保存。
- Redis transcript list 增加。
- 至少产生一个 turn。
- 至少产生一个 teaching event。
- `ai/state.rawState.memoryVersion` 增加。

### TC-I02: 正常课堂 fixture 生成 patch 并保持前端契约

步骤：

1. 创建 realtime session。
2. 按顺序提交 `math_function_lesson` fixture。
3. fake LLM 在触发时返回合法 append/update patch。
4. 查询 state。

期望：

- `state.minutesOutline` 非空。
- 至少一个 section title 与一次函数或例题相关。
- `state.studentQuestions` 包含学生问题。
- `state.homeworkCandidates` 包含作业。
- `state.activeSectionTitle` 不为空。
- `rawState.lastPatchId` 不为空。
- `rawState.lastTriggerReasons` 包含语义或固定触发原因。

### TC-I03: Guard 拒绝 patch 后不污染 state

前置：

- 已有一个合法 `minutesOutline`。
- fake LLM 返回缺少 evidence 的 patch。

步骤：

1. 提交能触发总结的 transcript。
2. 查询 state。

期望：

- API 返回 200。
- 旧 `minutesOutline` 与触发前一致。
- `rawState.guardRejectedCount` 增加。
- `rawState.lastGuardReasons` 包含 `missing_evidence`。

### TC-I04: 新 Agent feature flag 关闭时回退旧链路

环境：

- `AI_AGENT_REALTIME_AGENT_ENABLED=false`。

步骤：

1. 创建 session。
2. 提交满足旧固定触发条件的 transcript。

期望：

- 调用旧 `summarize_stage`。
- API 返回结构与现有测试兼容。
- 不要求 turn/event/patch 字段存在。

### TC-I05: Agent 节点异常不影响 transcript 保存

前置：

- fake Teaching Event Extractor 抛异常。

步骤：

1. 提交 transcript segment。

期望：

- API 返回 200，或至少不因 event extractor 异常导致课堂主链路失败。
- raw transcript 已保存。
- state 中记录 agent degraded/error。
- `minutesOutline` 不被清空。

## 9. API 回归测试

### TC-A01: `/sessions` 创建后初始化新 Memory 字段

步骤：

1. 创建 realtime session。
2. 查询 state。

期望：

- `segmentCount=0`。
- `minutesOutline=[]`。
- `rawState.memoryVersion` 初始存在或默认为 0。
- `rawState.summaryVersion` 初始存在或默认为 0。
- 现有字段 `asrEnabled`、`llmEnabled`、`status` 仍正确。

### TC-A02: `/transcript-segments` 保持兼容

步骤：

1. 创建 session。
2. 提交 transcript segment。

期望：

- 返回模型仍满足 `RealtimeLessonStateView`。
- 现有字段 `lessonId/mode/asrEnabled/llmEnabled/segmentCount/status/rawState` 存在。
- 新增内部字段不破坏 Pydantic response。

### TC-A03: `/audio-chunks` 静音仍不触发 ASR 与 Agent 总结

步骤：

1. 创建 session。
2. 提交 RMS 低于阈值的 audio chunk。

期望：

- `segmentCount=0`。
- 不产生 turn/event/patch。
- `asrListening=false` 或符合静音状态逻辑。

### TC-A04: `/finalize` 保留最终状态并可复用 Memory

步骤：

1. 创建 session。
2. 提交若干 transcript 生成 episode。
3. 调用 finalize。

期望：

- state status 为 `FINALIZED`。
- episode memory 不被删除。
- 课后报告链路可读取 episode；如果未实现课后复用，则必须回退旧报告输入。

## 10. 课后报告复用测试

### TC-R01: 有 episode memory 时优先作为课后报告输入

前置：

- lesson 有两个 episode：
  - 一次函数图像理解
  - 例题求解步骤
- student learning state 包含疑问点和作业。

步骤：

1. 触发 lesson report task。
2. fake report pipeline 捕获输入。

期望：

- 输入包含 episode summary。
- 输入包含 student questions/misconceptions/homework。
- 不只依赖 teacherNotes 的固定模板。

### TC-R02: episode 缺失时回退旧报告链路

前置：

- 没有 episode memory。

步骤：

1. 触发 lesson report task。

期望：

- report task 成功。
- 输出仍满足现有 `LessonReportView`。
- 不因新 Memory 缺失失败。

## 11. 离线效果评测用例

### TC-E01: 首段纪要延迟改善

输入：

- `math_function_lesson` 前 4 个 turn。

旧链路期望：

- 未到 300 秒或 8 段时通常不生成正式纪要。

新链路期望：

- 首段快速触发生成一个轻量 section 或 summary。
- `first_summary_latency_seconds` 小于配置目标，例如 60 秒或 5 个有效 turn 内。

### TC-E02: 历史 section 不丢失

步骤：

1. 先生成 section-1。
2. 后续触发例题阶段 patch。

期望：

- section-1 仍存在。
- 新 patch 只追加或更新目标 section。
- 没有全量替换导致 section-1 丢失。

### TC-E03: 重复率降低

输入：

- 连续多段同义重点：“k 决定倾斜”“斜率决定倾斜程度”“k 越大越陡”。

期望：

- `minutesOutline` 不出现三条高度重复 item。
- duplicate guard 计数大于 0 或合并记录存在。

### TC-E04: 总结内容有证据

步骤：

1. 用 fixture 生成所有 section/items/questions/homework。
2. 遍历最终 projection 和 patch 审计。

期望：

- 每个新增 item/question/homework/keyPoint 至少有一个 evidenceEventId。
- evidenceEventId 能在 Memory 中查到。

## 12. 可观测性测试

### TC-M01: 触发指标记录

步骤：

1. 构造一次 `first_summary` 触发。
2. 构造一次 `topic_transition` 触发。

期望：

- metrics 包含 `realtime_agent_trigger_total{reason="first_summary"}`。
- metrics 包含 `realtime_agent_trigger_total{reason="topic_transition"}`。

### TC-M02: Guard 拒绝指标记录

步骤：

1. fake LLM 返回缺少 evidence 的 patch。

期望：

- metrics 包含 `realtime_agent_guard_reject_total{reason="missing_evidence"}`。
- 日志 trace 包含 lessonId、patchId、guardStatus。

### TC-M03: LLM 调用分层指标记录

步骤：

1. 构造低价值内容。
2. 构造首段快速触发。
3. 构造主题切换触发。

期望：

- 低价值内容 LLM 调用次数为 0。
- 首段快速触发记录 small 或 configured tier。
- 主题切换记录 strong 或 configured tier。

## 13. 验收门槛

实现完成后至少需要通过：

- 所有 Turn/Event/Orchestrator/Patch/Guard 单测。
- 现有 `ai-agent-service/tests/test_api.py` 全部通过。
- 新增 realtime agent API 集成测试全部通过。
- feature flag 关闭时，新链路相关字段缺失也不影响旧接口。
- fake LLM 输出恶意或错误 patch 时，`minutesOutline` 不被污染。
- `math_function_lesson` fixture 可以在未到固定 300 秒时生成首段有效纪要。
- `classroom_management_only` fixture 不调用强模型生成正式课堂知识总结。
