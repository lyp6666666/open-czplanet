# ai-agent-service 概览

## 当前定位

- 服务名：`ai-agent-service`
- 类型：内部 Python AI Agent 微服务
- 调用方：Java 微服务
- 当前网关路径：`/internal/ai/**`
- 当前默认端口：`18086`

## 当前已实现能力

### 1. 课后报告生成

- 输入：
  - lessonId
  - 教师课后填写内容
- 输出：
  - 结构化课后报告草稿

### 2. IM 沟通摘要

- 输入：
  - roomId
  - 消息列表
- 输出：
  - 结构化会话摘要

### 3. 实时课堂 AI（P1/P2 骨架）

- 输入：
  - lessonId
  - 课堂会话信息
  - transcript segments
- 输出：
  - 实时课堂状态
  - 阶段性课堂摘要
  - 课堂事件流

## 当前技术选型

- Web：FastAPI
- 队列：Redis + RQ
- 存储：MySQL / SQLite（本地）
- LLM：Provider 抽象，支持火山方舟 EP
- Realtime workflow：LangGraph
- LLM orchestration：LangChain
- ASR：腾讯云实时语音识别 SDK 适配

## 当前未实现

- 向量检索
- 真正的 LiveKit 音频桥接
- 多 Agent 编排

## 演进原则

- 先做稳定的内部任务服务，再做智能体编排。
- 后续 LangChain / LangGraph 应在现有 pipeline / task 分层之上接入，不要推翻第一版基础结构。
