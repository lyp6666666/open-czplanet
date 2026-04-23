# 邮件通知闭环落地方案（一期可直接使用）

## 1. 目标

基于当前已审核通过的腾讯云模板，先落地一版可直接投入使用的邮件通知闭环，覆盖：

1. 邮箱绑定验证码通知
2. 聊天消息 2 小时未读提醒
3. 课程即将开始提醒
4. 课后总结已生成通知

本方案聚焦“一期能跑通、能上线、能运营排查”的最小闭环，不依赖邮件内跳转链接，不等待补发模板、邮箱变更通知等后续能力。

---

## 2. 一期已确认可用模板

当前腾讯云已审核通过模板：

1. `邮箱验证通知`
2. `未读消息待查看`
3. `课程即将开始提醒`
4. `课后总结已生成`

建议系统内模板编码固定为：

| 模板名称 | template_code | biz_type | 用途 |
| --- | --- | --- | --- |
| 邮箱验证通知 | `EMAIL_VERIFY_CODE` | `EMAIL_VERIFY` | 用户绑定邮箱时发送验证码 |
| 未读消息待查看 | `UNREAD_MESSAGE_REMINDER` | `UNREAD_MESSAGE` | 消息 2 小时未读召回 |
| 课程即将开始提醒 | `LESSON_START_REMINDER` | `LESSON_START` | 课前提醒教师和学生 |
| 课后总结已生成 | `LESSON_SUMMARY` | `LESSON_SUMMARY` | 课后总结 READY 后通知 |

一期暂不启用：

1. `EMAIL_CHANGED_NOTICE`
2. `LESSON_SUMMARY_BACKFILL`
3. 邮件打开/点击追踪
4. 邮件内跳转链接

---

## 3. 一期闭环范围

### 3.1 用户侧闭环

1. 用户在“邮箱设置”页发起绑定。
2. 系统发送 `邮箱验证通知`。
3. 用户输入验证码完成验证。
4. 主邮箱状态变为 `VERIFIED`。
5. 后续系统才允许向该邮箱发送业务邮件。

### 3.2 业务通知闭环

1. 业务系统在事件发生时创建邮件任务。
2. 定时任务扫描到期任务并抢占执行。
3. 发送前进行业务二次校验。
4. 校验通过后调用腾讯云发送邮件。
5. 成功则记录发送日志并更新状态。
6. 失败则按策略重试。
7. 重试后仍失败则记录失败状态，依赖站内提示兜底。

### 3.3 管理与排查闭环

1. 所有邮件任务必须落库。
2. 所有发送结果必须记录 `email_send_log`。
3. 后台可按模板、用户、邮箱、状态筛选。
4. 支持失败任务重试。

---

## 4. 服务边界

### 4.1 `tutor-appointment-service`

负责：

1. 用户邮箱绑定、验证、状态管理
2. 验证码邮件任务创建
3. 开课提醒任务创建/取消
4. 课后总结任务创建
5. 统一邮件任务处理与第三方发送
6. 内部邮箱查询接口

### 4.2 `videoCall-IM-service`

负责：

1. 新消息写入后创建未读提醒任务
2. 到发送时机时校验会话未读状态
3. 生成脱敏消息摘要

---

## 5. 闭环状态机

### 5.1 邮箱状态

| 状态 | 含义 | 是否允许业务邮件 |
| --- | --- | --- |
| `PENDING` | 已填写邮箱，待验证 | 否 |
| `VERIFIED` | 验证成功 | 是 |
| `INVALID` | 已失效/退信/被替换 | 否 |

规则：

1. 只有 `VERIFIED` 才能接收业务邮件。
2. 若邮箱被换绑或失效，后续任务发送前必须拦截。

### 5.2 邮件任务状态

| 状态 | 含义 |
| --- | --- |
| `PENDING` | 已创建，等待执行 |
| `VALIDATING` | 执行前二次校验中 |
| `SENT` | 发送成功 |
| `FAILED` | 超过重试次数后失败 |
| `CANCELED` | 业务状态变化，不再需要发送 |

一期实现建议统一走：

1. 创建任务时写入 `PENDING`
2. 扫描器抢占后改为 `VALIDATING`
3. 校验通过并发送成功后改为 `SENT`
4. 失败后回到 `PENDING` 等待重试，或最终改为 `FAILED`
5. 不再满足条件则改为 `CANCELED`

---

## 6. 四类邮件的直接可用闭环

## 6.1 邮箱验证通知

### 触发时机

1. 用户首次绑定主邮箱
2. 用户重新发送验证码
3. 用户换绑邮箱时发送新邮箱验证码

### 任务创建

- `template_code = EMAIL_VERIFY_CODE`
- `biz_type = EMAIL_VERIFY`
- `scheduled_at = now()`

### payload 建议

```json
{
  "code": "123456",
  "expireMinutes": 10,
  "emailType": "PRIMARY"
}
```

### 发送前校验

验证码邮件可直接发送，无需业务状态二次校验。

### 绑定成功条件

1. 验证码存在且未过期
2. 验证码状态为 `PENDING`
3. 输入验证码校验通过
4. 尝试次数未超限

### 成功后动作

1. `email_verify_code.status = VERIFIED`
2. `user_email.verify_status = VERIFIED`
3. 写入 `verified_at`
4. 后续业务任务允许投递

### 失败兜底

1. 60 秒内不可重复发送
2. 验证失败 5 次作废
3. 前端展示“重新发送验证码”

---

## 6.2 未读消息待查看

### 触发时机

1. 新消息成功写入 IM
2. 接收人主邮箱已验证
3. 创建延迟任务，发送时间为消息创建后 2 小时

### 任务创建

- `template_code = UNREAD_MESSAGE_REMINDER`
- `biz_type = UNREAD_MESSAGE`
- `scheduled_at = message.create_time + 120 minutes`
- `task_key = UNREAD:{roomId}:{receiverUid}:{msgId}`

### payload 建议

```json
{
  "msgId": 9527,
  "roomId": 1001,
  "fromUid": 2001,
  "senderName": "王老师",
  "senderRole": "老师",
  "messageSummary": "你收到一条约课沟通消息"
}
```

### 发送前二次校验

必须同时满足：

1. 消息仍存在
2. 消息未撤回、未删除
3. 房间状态有效
4. 接收人仍未读该消息
5. 接收人主邮箱仍为 `VERIFIED`
6. 退信状态为 `NORMAL`
7. 同一用户同一房间 24 小时内未发送过未读提醒
8. 同一用户当日未读提醒总数未超过 3 封

### 发送成功后动作

1. 任务标记为 `SENT`
2. 写入发送日志
3. 更新 `user_email.last_notify_at`

### 失败兜底

1. 保留站内未读红点
2. 保留消息列表未读状态
3. 邮件失败不影响站内消息正常使用

---

## 6.3 课程即将开始提醒

### 触发时机

1. 课节创建成功
2. 课节确认成功
3. 调课成功后重建任务

### 任务创建

- `template_code = LESSON_START_REMINDER`
- `biz_type = LESSON_START`
- `scheduled_at = lesson.start_time - reminderMinutes`
- `task_key = LESSON_START:{lessonId}:{receiverUid}:{reminderMinutes}`

### payload 建议

```json
{
  "lessonId": 3001,
  "courseId": 9001,
  "courseName": "初二数学提升课",
  "lessonDate": "2026-04-22",
  "lessonTime": "19:30",
  "counterpartName": "李同学",
  "prepareTips": "请提前 5 分钟进入课堂并检查网络",
  "startTime": "2026-04-22T19:30:00",
  "reminderMinutes": 30
}
```

### 发送对象

1. 教师主邮箱
2. 学生主邮箱

一期不发给：

1. 学生第二邮箱

### 发送前二次校验

1. 课节仍存在
2. 课节状态仍为待开始/已确认
3. 未取消、未改为结束、未暂停
4. `start_time` 未变化
5. 收件人主邮箱仍有效

### 调课/取消闭环

1. 调课后取消旧任务
2. 按新时间重建任务
3. 取消课节后全部提醒任务改为 `CANCELED`

### 失败兜底

1. 我的课程页继续展示待上课状态
2. 站内提醒继续保留

---

## 6.4 课后总结已生成

### 触发时机

1. 课节状态已完成
2. 总结生成成功
3. `lesson_summary.summary_status = READY`

### 任务创建

- `template_code = LESSON_SUMMARY`
- `biz_type = LESSON_SUMMARY`
- `scheduled_at = now()`
- `task_key = LESSON_SUMMARY:{lessonId}:{receiverUid}:{emailType}`

### payload 建议

```json
{
  "lessonId": 3001,
  "courseId": 9001,
  "courseName": "初二数学提升课",
  "lessonDate": "2026-04-22",
  "lessonTime": "19:30",
  "summaryHighlight": "完成一次函数图像与性质讲解，学生能独立判断增减性。",
  "homeworkAdvice": "完成配套练习第 3、4 题，复习课堂笔记。",
  "title": "初二数学提升课总结"
}
```

### 发送对象

一期建议：

1. 教师主邮箱
2. 学生主邮箱

如学生第二邮箱已经可用，可按当前实现一并发送；若业务侧尚未准备好，可先关闭 `SUMMARY_ONLY` 的任务创建。

### 发送前二次校验

1. 总结记录存在
2. `summary_status = READY`
3. `summary_content` 非空
4. 目标邮箱已验证
5. 同一 `lessonId + receiverUid + emailType` 未成功发送过

### 失败兜底

1. 总结详情页仍可站内查看
2. 不发送空总结
3. 若发送失败，仅记录失败，不影响总结页面展示

---

## 7. 统一发送流程

建议统一按以下时序实现：

1. 业务系统触发事件
2. 创建 `email_notification_task`
3. 扫描器定时拉取到期 `PENDING` 任务
4. CAS 抢占状态为 `VALIDATING`
5. 根据 `biz_type` 做二次校验
6. 渲染主题与正文
7. 调用腾讯云邮件发送接口
8. 写入 `email_send_log`
9. 成功则任务置为 `SENT`
10. 失败则按重试策略更新任务

---

## 8. 腾讯云邮件发送接入要求

一期必须遵守：

1. 邮件正文不包含外链跳转
2. 模板内容与审核通过版本保持一致
3. 邮件主题与模板名称保持稳定，不频繁改动

发送器入参建议：

```json
{
  "templateCode": "LESSON_SUMMARY",
  "toEmail": "user@example.com",
  "subject": "课后总结已生成",
  "htmlBody": "<html>...</html>",
  "requestId": "uuid"
}
```

第三方回执建议记录：

1. `provider = TENCENT`
2. `provider_message_id`
3. `request_id`
4. `send_status`
5. `error_code`
6. `error_message`

---

## 8.1 腾讯云发送器建议新增接口

为了让邮件闭环可直接接入真实发送，建议在 `tutor-appointment-service` 新增以下接口与 DTO：

### 发送接口

建议新增接口：

- `com.ai.tutor.appointment.integration.email.EmailSender`

职责：

1. 接收渲染后的主题与 HTML
2. 调用腾讯云邮件发送接口
3. 返回统一发送结果

建议方法签名：

```java
EmailSendResponse send(EmailSendRequest request);
```

### 模板渲染接口

建议新增接口：

- `com.ai.tutor.appointment.integration.email.EmailTemplateRenderer`

职责：

1. 根据 `templateCode` 和 `payload` 渲染主题
2. 根据本地 HTML 模板渲染正文
3. 屏蔽具体模板来源差异

建议方法签名：

```java
RenderedEmail render(String templateCode, Map<String, Object> payload);
```

### 请求 DTO

建议字段：

```java
templateCode
toEmail
subject
htmlBody
requestId
fromEmail
fromName
replyToEmail
```

### 响应 DTO

建议字段：

```java
success
provider
providerMessageId
requestId
errorCode
errorMessage
```

### 真实实现类建议

后续落地时建议新增：

1. `TencentCloudEmailSender`
2. `LocalHtmlEmailTemplateRenderer`

其中：

1. `TencentCloudEmailSender` 只负责第三方调用，不处理业务判断
2. `LocalHtmlEmailTemplateRenderer` 负责加载 [email-templates/tencent](/Users/luyipeng/project/ai_platform/ai-platform/email-templates/tencent) 下已过审 HTML，并按 payload 替换变量

---

## 8.2 腾讯云发送器建议新增配置

结合当前配置结构，建议在 `email.sender` 下新增如下配置：

| 配置项 | 说明 | 示例 |
| --- | --- | --- |
| `email.sender.provider` | 发送器类型 | `MOCK` / `TENCENT` |
| `email.sender.enabled` | 发送器总开关 | `true` |
| `email.sender.endpoint` | 腾讯云 SES API 域名 | `ses.tencentcloudapi.com` |
| `email.sender.region` | 腾讯云区域 | `ap-guangzhou` |
| `email.sender.secret-id` | 腾讯云 SecretId | 敏感配置 |
| `email.sender.secret-key` | 腾讯云 SecretKey | 敏感配置 |
| `email.sender.from-email` | 发件邮箱 | `no-reply@domain.com` |
| `email.sender.from-name` | 发件人名称 | `创智星球` |
| `email.sender.reply-to-email` | 回复邮箱 | `support@domain.com` |
| `email.sender.connect-timeout-ms` | 连接超时 | `3000` |
| `email.sender.read-timeout-ms` | 读取超时 | `5000` |

对应配置样例已补充到：

- [tutor-appointment-service-dev.yaml](/Users/luyipeng/project/ai_platform/ai-platform/docs/nacos/templates/tutor-appointment-service-dev.yaml)
- [videoCall-IM-service-dev.yaml](/Users/luyipeng/project/ai_platform/ai-platform/docs/nacos/templates/videoCall-IM-service-dev.yaml)

配置切换建议：

1. 开发环境默认 `provider=MOCK`
2. 联调环境切到 `provider=TENCENT`
3. 正式环境开启真实腾讯云密钥

### 当前已审核通过模板 ID

根据当前腾讯云控制台已审核通过模板，一期建议直接配置：

| 模板名称 | template_code | 腾讯云 TemplateId |
| --- | --- | --- |
| 邮箱验证通知 | `EMAIL_VERIFY_CODE` | `173982` |
| 未读消息待查看 | `UNREAD_MESSAGE_REMINDER` | `173983` |
| 课程即将开始提醒 | `LESSON_START_REMINDER` | `173984` |
| 课后总结已生成 | `LESSON_SUMMARY` | `173985` |

### Nacos 必配项

#### `tutor-appointment-service`

必须配置：

```yaml
email:
  enabled: true
  sender:
    provider: "TENCENT"
    enabled: true
    endpoint: "ses.tencentcloudapi.com"
    region: "ap-guangzhou"
    secret-id: "<你的腾讯云 SecretId>"
    secret-key: "<你的腾讯云 SecretKey>"
    from-email: "<已完成发信域验证的发件邮箱>"
    from-name: "创智星球"
    reply-to-email: "<客服或支持邮箱>"
    template-dir: "email-templates/tencent"
    template-ids:
      EMAIL_VERIFY_CODE: 173982
      LESSON_START_REMINDER: 173984
      LESSON_SUMMARY: 173985
```

#### `videoCall-IM-service`

必须配置：

```yaml
email:
  sender:
    provider: "TENCENT"
    enabled: true
    endpoint: "ses.tencentcloudapi.com"
    region: "ap-guangzhou"
    secret-id: "<你的腾讯云 SecretId>"
    secret-key: "<你的腾讯云 SecretKey>"
    from-email: "<已完成发信域验证的发件邮箱>"
    from-name: "创智星球"
    reply-to-email: "<客服或支持邮箱>"
    template-ids:
      UNREAD_MESSAGE_REMINDER: 173983
```

注意：

1. `from-email` 必须是腾讯云 SES 已验证的发件地址或域名下地址。
2. 两个服务都要配 `secret-id/secret-key`，否则会继续走 `MOCK` 或发送失败。
3. 若当前只想先联调预约服务链路，可先只把 `tutor-appointment-service` 切到 `TENCENT`。

---

## 9. 重试策略

统一重试策略建议：

1. 第一次失败后 1 分钟重试
2. 第二次失败后 5 分钟重试
3. 第三次失败后 30 分钟重试
4. 超过最大次数后置为 `FAILED`

不应重试的错误：

1. 邮箱状态无效
2. 业务状态已变化
3. 模板被停用
4. 参数缺失导致渲染失败

这些情况应直接置为 `CANCELED` 或 `FAILED`，避免空转。

---

## 10. 站内兜底策略

邮件只是补充触达，站内必须始终可完成闭环。

### 未读消息

1. 聊天列表未读红点保留
2. 消息中心保留未读标记

### 开课提醒

1. 我的课程页展示最近待上课课节
2. 站内提醒与课程详情提示继续存在

### 课后总结

1. 总结详情页可直接查看
2. 若未绑定邮箱，展示“绑定邮箱后可接收课后总结提醒”

---

## 11. 一期必须补齐的接口与能力

为了让闭环可直接使用，至少要保证以下能力齐备：

### 11.1 用户邮箱能力

1. `GET /user/email`
2. `POST /user/email/code`
3. `POST /user/email/verify`
4. `GET /internal/facade/users/{uid}/emails`

### 11.2 任务能力

1. 创建邮件任务
2. 扫描到期任务
3. 任务状态流转
4. 失败重试

### 11.3 发送能力

1. 模板渲染
2. 腾讯云发送封装
3. 发送日志记录

### 11.4 后台能力

1. 按模板查看任务
2. 按用户查看最近发送记录
3. 查看失败原因
4. 支持重试失败任务

---

## 12. 一期上线顺序

建议严格按以下顺序上线：

1. 邮箱绑定验证码链路
2. 开课提醒链路
3. 课后总结链路
4. 未读消息提醒链路

原因：

1. 绑定链路是所有业务邮件前提
2. 开课提醒和课后总结价值更高、规则更稳定
3. 未读消息提醒频控与二次校验最复杂，适合最后接入

---

## 13. 一期验收清单

### 邮箱验证通知

1. 发送验证码后能收到邮件
2. 验证成功后邮箱状态变为 `VERIFIED`
3. 60 秒内重复发送被拦截
4. 验证错误 5 次后验证码失效

### 未读消息待查看

1. 2 小时未读会创建并发送邮件
2. 2 小时内已读则不发送
3. 同一房间 24 小时内只发 1 封
4. 摘要不暴露完整消息正文

### 课程即将开始提醒

1. 课节确认后按提醒时间创建任务
2. 调课后旧任务取消，新任务生成
3. 取消课节后提醒不发送

### 课后总结已生成

1. 总结 READY 后创建任务并发送
2. 总结内容为空时不发送
3. 同一总结同一邮箱不重复发送

### 日志与后台

1. 每封邮件都有发送日志
2. 失败原因可查
3. 失败任务可人工重试

---

## 13.1 联调测试验收方法

为了确保“邮件相关闭环可以直接使用”，联调测试建议按以下层次验收。

### A. 配置验收

目标：确认发送器配置正确、生效可读。

检查项：

1. `email.enabled=true`
2. `email.sender.provider=TENCENT`
3. `secret-id`、`secret-key`、`from-email` 已正确下发
4. 服务启动后无配置绑定异常

通过标准：

1. 应用启动成功
2. 日志无发送器初始化报错

### B. 验证码邮件联调

步骤：

1. 调用 `POST /user/email/code`
2. 检查 `email_notification_task` 是否创建 `EMAIL_VERIFY_CODE`
3. 等待调度器执行
4. 检查目标邮箱是否收到 `邮箱验证通知`
5. 调用 `POST /user/email/verify` 完成验证

验收标准：

1. 任务状态从 `PENDING -> VALIDATING -> SENT`
2. `email_send_log.provider = TENCENT`
3. `email_send_log.send_status = SUCCESS`
4. `user_email.verify_status = VERIFIED`

失败场景验收：

1. 60 秒内重复发送被拦截
2. 错误验证码无法绑定成功
3. 验证错误超限后验证码失效

### C. 开课提醒联调

步骤：

1. 准备一个 30 分钟后开始的课节
2. 确保教师和学生主邮箱均为 `VERIFIED`
3. 调用创建/确认课节流程
4. 检查是否创建 `LESSON_START_REMINDER` 任务
5. 到提醒时间后检查是否成功发送

验收标准：

1. 教师和学生各生成 1 条任务
2. 两条任务均成功发送
3. 邮件主题与模板内容正确
4. 调课后旧任务取消、新任务重建
5. 取消课节后不发送提醒

### D. 课后总结联调

步骤：

1. 准备一个已完成课节
2. 生成一条 `lesson_summary.summary_status = READY` 的总结
3. 调用总结任务创建逻辑
4. 检查是否创建 `LESSON_SUMMARY` 任务
5. 检查目标邮箱是否收到 `课后总结已生成`

验收标准：

1. 教师主邮箱成功收到
2. 学生主邮箱成功收到
3. 总结内容为空时不发送
4. 同一总结重复触发不会重复发送

### E. 未读消息提醒联调

步骤：

1. 创建一条聊天消息
2. 接收方主邮箱状态为 `VERIFIED`
3. 创建 `UNREAD_MESSAGE_REMINDER` 延迟任务
4. 将任务时间调到当前或等待 2 小时阈值
5. 检查发送结果

验收标准：

1. 未读时会发送
2. 提前已读则任务取消
3. 消息撤回后不发送
4. 同房间 24 小时内只发送 1 封

### F. 重试与失败验收

步骤：

1. 人为配置错误密钥或发送失败环境
2. 触发任一邮件发送
3. 观察重试行为

验收标准：

1. 首次失败后按 1/5/30 分钟重试
2. 超过最大次数后状态变为 `FAILED`
3. `email_send_log` 中可看到失败原因
4. 修复配置后可通过后台重试成功

### G. 数据库与日志验收

必须检查：

1. `email_notification_task.subject` 已正确落库
2. `email_notification_task.sent_at` 已写入
3. `email_send_log.provider_message_id` 有值
4. `email_send_log.request_id` 可用于追踪
5. `user_email.last_notify_at` 在成功发送后更新

### H. 最终上线前通过标准

以下全部满足，才视为邮件闭环可直接上线：

1. 验证码邮件联调通过
2. 开课提醒联调通过
3. 课后总结联调通过
4. 未读消息提醒联调通过
5. 失败重试联调通过
6. 后台能查询任务和失败原因
7. 发送链路已切换到 `provider=TENCENT`

---

## 14. 当前代码实现与本方案的差距

结合当前仓库代码，现状如下：

### 已具备

1. 邮件任务表、日志表、验证码表设计已明确
2. `EmailNotificationServiceImpl` 已具备任务创建、调度、状态流转骨架
3. `UnreadEmailReminderServiceImpl` 已具备未读提醒任务创建与校验逻辑
4. HTML 模板已准备，且已去除邮件内链接

### 待补齐

1. 将 `MOCK` 发送替换为真实腾讯云发送器
2. 渲染主题和 HTML 正文，而不是只写主题
3. 后台任务查询与重试接口
4. 第二邮箱是否在一期启用的开关控制
5. 更细的模板 payload 与渲染字段对齐

---

## 15. 一期结论

只要完成以下 4 件事，这套邮件闭环就可以直接使用：

1. 打通邮箱绑定验证码发送与验证
2. 将现有任务调度器接入腾讯云真实发送
3. 接通开课提醒、课后总结、未读消息三类任务创建入口
4. 补一个后台查询/重试入口用于运营排查

完成后即可形成完整闭环：

用户绑定邮箱 -> 系统按业务触发邮件 -> 发送前校验 -> 发送日志可追踪 -> 失败可重试 -> 站内继续兜底。
