# 小程序接口依赖与差距表

## 已确认可直接复用的接口

| 领域 | 接口 | 用途 | 小程序改造点 |
| --- | --- | --- | --- |
| 认证 | `POST /user/sendcode` | 发送短信验证码 | 加入频控提示和倒计时。 |
| 认证 | `POST /user/loginOrRegister` | 手机号登录/注册 | 登录成功后必须统一刷新 `/user/me`。 |
| 认证 | `POST /user/wechatLogin` | 微信 code 登录 | Mock code 必须限制在开发环境。 |
| 用户 | `GET /user/me` | 完整用户上下文 | 作为角色、教师状态和头像昵称的唯一基线。 |
| 用户 | `POST /user/updateUserInfo` | 更新资料/教师扩展资料 | 需要明确教师资料字段和认证状态映射。 |
| 邮箱 | `GET /user/email` | 邮箱状态 | 可复用。 |
| 邮箱 | `POST /user/email/code` | 邮箱验证码 | 可复用。 |
| 邮箱 | `POST /user/email/verify` | 验证邮箱 | 可复用。 |
| 邮箱 | `DELETE /user/email/summary` | 删除课后总结邮箱 | 可复用。 |
| 上传 | `POST /api/v1/assets/upload` | 头像/证书/凭证上传 | 需要统一上传组件。 |
| 学生 | `GET /api/v1/parent/tutors/page` | 学生浏览教师 | 需要类型化列表字段。 |
| 学生 | `POST /api/v1/parent/jobs` | 发布需求 | 需要重做表单字段和校验。 |
| 学生 | `GET /api/v1/parent/jobs/mine` | 我的需求 | 需要确认分页字段是 `list` 还是 `items`。 |
| 学生/教师 | `GET /api/v1/parent/jobs/{id}` | 学生视角需求详情 | 可复用。 |
| 教师 | `GET /api/v1/parent/jobs/feed` | 需求广场 | 可复用。 |
| 教师 | `GET /api/v1/parent/jobs/{id}/view` | 教师视角需求详情 | 可复用。 |
| 认证 | `POST /teacher/verification/education/submit` | 学历认证 | 可复用。 |
| 认证 | `POST /teacher/verification/realname/submit` | 实名认证 | 需要确定小程序端采集字段。 |
| 申请 | `GET /chat/application/sent/page` | 发出的申请 | 需要新增小程序页面。 |
| 申请 | `GET /chat/application/received/page` | 收到的申请 | 需要新增小程序页面。 |
| 申请 | `GET /chat/application/unread` | 申请未读数 | 消息 Tab 使用。 |
| 申请 | `GET /chat/application/{applicationId}` | 申请详情 | 需要新增页面。 |
| 申请 | `POST /chat/application/{applicationId}/decision` | 通过/拒绝申请 | 可复用。 |
| 申请/聊天 | `POST /chat/application/{applicationId}/enter-chat` | 支付后进入聊天 | 支付成功后使用。 |
| IM | `GET /chat/room/page` | 聊天列表 | 需要业务状态字段映射。 |
| IM | `GET /chat/public/msg/page` | 消息历史 | 需要消息类型适配。 |
| IM | `POST /chat/msg` | 发送消息 | 加 pending/失败重试。 |
| IM | `POST /chat/read/ack` | 已读回执 | 当前小程序缺失。 |
| IM | `GET /chat/events/sync` | 增量事件同步 | 替代全量短轮询。 |
| 支付 | `POST /payment/prepay` | 当前统一下单接口 | 小程序应替换旧 `/payment/create`。 |
| 支付 | `GET /payment/orders/{orderNo}` | 轮询支付状态 | 支付页必需。 |
| 支付 | `POST /payment/dev/orders/{orderNo}/mock-success` | Dev/E2E 模拟成功 | 仅开发环境显示。 |
| 合作 | `POST /chat/collaboration/proposal` | 发起试课合作 | 小程序缺失。 |
| 合作 | `POST /chat/collaboration/proposal/{proposalId}/response` | 同意/拒绝合作 | 小程序缺失。 |
| 日程 | `GET /api/v1/schedule/availability/day` | 双方日程可用性 | 小程序缺失 UI。 |
| 课程 | `GET /courses/my` | 我的课程 | 小程序缺失。 |
| 课程 | `GET /courses/{courseId}` | 课程详情 | 小程序缺失。 |
| 课程 | `POST /courses/{courseId}/trial-result` | 试课结果 | 小程序缺失。 |
| 日程 | `POST /api/v1/schedule/courses/{courseId}/weekly-schedule` | 正式课表 | 小程序缺失。 |

## 需要后端确认或补齐的差距

| 差距 | 风险 | 建议 |
| --- | --- | --- |
| 小程序微信 JSAPI 支付的 `openid` 与支付 `payParams` 是否完整闭环 | 当前 `/payment/prepay` 支持 `tradeType` 和 `openid` 字段，但现有小程序仍用旧 `/payment/create`；若返回扫码链接，生产小程序无法直接完成原生支付。 | 明确小程序支付标准：`channel=WECHAT`, `tradeType=JSAPI`, `openid` 来自登录绑定，并返回 `uni.requestPayment` 所需参数。 |
| 教师认证状态字段口径不清 | 当前小程序把 `teacherProfile` 存在视为 `APPROVED`，会绕过审核。 | `/user/me` 需要稳定返回教师入驻状态、实名状态、学历状态、拒绝原因。 |
| `/api/v1/parent/jobs/mine` 分页字段不统一 | 当前页面读取 `res.items`，其他列表常用 `res.list`，可能导致空列表误判。 | 在 API 类型中统一适配，并确认后端返回结构。 |
| 收藏状态接口尚未接入当前页面 | 学生/教师收藏能力在后端存在，但小程序页面没有状态回显。 | 增加 check/page/add/delete 封装与按钮状态。 |
| 申请创建接口的推荐用法需统一 | 当前教师详情使用 `/chat/application/start-chat`，规格更倾向申请中心先创建申请。 | 确认首期统一使用 `/chat/application` 还是保留 `start-chat` 并封装成同一领域方法。 |
| 小程序不适合 SSE | 后端已有 `/chat/stream`，但小程序端不稳定支持 EventSource。 | 首期使用 `/chat/events/sync` 增量同步；后续再评估 WebSocket。 |
| 课程改期/取消接口小程序路径未完全映射 | 规格要求我的课程支持改期/取消，但当前接口分散在 schedule / appointment / IM。 | 在开发前补一张课程动作到接口的映射表。 |
| 退款入口需要区分信息费退款和课程/试课退款 | 后端存在 `/chat/refund/*`、`/courses/{courseId}/trial-refund/apply`、支付退款服务。 | 小程序首期先按试课失败教师信息费退款接入，其他退款仅展示状态或引导客服。 |

## 支付接口现状备注

当前 `payment-service` 的主收银台接口是：

- `POST /payment/prepay`
- `GET /payment/orders/{orderNo}`
- `POST /payment/dev/orders/{orderNo}/mock-success`

`PrepayRequest` 已包含：

- `contextType`
- `contextId`
- `channel`
- `tradeType`
- `openid`

`PrepayResponse` 已包含：

- `orderNo`
- `amountFen`
- `channel`
- `qrCodeUrl`
- `codeUrl`
- `expireTime`
- `payParams`

因此小程序重建时应优先走 `/payment/prepay`，并把 `tradeType=JSAPI` 和 `payParams` 作为生产微信支付闭环的验收重点。
