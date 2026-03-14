## ADDED Requirements

### Requirement: 提供统一的 API 测试客户端与 BaseResponse 解析
系统 MUST 在 `qa/automation/` 内提供统一的 API 客户端封装，支持复用连接（Session）、统一超时、统一鉴权注入，并对后端 `BaseResponse` 结构（code/message/data）做一致解析与断言。

#### Scenario: API 调用统一解析业务响应结构
- **WHEN** 用例调用任意后端接口并获得 HTTP 200 响应
- **THEN** 客户端 MUST 能解析出 code/message/data 并提供对 code 的统一断言能力

### Requirement: 支持手机号验证码登录并获取 Bearer Token
系统 MUST 支持通过接口方式完成手机号验证码登录（`/user/sendcode` + `/user/loginOrRegister`），并在后续请求中自动携带 `Authorization: Bearer <token>`。

#### Scenario: 登录成功后可访问需要鉴权的接口
- **WHEN** 用例完成登录并获得 token
- **THEN** 用例调用 `/user/me` MUST 返回当前用户信息且不返回未登录错误

### Requirement: 覆盖核心业务 API 链路（主流程）
系统 MUST 提供覆盖主流程的接口用例集合，至少包含：资料读取/更新、对象存储上传、IM 会话创建/发消息/已读 ACK/未读查询、支付统一下单/查单/回调闭环的关键断言。

#### Scenario: 覆盖 IM 会话与消息核心链路
- **WHEN** 教师调用 `/chat/room/start` 发起沟通并可选携带 greeting
- **THEN** 系统 MUST 创建或复用 room，并在首次建立联系时最多发送一次 greeting 消息

#### Scenario: 覆盖支付闭环核心链路
- **WHEN** 用户调用 `/payment/prepay` 创建扫码支付订单并轮询 `/payment/orders/{orderNo}`
- **THEN** 系统 MUST 返回可展示的支付要素与过期时间，并在回调后将订单状态迁移为 SUCCESS

### Requirement: 覆盖关键异常与风控分支
系统 MUST 覆盖关键异常与风控分支的接口用例集合，至少包含：无 token 访问鉴权接口、错误验证码登录、支付回调验签失败、支付回调金额不一致、IM 频控或参数校验失败。

#### Scenario: 未携带 token 访问鉴权接口返回明确错误
- **WHEN** 用例不携带 Authorization 访问 `/user/me`
- **THEN** 系统 MUST 返回未登录错误码或等价的拒绝响应
