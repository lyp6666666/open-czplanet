# 课堂 AI 后端接口与状态流转文档

## 1. 文档目的

本文件用于明确课堂 AI 的后端分层、接口职责、状态流转、消息投递与失败补偿策略。

目标：

1. `live-class-service` 成为课堂 AI 的聚合入口
2. `ai-agent-service` 负责 AI 计算与数据沉淀
3. `videoCall-IM-service` 负责课后结果消息卡片触达

---

## 2. 服务职责

### 2.1 live-class-service

负责：

- 课程生命周期
- 课堂 session 生命周期
- 课前 prepare / 课中 status / 课后 timeline
- 聚合课堂 AI 当前状态与课后结果状态
- 调用 `ai-agent-service`

### 2.2 ai-agent-service

负责：

- 创建实时课堂 AI session
- 接收 transcript segment
- 输出实时课堂状态
- finalize 后生成：
  - 课后总结
  - 课后报告草稿

### 2.3 videoCall-IM-service

负责：

- 向聊天房间投递课后 AI 结果系统消息

---

## 3. 主链路时序

## 3.1 课前

```text
前端 -> live-class-service /prepare
live-class-service -> 返回课堂准备信息
前端进入课堂后
前端 -> live-class-service /status
live-class-service -> ai-agent-service 创建/查询 AI session
```

## 3.2 课中

```text
音频 -> ASR -> ai-agent-service
ai-agent-service -> Redis state + MySQL transcript/stage_summary
前端 -> live-class-service 查询课堂 AI 聚合状态
live-class-service -> ai-agent-service 查询当前 AI state
```

## 3.3 课后

```text
前端/系统 -> live-class-service /end
live-class-service -> ai-agent-service /finalize
ai-agent-service -> 生成课后总结 + 报告草稿
live-class-service -> videoCall-IM-service 发系统消息
前端 -> live-class-service 查询课后结果
```

---

## 4. 建议新增接口

## 4.1 live-class-service 对前端新增

### 查询课堂 AI 当前状态

`GET /live/sessions/{sessionId}/ai/state`

响应：

```json
{
  "sessionId": 1001,
  "courseId": 2001,
  "aiStatus": "ACTIVE",
  "realtimeEnabled": true,
  "summaryStatus": "ACTIVE",
  "currentTopic": "一次函数图像",
  "latestStageSummary": "本阶段重点讲解了一次函数斜率与图像变化关系。",
  "studentQuestions": ["为什么 k 越大图像越陡"],
  "homeworkCandidates": ["完成讲义 P12-P14"],
  "keyPoints": ["斜率决定倾斜程度"],
  "updatedAt": "2026-04-21T21:10:00+08:00"
}
```

### 查询课后 AI 结果

`GET /live/sessions/{sessionId}/ai/result`

响应：

```json
{
  "sessionId": 1001,
  "courseId": 2001,
  "resultStatus": "READY",
  "reportStatus": "WAITING_TEACHER_REVIEW",
  "summary": {
    "currentTopic": "一次函数综合训练",
    "stageSummary": "本节课围绕一次函数图像与应用题展开，学生对基础概念理解较好，但综合题审题仍需训练。",
    "studentQuestions": ["为什么截距变化后图像整体平移"],
    "homeworkCandidates": ["完成讲义P12-P14", "整理错题1道"],
    "keyPoints": ["斜率与图像倾斜程度", "截距与图像平移"]
  },
  "report": {
    "reportTitle": "初二数学课后报告",
    "parentSummary": "本节课重点讲解一次函数图像与应用题...",
    "nextLessonPlan": "继续训练综合题"
  }
}
```

### 重新触发课后生成

`POST /live/sessions/{sessionId}/ai/result/retry`

用途：

- 课后结果失败后的人工补偿

---

## 4.2 live-class-service 对 ai-agent-service 新增/调用

### 创建课堂 AI session

`POST /internal/ai/live-lessons/{lessonId}/sessions`

已存在，继续复用。

### 查询课堂 AI state

`GET /internal/ai/live-lessons/{lessonId}/state`

已存在，继续复用。

### finalize

`POST /internal/ai/live-lessons/{lessonId}/finalize`

已存在，继续复用。

### 课后报告

`GET /internal/ai/lessons/{lessonId}/report`

已存在，继续复用。

---

## 4.3 videoCall-IM-service 系统消息协议

新增 bizType：

- `LESSON_AI_RESULT`

body 建议：

```json
{
  "bizType": "LESSON_AI_RESULT",
  "eventId": 3001,
  "title": "本节课 AI 总结已生成",
  "status": "READY",
  "contextType": "COURSE",
  "contextId": 2001,
  "content": "本节课围绕一次函数图像与应用题展开。",
  "reportStatus": "WAITING_TEACHER_REVIEW"
}
```

---

## 5. 状态流转

## 5.1 课堂 AI 实时状态

```text
OFF
  -> INITING
  -> ACTIVE
  -> ASR_DEGRADED
  -> LLM_DEGRADED
  -> FAILED
  -> FINALIZING
  -> FINALIZED
```

### 状态定义

- `OFF`
  - 本节课不启用课堂 AI
- `INITING`
  - AI session 创建中
- `ACTIVE`
  - 实时课堂 AI 正常
- `ASR_DEGRADED`
  - 语音识别异常，但课堂继续
- `LLM_DEGRADED`
  - 阶段摘要异常，但规则提取仍可用
- `FAILED`
  - 本节课 AI 不可用
- `FINALIZING`
  - 正在生成课后结果
- `FINALIZED`
  - 课后结果已产出

## 5.2 课后结果状态

```text
OFF
PENDING
FINALIZING
READY
FAILED
```

---

## 6. 数据聚合规则

## 6.1 实时状态聚合

`live-class-service` 从 `ai-agent-service` 获取 state 后，需映射为前端稳定契约：

原始 state 字段：

- `mode`
- `asrEnabled`
- `llmEnabled`
- `currentTopic`
- `latestStageSummary`
- `studentQuestions`
- `homeworkCandidates`
- `keyPoints`
- `status`

映射逻辑：

1. `status=ACTIVE` 且 `asrEnabled=true` -> `aiStatus=ACTIVE`
2. `status=ACTIVE` 且 state 中有异常标记 -> `ASR_DEGRADED/LLM_DEGRADED`
3. session 未初始化成功 -> `FAILED`
4. `aiPolicy=OFF` -> `OFF`

## 6.2 课后结果聚合

数据来源：

- `ai_lesson_stage_summary`
- `ai_lesson_report`

聚合原则：

1. 优先展示课后总结可读版
2. 报告草稿作为老师侧附加内容
3. 如果总结不存在但报告存在，仍返回 `resultStatus=READY`
4. 如果二者都失败，返回 `FAILED`

---

## 7. 课后消息卡片投递

## 7.1 触发时机

在 `live-class-service.end()` 成功后：

1. 调用 `ai-agent-service.finalize`
2. 查询课后结果状态
3. 向 IM 房间投递系统消息

## 7.2 投递策略

### 如果 finalize 返回可用结果

发送：

- `LESSON_AI_RESULT`
- `status=READY`

### 如果 finalize 仅进入处理中

发送：

- `LESSON_AI_RESULT`
- `status=FINALIZING`

### 如果 finalize 失败

发送：

- `LESSON_AI_RESULT`
- `status=FAILED`

---

## 8. 失败补偿

## 8.1 finalize 失败

处理：

1. 写入 `CLASS_AI_FINALIZE_FAILED` timeline 事件
2. 前端查询结果返回 `FAILED`
3. 支持手动 `retry`

## 8.2 IM 消息发送失败

处理：

1. 课堂结果已生成不受影响
2. 记录日志
3. 后续可通过定时补发或人工触发补发

## 8.3 ai-agent-service 不可达

处理：

1. 课堂主流程继续
2. `live-class-service` 返回稳定失败态
3. 不让前端直接感知内部 5xx 原始错误

---

## 9. 测试建议

## 9.1 live-class-service 单测

覆盖：

1. 查询课堂 AI 当前状态
2. 查询课后结果
3. end 后触发 finalize
4. finalize 失败降级

## 9.2 ai-tutor-web 页面测试

覆盖：

1. 课堂页显示 `AI 纪要`
2. 课程页显示课后结果入口
3. 聊天页渲染 `lesson_ai_result` 卡片
4. 课后总结页显示完整信息

## 9.3 ai-agent-service 测试

覆盖：

1. 实时课堂状态查询
2. finalize 输出结果
3. 报告查询

---

## 10. 一句话结论

后端的关键不是让每个服务都“懂 AI”，而是让：

- `ai-agent-service` 专注生成
- `live-class-service` 专注课堂聚合
- `videoCall-IM-service` 专注触达消息

这样前端只需要对接稳定、统一、可降级的课堂 AI 契约。
