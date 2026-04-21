# 实时课堂 AI 后端实现设计

## 1. 文档信息

- 文档名称：实时课堂 AI 后端实现设计
- 核心服务：`ai-agent-service`
- 相关服务：
  - `live-class-service`
  - `videoCall-IM-service`
  - `ai-tutor-gateway`
  - `ai-tutor-common`
- 日期：2026-04-20

---

## 2. 设计目标

本设计目标是实现一个：

- 低成本
- 可降级
- 可逐步扩展
- 可演进到 LangChain / LangGraph

的实时课堂 AI 后端架构。

---

## 3. 总体架构

```text
课堂前端
  -> live-class-service
  -> LiveKit
  -> ai-agent-service
      -> ASR Provider
      -> Redis
      -> MySQL
      -> LLM Provider
```

职责拆分：

### `live-class-service`

负责：

- 课程开始/结束事件
- 房间与课堂生命周期
- 音视频会话管理
- 课程元信息

### `ai-agent-service`

负责：

- 实时字幕处理
- 课堂事件提取
- 阶段性摘要
- 课后总结
- 课后报告

---

## 4. 最省钱实现原则

### 4.1 不触发原则

以下情况不触发 ASR：

- 没有音频流
- 音频流不可用
- 当前课型不需要音频 AI
- 课堂被配置为“仅课后 AI”

以下情况不触发 LLM：

- 没有新增 transcript
- 新增内容不足以生成新阶段摘要
- 当前处于冷却窗口内

### 4.2 调用优先级

```text
规则引擎 > Redis 状态机 > LLM
```

说明：

- 高频提取靠规则
- 状态维护靠 Redis
- 语言组织和归纳才交给 LLM

---

## 5. 微服务改造方案

### 5.1 `ai-agent-service`

新增实时课堂模块：

```text
app/realtime/
  session_manager.py
  transcript_buffer.py
  rule_engine.py
  insight_scheduler.py
  ws_push.py
```

建议新增能力：

1. Realtime Session 管理
2. Transcript Segment 处理
3. Rule Engine
4. LLM Insight Scheduler
5. Lesson Finalizer

### 5.2 `live-class-service`

建议改造点：

1. 课程开始时调用 `ai-agent-service` 创建课堂 AI Session
2. 课程结束时调用 `ai-agent-service` 触发 finalize
3. 提供课程元信息：
   - lessonId
   - teacherId
   - studentId
   - subject
   - grade
   - classMode

### 5.3 `ai-tutor-gateway`

已有内部路由：

- `/internal/ai/**`

后续保持内部调用即可，不对外暴露。

### 5.4 `videoCall-IM-service`

与课堂内实时 AI 无强依赖，但可在课后：

- 写入课堂纪要消息卡片
- 写入教师复盘链接
- 写入家长报告入口

---

## 6. 数据流设计

### 6.1 课堂开始

```text
live-class-service
  -> POST /internal/ai/live-lessons/{lessonId}/sessions
```

请求：

```json
{
  "teacherId": 1001,
  "studentId": 2001,
  "subject": "数学",
  "grade": "初二",
  "courseType": "ONLINE_FORMAL",
  "audioEnabled": true,
  "realtimeAiMode": "LIGHT"
}
```

返回：

```json
{
  "sessionId": "lesson_ai_xxx",
  "asrEnabled": true,
  "llmEnabled": true,
  "mode": "LIGHT"
}
```

### 6.2 实时 transcript

ASR 输出：

```json
{
  "lessonId": 123,
  "seq": 1,
  "speaker": "teacher",
  "startMs": 1000,
  "endMs": 3200,
  "text": "我们先看一次函数图像"
}
```

进入：

- Redis transcript buffer
- Rule Engine

### 6.3 规则引擎输出

```json
{
  "currentTopic": "一次函数图像",
  "questionCandidates": ["为什么 k 越大图像越陡"],
  "homeworkCandidates": [],
  "keyPoints": ["斜率决定倾斜程度"]
}
```

### 6.4 阶段性摘要

每 3~5 分钟触发：

```text
transcript chunk + 当前规则状态
  -> LLM
  -> stage insight
```

输出：

```json
{
  "stageSummary": "本阶段重点讲解了一次函数斜率与图像变化关系。",
  "currentTopic": "一次函数图像",
  "studentQuestions": ["为什么 k 越大图像越陡"],
  "attentionPoint": "学生对图像变化趋势理解较好，但对抽象表达仍需要巩固。"
}
```

### 6.5 课程结束

```text
live-class-service
  -> POST /internal/ai/live-lessons/{lessonId}/finalize
```

动作：

1. flush transcript
2. 生成完整课后总结
3. 生成家长版报告草稿
4. 生成教师复盘版

---

## 7. Redis 设计

### 7.1 Transcript Buffer

```text
ai:lesson:{lessonId}:transcript
```

类型：

- List

策略：

- 只保留最近 200~500 条 segment

### 7.2 Lesson State

```text
ai:lesson:{lessonId}:state
```

类型：

- Hash / JSON

内容：

```json
{
  "lessonId": 123,
  "mode": "LIGHT",
  "audioEnabled": true,
  "asrEnabled": true,
  "llmEnabled": true,
  "currentTopic": "一次函数图像",
  "latestStageSummary": "本阶段重点讲解...",
  "studentQuestions": ["为什么k越大越陡"],
  "homeworkCandidates": ["完成讲义P12-P14"],
  "lastLlmSummaryAt": "2026-04-20T21:00:00+08:00"
}
```

### 7.3 LLM 冷却控制

```text
ai:lesson:{lessonId}:llm:cooldown
```

作用：

- 控制 3~5 分钟内不重复调 LLM

### 7.4 Finalize 标记

```text
ai:lesson:{lessonId}:finalized
```

作用：

- 防止重复课后总结

---

## 8. MySQL 设计

建议新增表：

### `ai_lesson_transcript`

用途：

- 存课堂最终 transcript

### `ai_lesson_stage_summary`

用途：

- 存阶段性课堂总结

### `ai_lesson_report`

已存在，用于课后报告

### `ai_task`

已存在，用于任务状态

---

## 9. API 设计

### 9.1 创建课堂 AI Session

```http
POST /internal/ai/live-lessons/{lessonId}/sessions
```

### 9.2 推送 transcript segment

```http
POST /internal/ai/live-lessons/{lessonId}/transcript-segments
```

说明：

- 如果实时 ASR 在 `ai-agent-service` 内完成，这个接口也可以转为内部调用
- 如果实时 ASR 在外部网关或适配器完成，可通过此接口落库和驱动规则引擎

### 9.3 查询课堂实时状态

```http
GET /internal/ai/live-lessons/{lessonId}/state
```

### 9.4 课堂结束 finalize

```http
POST /internal/ai/live-lessons/{lessonId}/finalize
```

### 9.5 实时推送接口

建议：

```http
WS /internal/ai/live-lessons/{lessonId}/stream
```

事件：

- `transcript.segment`
- `lesson.insight`
- `lesson.status`

---

## 10. 调度与省钱策略实现

### 10.1 ASR 启动条件

满足以下条件才启动：

1. 课程类型允许音频 AI
2. 音频流实际存在
3. 当前租户/环境已开启 ASR

否则：

- `asrEnabled=false`
- 降级为文字课堂 AI

### 10.2 LLM 启动条件

满足以下条件才调用：

1. 最近窗口内新增 transcript 达到阈值
2. 不在冷却窗口中
3. 当前课堂 AI 模式允许

否则：

- 跳过 LLM
- 继续沿用上一版阶段摘要

### 10.3 课型差异化

建议配置：

- `ONLINE_FORMAL`
  - ASR + 规则 + 阶段摘要 + finalize
- `ONLINE_TRIAL`
  - ASR + finalize
- `TEXT_ONLY`
  - 无 ASR，仅文字摘要
- `LOW_COST`
  - ASR + 规则，无课堂中 LLM，只做课后总结

---

## 11. 前端对接建议

### 11.1 老师端

第一阶段完整展示：

- AI 课堂纪要浮层
- 实时字幕
- 阶段摘要
- 学生提问
- 作业候选

### 11.2 学生端

第一阶段轻量展示：

- 实时字幕
- 不展示复杂纪要面板

### 11.3 家长端

不参与课堂中实时面板；
课后查看：

- 家长版课后报告

---

## 12. 技术演进到 LangChain / LangGraph

### 当前阶段

不直接引入 LangChain / LangGraph 到主链路。

原因：

- 实时课堂要求低延迟
- 第一阶段优先稳定和低成本

### 后续接入点

未来可接在：

1. `pipelines/`
2. `tasks/`
3. `lesson finalize`
4. `teacher review assistant`
5. `student profile update`

建议演进：

- P1：当前原生 pipeline
- P2：LangChain 工具链与 prompt 编排
- P3：LangGraph 多 Agent 状态机

---

## 13. 风险与降级

### 风险 1：ASR 不稳定

降级：

- 无字幕
- 课后靠老师输入补报告

### 风险 2：LLM 失败

降级：

- 保留规则引擎结果
- 面板显示“阶段摘要暂未更新”

### 风险 3：Redis 压力

措施：

- transcript 只保留最近窗口
- 长期结果落 MySQL

---

## 14. 实施建议

### 第一阶段

1. `ai-agent-service` 增加 realtime 模块
2. `live-class-service` 增加 session/create/finalize 集成
3. 老师端课堂页增加 AI 面板
4. 使用低成本模式：
   - 有音频才开 ASR
   - 每 5 分钟一次 LLM

### 第二阶段

1. transcript 持久化
2. 教师复盘版
3. 家长回顾增强

### 第三阶段

1. LangChain / LangGraph
2. 多 Agent 协作
3. 学生画像沉淀

