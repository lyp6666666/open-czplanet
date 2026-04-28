# Design: Live Classroom Excalidraw Whiteboard

## Product Decision

白板模式不是在视频页里塞一个小画板，而是把课堂主舞台切到“白板优先”。这样教师写题、推导公式、画图时不被视频画面抢空间，同时保留人物状态，用户不会感觉音视频断了。

### 视频放置

- 桌面端 `>= 1200px`：白板占据中央主舞台，视频固定停靠在右上角浮层。
- 远端视频优先，尺寸约 `240px * 150px`；本地视频缩小为 `150px * 96px`，叠放在远端下方。
- 浮层带成员名、麦克风/摄像头状态、收起按钮；收起后变成一个“2 人在线”的胶囊，不遮挡书写。
- 白板模式下右侧 AI/聊天面板高度下收，给右上角视频区留出视觉重心；AI 内容区必须是独立滚动容器，不能挤压白板。
- 如果 AI 总结内容超过可视高度，只滚动 `summary-timeline` 内容，不滚动顶部课堂栏、左侧工具栏、底部会议控制。
- 平板 `900px - 1199px`：视频停靠在白板顶部横向条。
- 移动端 `< 900px`：视频改为底部横向 PiP 条，白板占据剩余空间，底部会议控制保持可触达。

### 页面结构

```text
┌──────────────────────────────────────────────────────────┐
│ Top classroom status bar                                  │
├──────┬───────────────────────────────────────┬───────────┤
│ Tool │ Whiteboard stage                       │ Video /   │
│ Rail │   ┌──────── remote video dock ──────┐ │ AI Panel  │
│      │   │                                  │ │           │
│      │   │       Excalidraw canvas          │ │           │
│      │   │                                  │ │           │
│      │   └──────────────────────────────────┘ │           │
├──────┴───────────────────────────────────────┴───────────┤
│ Bottom meeting controls                                   │
└──────────────────────────────────────────────────────────┘
```

## Binding Model

“一节课一个白板”的主键必须是 `live_class_session.id`。

原因：

- 当前前端路由仍使用 `/live/classroom/:courseId`，但后端 `LiveClassSession` 同时包含 `courseId` 和 `scheduleEventId`。
- 长期课程可能有多节 `scheduleEvent`，如果只按 `courseId` 绑定，第二节课会误用第一节课白板。
- 课堂真正的实时房间、参与者、AI 状态都已经围绕 `sessionId` 工作，因此白板也应以 `sessionId` 为唯一口径。

落库时可冗余 `course_id` 和 `schedule_event_id` 方便排查，但唯一约束只放在 `live_session_id`。

## Excalidraw Integration

官方文档说明 Excalidraw 通过 `@excalidraw/excalidraw` 组件嵌入项目，并需要安装 `react`、`react-dom` 与该包。它的 `onChange` 会返回 `elements`、`appState`、`files`，适合保存到后端；`updateScene` 可用于应用远端场景更新。

现有前端是 Vue3，因此采用 React island：

- `LiveWhiteboardPanel.vue`：Vue wrapper，负责接收 `sessionId`、`roomClient`、当前用户与只读状态。
- `src/modules/whiteboard/ExcalidrawWhiteboardHost.tsx`：React 组件，真正渲染 `<Excalidraw />`。
- `src/modules/whiteboard/mountExcalidrawWhiteboard.tsx`：提供 `mount(el, props)` 和 `unmount()`，让 Vue 生命周期管理 React root。
- `src/modules/whiteboard/whiteboardSync.ts`：封装快照加载、保存、LiveKit DataChannel 同步、防抖和远端更新合并。

容器必须给明确高度和宽度，因为 Excalidraw 会铺满父容器。白板模式切换、右侧面板收缩后，需要调用 `excalidrawAPI.refresh()` 纠正指针坐标。

## Realtime Sync

优先使用 LiveKit DataChannel，而不是新建 websocket：

- 两个用户本来就在同一个 LiveKit room。
- 音视频和白板生命周期一致，离开课堂即可断开同步。
- 可以区分可靠消息和非可靠消息：白板场景用 reliable，光标用 lossy。

消息 topic：

- `whiteboard.scene.snapshot`：可靠消息，发送节流后的场景快照或增量。
- `whiteboard.cursor`：非可靠消息，发送当前指针位置和在线状态。
- `whiteboard.presence`：可靠消息，进入/退出白板模式时广播，更新协作者列表。

第一阶段同步策略：

- 本地 `onChange` 后先更新本地状态。
- 800ms 防抖后通过 DataChannel 发送场景更新。
- 2-5 秒防抖后调用后端保存快照。
- 接收到远端更新时设置 `applyingRemote = true`，调用 `updateScene({ elements, appState, collaborators, captureUpdate: NEVER })`，避免写入本地 undo 栈和触发回环。
- 结束课堂、离开课堂、刷新页面前做一次最终保存。

冲突处理：

- MVP 可发送全量 scene，但合并时按 element `id + versionNonce/version` 取更新版本，避免双方同时画图时互相覆盖。
- 删除元素也必须保留一段时间的 `isDeleted` 记录参与合并，否则远端无法感知删除。
- 如果单次 payload 超过限制，降级为“只保存后端快照 + 提示网络同步较慢”，不影响音视频。

## Persistence API

前端 API：

```ts
type LiveWhiteboardSnapshot = {
  whiteboardId: number
  sessionId: number
  courseId: number
  scheduleEventId?: number | null
  sceneVersion: number
  scene: {
    elements: unknown[]
    appState: Record<string, unknown>
    files?: Record<string, unknown>
  }
  updatedAt?: string | null
}
```

后端接口：

- `GET /live/sessions/{sessionId}/whiteboard`
  - 返回本节课白板快照；不存在时创建空白板并返回。
- `PUT /live/sessions/{sessionId}/whiteboard/snapshot`
  - 保存快照，携带 `sceneVersion` 做乐观锁。
- `POST /live/sessions/{sessionId}/whiteboard/finalize`
  - 结束课堂前最终保存，可复用 `PUT`，但独立接口便于审计。

权限：

- 复用 live-class-service 现有参与者校验：只有本节课老师或学生可读写。
- 课程结束后默认可读不可写，除非后续增加“课后补充白板”权限。

## Database

```sql
CREATE TABLE live_whiteboard (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  live_session_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  schedule_event_id BIGINT NULL,
  scene_json MEDIUMTEXT NOT NULL,
  scene_version BIGINT NOT NULL DEFAULT 0,
  updated_by_uid BIGINT NULL,
  finalized TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_live_whiteboard_session (live_session_id),
  KEY idx_live_whiteboard_course (course_id),
  KEY idx_live_whiteboard_schedule_event (schedule_event_id)
);
```

`scene_json` 内容只保存必要字段：

- `elements`
- 安全白名单内的 `appState`，例如背景、滚动、缩放、当前名称，不保存临时 UI 状态。
- `files` 第一阶段默认关闭或限制大小；如果开放图片，需要转为对象存储 URL，不建议长期保存 base64。

## Frontend Changes

`LiveClassroomPage.vue`：

- 新增 `classroomMode = ref<'video' | 'whiteboard'>('video')`。
- 左侧“白板”按钮切换模式并显示 active。
- 白板模式下主舞台渲染 `LiveWhiteboardPanel`。
- 视频 DOM 不销毁，只改变 CSS 布局位置，确保 LiveKit track 不被反复 detach/attach。
- 白板模式下 `insight-panel` 增加 `whiteboard-compact`，高度收窄；`.summary-timeline`、`.side-section`、`.chat-panel` 使用 `min-height: 0 + overflow: auto`。
- 底部控制栏保持原有麦克风、摄像头、结束课堂逻辑。

`livekit.ts`：

- 增加 `sendData(topic, payload, reliable)`。
- 增加 `onDataReceived(listener)`，返回 unsubscribe。
- 对 JSON 编解码、topic 过滤、异常吞吐做封装，避免页面直接依赖 LiveKit 细节。

`live.ts`：

- 增加 `getWhiteboard(sessionId)`、`saveWhiteboardSnapshot(sessionId, payload)`、`finalizeWhiteboard(sessionId, payload)`。

## Visual Direction

白板应像一个“安静的课堂桌面”，不是会议软件弹窗堆叠：

- 白板背景默认暖白或浅米色，降低长时间书写刺眼感。
- 视频 dock 使用半透明白底、细边框和柔和阴影，但不遮挡工具栏。
- 浮层按钮只保留“收起/返回视频/成员在线”，不要放无关说明。
- 白板顶部只放同步状态、协作者头像、保存状态，不展示技术名词。
- 同步状态文案：
  - `已同步`
  - `正在同步...`
  - `网络较慢，已保存本地变更`
  - `只读查看`

## Rollout Plan

1. UI 骨架：先实现白板模式布局、视频浮层、模式切换，不接 Excalidraw。
2. 单机白板：接入 Excalidraw React island，支持本地绘制和刷新恢复。
3. 后端快照：新增表和 REST 接口，保存/读取本节课白板。
4. 实时同步：扩展 LiveKit DataChannel，同步 scene 和 cursor。
5. 收尾保护：结束课堂前最终保存、payload 限流、异常降级提示。
6. E2E：双浏览器进入同一课堂，教师绘制、学生实时看到，刷新后恢复。

## End-to-End Test Plan

- 前端单测：
  - 点击白板工具后出现 `live-whiteboard-stage`。
  - `stage-panel` 有 `whiteboard-active`，视频节点仍存在。
  - `insight-panel` 有 `whiteboard-compact`，AI 总结容器保持 `overflow: auto`。
- 后端接口测试：
  - `GET /live/sessions/{sessionId}/whiteboard` 返回或创建空白板。
  - `PUT /live/sessions/{sessionId}/whiteboard/snapshot` 保存 scene 并递增版本。
  - `POST /live/sessions/{sessionId}/whiteboard/finalize` 保存最终 scene 并进入只读归档。
- 浏览器 E2E：
  - mock 课堂接口进入 `/live/classroom/:courseId`。
  - 点击白板按钮，确认 Excalidraw 容器可见。
  - 校验右上角远端/本地视频 dock 可见。
  - 注入多段 AI 纪要，确认右侧 AI 内容区出现滚动条且页面主布局不溢出。
  - 保存接口返回后，白板同步状态显示 `已同步`。

## References

- Excalidraw Installation: https://docs.excalidraw.com/docs/%40excalidraw/excalidraw/installation
- Excalidraw Props: https://docs.excalidraw.com/docs/%40excalidraw/excalidraw/api/props
- Excalidraw API: https://docs.excalidraw.com/docs/%40excalidraw/excalidraw/api/props/excalidraw-api
- LiveCollaborationTrigger: https://docs.excalidraw.com/docs/%40excalidraw/excalidraw/api/children-components/live-collaboration-trigger
