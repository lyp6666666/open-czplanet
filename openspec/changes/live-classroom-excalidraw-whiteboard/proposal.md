# Proposal: Live Classroom Excalidraw Whiteboard

## Why

实时课堂已经有稳定的 LiveKit 音视频与 AI 实时纪要能力，但左侧工具轨里的“白板”仍只是入口。线上一对一课堂需要教师和学生在同一节课里共同板书、画图、批注题目，并且课后还能恢复这节课对应的白板内容。

本次目标是引入 Excalidraw 作为课堂白板内核，实现“一节课绑定一个白板”，并保证切换到白板后音视频不断线、人物画面不消失、现有实时课堂能力不回退。

## What Changes

- 在实时课堂页增加 `video` / `whiteboard` 两种工作模式。
- 点击“白板”后，中央舞台从视频优先切换为白板优先，人物视频改为悬浮小窗/侧边停靠窗。
- 使用 `@excalidraw/excalidraw` 作为白板编辑器，通过 Vue 页面内挂载 React island 的方式接入。
- 每个 `live_class_session.id` 绑定唯一白板记录，避免长期课程下多节课共用一个白板。
- 通过 LiveKit DataChannel 同步白板变更和光标位置，复用现有课堂房间，不新增单独 websocket 房间。
- 后端新增白板快照接口，支持进入课堂恢复、课中节流保存、结束课堂前最终保存。
- 不改变现有音视频连接、AI 音频旁路、课堂结束确认、聊天与状态轮询流程。

## Out of Scope

- 第一阶段不做多人白板房间分享链接，不接入 excalidraw.com 的公开协作房间。
- 第一阶段不开放任意图片/文件无限上传，避免白板 JSON 过大和安全问题。
- 第一阶段不做复杂操作回放；仅保存当前白板快照。若后续需要课堂回放，再新增事件表。
- 第一阶段不把 AI 自动画图接入白板；AI 纪要仍在右侧面板独立展示。

## Impact

- 前端：`ai-tutor-web` 新增 React 依赖、白板 React host、Vue wrapper，并改造 `LiveClassroomPage.vue` 布局状态。
- 实时通信：`ai-tutor-web/src/modules/live/livekit.ts` 增加 DataChannel 发送/订阅封装。
- 后端：`live-class-service` 新增白板 entity/mapper/service/controller 与迁移 SQL。
- 数据库：新增 `live_whiteboard` 表，以 `live_session_id` 唯一约束绑定一节课。
- 测试：补充前端单测、后端接口测试、两端浏览器 E2E 同步与刷新恢复测试。
