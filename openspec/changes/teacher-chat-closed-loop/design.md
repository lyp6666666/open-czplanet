## Context

当前系统已具备 IM 的基础能力：1v1 room 创建/获取、会话列表、消息分页、发送文本消息（落库 + 更新 room.lastMsgId），并在事务内发布 MessageSendEvent 通过 RocketMQ 投递消息事件。但缺少：

- 教师端从首页热门需求的沟通入口，导致“触达→沟通”不闭环
- 默认打招呼语配置与首次沟通自动触发能力
- 实时收发通道（WebSocket/SSE）与消息状态能力（已读/未读）
- 可重复验证的端到端测试（创建会话→首条消息→拉取/实时接收）

代码与职责边界：

- `tutor-appointment-service` 负责 `user/teacher_profile/student_profile` 的资料读写（`/user/me`、`/user/updateUserInfo`）
- `videoCall-IM-service` 负责 `room/message` 的会话与消息能力（`/chat/*`）

## Goals / Non-Goals

**Goals:**

- 教师从需求列表/详情与首页热门需求统一发起沟通，首次发起保证创建 room
- 教师可设置默认打招呼语并回显；首次建立联系时自动发送且幂等
- IM 补齐企业级核心能力的最小闭环：实时推送通道 + 已读/未读状态 + 关键链路验证测试
- 对现有接口保持兼容：不破坏 `/chat/room`、`/chat/msg`、消息分页接口的既有调用

**Non-Goals:**

- 群聊、会话搜索、敏感词审核、文件/图片/语音等多媒体消息全量支持
- 跨设备多端已读同步的复杂策略（仅实现 1v1 基础已读/未读）
- 复杂的在线状态系统、消息撤回/编辑、端到端加密

## Decisions

- 默认打招呼语落库位置
  - 选择：`teacher_profile` 新增字段 `default_greeting`
  - 理由：教师专属设置、与资料同域；`tutor-appointment-service` 为 Owner，改动集中且无需动 `user` 表通用结构
  - 备选：复用 `teacher_profile.introduction`（语义不匹配）；落在 `user` 表（影响面更大）

- 首次沟通自动发送招呼语的触发位置
  - 选择：IM 新增 `POST /chat/room/start`（或同名语义接口），在后端完成“获取/创建 room + 首条招呼语幂等发送”
  - 理由：统一入口、跨前端多入口一致性；避免前端并发/刷新导致重复发送
  - 幂等策略：仅当 room 的 `last_msg_id` 为空（或 room 内无消息）且请求携带 greeting 时发送；对 room 行加锁（`SELECT ... FOR UPDATE`）或用更新条件防止重复写
  - 备选：前端进入聊天页后检测消息为空再发送（易重复、弱一致）

- 实时收发通道选择
  - 选择：SSE（`SseEmitter`）作为第一期实时通道
  - 理由：实现成本低、与现有 Spring MVC 兼容，适合 1v1 IM MVP；WebSocket 可在后续按需升级
  - 事件来源：复用 RocketMQ 的 SEND_MSG_TOPIC；补齐 consumer，将消息转换为客户端事件并推送给在线用户连接

- 已读/未读状态数据模型
  - 选择：新增表 `room_read_state(room_id, uid, last_read_msg_id, last_read_time, ...)`
  - 理由：不污染 `room` 表的角色耦合字段；适合未来扩展多成员会话；可按 uid/room 维度快速计算未读
  - 未读计算：`count(message) where room_id=? and to_uid=? and id > last_read_msg_id and status=0`

## Risks / Trade-offs

- [SSE 连接数与资源占用] → 使用 uid 维度单连接/多连接上限、心跳与超时清理；必要时引入连接池/限流
- [RocketMQ 本地开发不可用导致测试困难] → 测试以 REST 拉取为主；实时推送以“SessionManager 单测 + controller 层模拟”分层验证
- [首次招呼语重复发送] → room 行级锁/条件更新 + 服务端幂等判断；前端不再做二次自动发送
- [跨服务字段更新不一致] → greeting 仅由 `tutor-appointment-service` 写入；前端从 `/user/me` 拉取后在 start 接口中透传

## Migration Plan

- 数据库
  - `teacher_profile` 增加 `default_greeting` 字段
  - IM 库增加 `room_read_state` 表
- 后端
  - `tutor-appointment-service`：扩展 DTO/Entity/Mapper + `/user/me` 回显
  - `videoCall-IM-service`：新增 start、SSE、已读/未读相关接口；补齐 MQ consumer 推送
- 前端
  - 首页热门需求卡片新增“立即沟通”
  - 顶部头像下拉新增“默认打招呼语”入口（弹窗编辑）
  - 发起沟通统一调用 start 接口（携带 greeting），并跳转聊天页
- 回滚
  - 新增字段/表均为向后兼容；回滚只需停止新接口调用并忽略新增字段

## Open Questions

- 默认打招呼语是“仅在首次建立联系时发送一次”还是“每次打开会话都自动填充输入框”？
- 家长/学生端是否也需要默认招呼语（当前设计仅教师端）？
- SSE 是否需要鉴权 token 放在 header/cookie 以支持跨域与多端登录（当前沿用现有鉴权方式）？
