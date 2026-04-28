## ADDED Requirements

### Requirement: 实时课堂必须使用工作台布局

实时课堂页面 MUST 使用全屏工作台布局，优先展示视频课堂，而不是普通卡片式详情页。

#### Scenario: 桌面端进入课堂
- **WHEN** 用户进入实时课堂
- **THEN** 页面 MUST 展示顶部课堂状态栏、左侧工具轨、中央视频舞台、右侧实时总结面板和底部控制栏
- **AND** 中央视频舞台 MUST 是首屏最大视觉区域

### Requirement: 视频舞台必须稳定展示远端与本地画面

系统 MUST 在中央舞台展示远端主画面，并在右下角展示本地小窗。

#### Scenario: 远端视频已接收
- **WHEN** 远端视频轨道已连接
- **THEN** 主舞台 MUST 展示远端视频
- **AND** 本地画面 MUST 以悬浮小窗展示

#### Scenario: 对方已入会但暂无视频
- **WHEN** 对方已加入课堂但远端视频轨道暂不可用
- **THEN** 页面 MUST 保持视频舞台尺寸
- **AND** MUST 展示等待视频画面恢复的状态

### Requirement: 底部控制栏必须只承载课堂动作

底部控制栏 MUST 使用稳定尺寸的 icon + label 控制项，避免按钮挤压和布局跳动。

#### Scenario: 用户切换麦克风或摄像头
- **WHEN** 用户点击麦克风或摄像头控制
- **THEN** 页面 MUST 调用现有媒体控制逻辑
- **AND** 控制项 MUST 立即反映开启/关闭状态

### Requirement: 右侧面板必须承载 AI 总结和课堂消息

右侧面板 MUST 支持在 AI 总结、课堂纪要和课中聊天之间切换。

#### Scenario: AI 状态可用
- **WHEN** AI 状态为可用或正在整理
- **THEN** 页面 MUST 展示当前主题、阶段摘要、学生提问、课堂重点和作业候选

#### Scenario: AI 实时总结默认自动运行
- **WHEN** 课堂 AI 策略开启
- **THEN** 页面 MUST 展示 AI 自动监听状态
- **AND** MUST 展示 ASR 接通状态、转写段数、LLM 阶段总结进度和上次总结时间
- **AND** 前端 MUST NOT 提供需要用户手动开启 AI 总结的主操作

#### Scenario: 课前选择 AI 能力
- **WHEN** 用户在课堂准备页进入课堂前
- **THEN** 页面 MUST 允许用户选择是否开启 AI 实时总结
- **AND** 页面 MUST 允许用户选择是否开启 AI 课后总结
- **AND** 系统 MUST 将选择写入实时课堂会话

#### Scenario: AI 实时总结开启后上传有效语音
- **WHEN** 用户开启 AI 实时总结并进入课堂
- **THEN** 前端 MUST 使用独立于 LiveKit 的音频旁路上传本地麦克风 PCM 分片
- **AND** 前端 MUST 在长时间未检测到有效声音时暂停上传以节省 ASR 资源
- **AND** ai-agent MUST 在收到 ASR 转写段后累计触发 LLM 阶段总结

#### Scenario: 切换到课中聊天
- **WHEN** 用户点击聊天入口
- **THEN** 右侧面板 MUST 展示课中聊天组件
- **AND** MUST 保留发送课中消息能力

### Requirement: AI 总结状态不得影响音视频通话

实时课堂 MUST 将 AI 状态查询与 LiveKit 音视频连接解耦。

#### Scenario: AI 状态接口异常
- **WHEN** `ai/state` 查询失败或返回降级状态
- **THEN** 页面 MUST 保留已建立的音视频通话
- **AND** MUST 在右侧面板展示 AI 状态不可用或降级提示
- **AND** MUST NOT 断开或重建 LiveKit 房间连接

### Requirement: 课堂标题必须展示真实课堂上下文

实时课堂标题 MUST 使用对方名称、学科/课程标题和课堂类型组成。

#### Scenario: 用户进入课堂
- **WHEN** 页面加载课堂信息
- **THEN** 标题 MUST 展示对方名称
- **AND** MUST 展示学科或课程标题
- **AND** MUST 标识试课或正式课

### Requirement: 页面必须适配小屏

实时课堂 MUST 在小屏下保持视频、控制和右侧信息可用。

#### Scenario: 视口宽度小于 900px
- **WHEN** 用户在小屏打开实时课堂
- **THEN** 左侧工具轨 MUST 转为横向工具栏
- **AND** 右侧面板 MUST 堆叠到视频舞台下方
- **AND** 底部控制栏 MUST 自动换行且不遮挡内容
