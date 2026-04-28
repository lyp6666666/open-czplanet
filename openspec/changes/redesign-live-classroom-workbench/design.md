# Design: Live Classroom Workbench

## Reference Decomposition

图二的体验不是“网页详情页”，而是“会议课堂工作台”。核心元素如下：

1. Top Status Bar
   - 左侧：品牌/课堂名，例如 `数学 · 试课`
   - 中间状态：`进行中`、课堂计时
   - 右侧：宫格视图、成员数、设置、结束课堂

2. Left Tool Rail
   - 垂直工具入口：白板、文档、屏幕、互动、工具
   - 底部辅助入口：表情/成员/聊天
   - 宽度固定，避免挤压视频画面

3. Main Video Stage
   - 远端视频占据主视觉，比例稳定
   - 本地视频悬浮在右下角
   - 画面内覆盖成员名、麦克风/摄像头状态
   - 无远端视频时使用等待态，但保持同样舞台尺寸

4. Bottom Meeting Controls
   - 只放课堂操作，不混入说明文案
   - 动作包括：静音、开启/关闭视频、共享屏幕、邀请、成员、聊天、录制、更多
   - 控制项使用 icon + label 结构，状态变化只改变图标/强调色，不改变布局宽度

5. Right Insight Panel
   - 固定宽度，承载 AI 能力
   - 顶部标题：`实时总结`
   - 切换：`AI 总结` / `课堂纪要`
   - AI 总结内容使用时间线/分组卡片：
     - 知识点回顾
     - 例题讲解
     - 课堂互动与答疑
   - 底部保留 AI 提问输入区域
   - 当用户切换到聊天时，复用同一面板区域展示课中聊天

## Layout Rules

- Desktop `>= 1200px`:
  - Grid: `72px tool rail` + `minmax(0, 1fr) stage` + `392px insight panel`
  - Top bar spans full width
  - Bottom controls align under stage, not under right panel

- Tablet `900px - 1199px`:
  - Right panel becomes collapsible/stacked below stage
  - Tool rail remains left side

- Mobile `< 900px`:
  - Tool rail becomes horizontal toolbar under top bar
  - Right panel stacks below video
  - Controls wrap into two rows with stable button sizes

## State Model

- Connection state:
  - `connecting` -> 顶部显示 `加入中`
  - `connected` + session `IN_PROGRESS` -> 顶部显示 `进行中`
  - `reconnecting` -> 顶部显示 `重连中`
  - `disconnected` -> 顶部显示 `已断开`

- Peer state:
  - 有远端视频：展示远端画面
  - 仅 peer joined：展示等待视频画面恢复
  - peer 未加入：展示等待对方加入

- AI state:
  - `ACTIVE` -> 实时总结可见
  - degraded/failed/off -> 保留面板，但用状态说明替代空白
  - `asrEnabled` -> 展示 ASR 是否已接通；AI 默认自动开启，不提供手动开关
  - `llmEnabled` + `segmentCount` + `lastLlmSummaryTs` -> 展示 3-5 分钟阶段总结进度，不由前端主动触发 LLM
  - `latestStageSummary` 更新时 -> 右侧卡片以动态时间线方式刷新

## Visual Direction

- 风格：轻量会议软件工作台，明亮、克制、可长时间使用。
- 视频区域优先，不使用营销卡片视觉。
- 背景使用极浅冷灰，控制与工具面板白底，强调色保留平台蓝绿系但降低饱和。
- 卡片圆角控制在 8px 左右，只有视频画面和面板边界需要明显圆角。

## AI Realtime Summary

- 右侧实时总结必须表现为“持续运行的系统”，包含 ASR 接通状态、转写段数、LLM 阶段总结周期和上次生成时间。
- 前端只读取 `ai/state`，不直接调用 LLM；LLM 由 ai-agent 在累计有效转写且达到时间窗口后自动执行。
- 默认周期按 300 秒展示，文案表达为 3-5 分钟自动整理一次，避免给用户造成每秒都调用模型的误解。
- 如果 ASR 或 LLM 降级，面板仍保留并明确提示，不影响 LiveKit 音视频通话。
- 课前准备页提供 AI 实时总结与 AI 课后总结开关，写入 live-class session 后在本节课生效。
- AI 音频采用独立旁路：浏览器采集本地麦克风 PCM16 分片，静音 RMS 低于阈值时不上传；live-class-service 负责鉴权并转发到 ai-agent，ai-agent 再接入 ASR 与实时编排。
- 课堂标题使用 `对方姓名 · 学科/课程标题 · 试课/正式课`，避免继续显示静态占位。
