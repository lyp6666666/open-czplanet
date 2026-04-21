# 邮箱通知能力后端设计

## 1. 文档信息

- 文档名称：邮箱通知能力接口/表结构/任务调度设计
- 关联 PRD：[邮箱通知与绑定能力产品 PRD](./email-notification-prd.md)
- 适用服务：`tutor-appointment-service`、`videoCall-IM-service`、课程/课堂相关服务、通知任务模块
- 文档日期：2026-04-21

---

## 2. 设计目标

本方案用于承接短信业务通知退出后的邮箱通知能力，覆盖：

1. 主邮箱绑定、验证码验证、修改、解绑状态管理
2. 学生侧课后总结专用第二邮箱
3. 消息 2 小时未读提醒
4. 开课提醒，提醒时间与站内提醒配置一致
5. 每节课课后总结邮件推送
6. 未绑定后补绑邮箱时，补发最近一次可补发课后总结
7. 邮件发送记录、任务状态、失败重试与运营查询

---

## 3. 服务边界建议

### 3.1 `tutor-appointment-service`

负责：

1. 用户邮箱资料与验证码能力
2. 学生第二邮箱配置
3. 邮箱绑定状态查询
4. 开课提醒任务创建与课节状态联动
5. 课后总结通知触发入口
6. 提供内部用户邮箱查询接口

原因：

- 当前用户资料、`/user/me`、`user_settings`、课节 `tutor_appointment` 均在该服务内或与该服务强相关。

### 3.2 `videoCall-IM-service`

负责：

1. 监听消息创建后的未读提醒任务创建
2. 在任务触发时校验 `room_read_state`
3. 按消息类型生成脱敏摘要
4. 调用通知发送能力投递邮件

原因：

- 当前消息、会话、已读状态均在该服务内。

### 3.3 通知模块

建议一期先作为公共业务模块落在 `tutor-appointment-service` 或独立 `notification` package 中，后续抽为单独服务。

负责：

1. 邮件模板渲染
2. 发送任务统一入库
3. 重试与失败记录
4. 调用第三方邮件服务

---

## 4. 数据表设计

## 4.1 用户邮箱表 `user_email`

不建议直接把邮箱字段全部加在 `user` 表中，因为本需求包含主邮箱、第二邮箱、状态、验证码、退信和多场景用途，独立表更利于扩展。

```sql
CREATE TABLE `user_email` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '邮箱绑定记录id',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
  `email_type` varchar(32) NOT NULL COMMENT '邮箱类型 PRIMARY主邮箱 SUMMARY_ONLY课后总结专用邮箱',
  `email` varchar(255) NOT NULL COMMENT '邮箱地址',
  `email_masked` varchar(255) DEFAULT NULL COMMENT '脱敏邮箱',
  `verify_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '验证状态 PENDING待验证 VERIFIED已验证 INVALID已失效',
  `verified_at` datetime(3) DEFAULT NULL COMMENT '验证通过时间',
  `bind_source` varchar(32) DEFAULT NULL COMMENT '绑定来源 MY_PAGE/COURSE_PAGE/CHAT_PAGE/SUMMARY_PAGE',
  `bounce_status` varchar(32) NOT NULL DEFAULT 'NORMAL' COMMENT '退信状态 NORMAL正常 SOFT_BOUNCE软退信 HARD_BOUNCE硬退信',
  `last_notify_at` datetime(3) DEFAULT NULL COMMENT '最近一次业务通知发送时间',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态 1有效 0删除',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_user_email_type_active` (`user_id`, `email_type`, `status`),
  KEY `idx_email` (`email`),
  KEY `idx_user_type_status` (`user_id`, `email_type`, `verify_status`),
  KEY `idx_bounce_status` (`bounce_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户邮箱绑定表';
```

约束说明：

1. `PRIMARY` 每个用户最多一条有效记录。
2. `SUMMARY_ONLY` 仅学生用户可配置，后端必须校验用户类型。
3. `SUMMARY_ONLY` 允许与 `PRIMARY` 使用相同邮箱。
4. 主邮箱不可被其他用户重复绑定，建议通过业务校验完成。
5. 第二邮箱是否允许跨账号重复使用，建议允许，因为家长可能管理多个孩子账号；但需防止同一用户重复配置多条。

## 4.2 邮箱验证码表 `email_verify_code`

```sql
CREATE TABLE `email_verify_code` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '邮箱验证码id',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
  `email` varchar(255) NOT NULL COMMENT '目标邮箱',
  `email_type` varchar(32) NOT NULL COMMENT '邮箱类型 PRIMARY/SUMMARY_ONLY',
  `code_hash` varchar(128) NOT NULL COMMENT '验证码hash',
  `scene` varchar(32) NOT NULL COMMENT '场景 BIND/CHANGE/REBIND',
  `expire_at` datetime(3) NOT NULL COMMENT '过期时间',
  `verified_at` datetime(3) DEFAULT NULL COMMENT '验证成功时间',
  `try_count` int(11) NOT NULL DEFAULT 0 COMMENT '验证尝试次数',
  `send_ip` varchar(64) DEFAULT NULL COMMENT '发送请求ip',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/VERIFIED/EXPIRED/CANCELED',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_user_email_type_status` (`user_id`, `email`, `email_type`, `status`),
  KEY `idx_expire_at` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮箱验证码表';
```

验证码规则：

1. 验证码 6 位数字。
2. 有效期建议 10 分钟。
3. 同邮箱同场景 60 秒内不可重复发送。
4. 同用户同类型邮箱每日最多发送 10 次。
5. 同 IP 每分钟最多 5 次。
6. 验证错误 5 次后该验证码作废。

## 4.3 邮件模板表 `email_template`

```sql
CREATE TABLE `email_template` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '模板id',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `subject_template` varchar(255) NOT NULL COMMENT '标题模板',
  `body_template` mediumtext NOT NULL COMMENT '正文模板',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
  `version` int(11) NOT NULL DEFAULT 1 COMMENT '模板版本',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_template_code_version` (`template_code`, `version`),
  KEY `idx_template_code_status` (`template_code`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件模板表';
```

模板编码：

1. `EMAIL_VERIFY_CODE`
2. `UNREAD_MESSAGE_REMINDER`
3. `LESSON_START_REMINDER`
4. `LESSON_SUMMARY`
5. `EMAIL_CHANGED_NOTICE`
6. `LESSON_SUMMARY_BACKFILL`

## 4.4 邮件通知任务表 `email_notification_task`

```sql
CREATE TABLE `email_notification_task` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '邮件通知任务id',
  `task_key` varchar(128) NOT NULL COMMENT '业务幂等key',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码',
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型 EMAIL_VERIFY/UNREAD_MESSAGE/LESSON_START/LESSON_SUMMARY',
  `biz_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '业务id，如msgId或lessonId',
  `receiver_uid` bigint(20) UNSIGNED DEFAULT NULL COMMENT '收件用户id',
  `receiver_role` varchar(32) DEFAULT NULL COMMENT '收件角色 TEACHER/STUDENT/PARENT_SUMMARY',
  `email_type` varchar(32) DEFAULT NULL COMMENT '邮箱类型 PRIMARY/SUMMARY_ONLY',
  `email` varchar(255) NOT NULL COMMENT '收件邮箱',
  `subject` varchar(255) DEFAULT NULL COMMENT '渲染后标题',
  `payload_json` json DEFAULT NULL COMMENT '模板变量与业务上下文',
  `scheduled_at` datetime(3) NOT NULL COMMENT '计划发送时间',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/VALIDATING/SENDING/SENT/FAILED/CANCELED/EXPIRED',
  `retry_count` int(11) NOT NULL DEFAULT 0 COMMENT '重试次数',
  `max_retry_count` int(11) NOT NULL DEFAULT 3 COMMENT '最大重试次数',
  `last_error` varchar(1024) DEFAULT NULL COMMENT '最近失败原因',
  `sent_at` datetime(3) DEFAULT NULL COMMENT '发送成功时间',
  `opened_at` datetime(3) DEFAULT NULL COMMENT '打开时间',
  `clicked_at` datetime(3) DEFAULT NULL COMMENT '点击时间',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_task_key` (`task_key`),
  KEY `idx_status_scheduled` (`status`, `scheduled_at`),
  KEY `idx_receiver_uid` (`receiver_uid`),
  KEY `idx_biz` (`biz_type`, `biz_id`),
  KEY `idx_template_status` (`template_code`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件通知任务表';
```

`task_key` 建议：

1. 验证码：`EMAIL_VERIFY:{userId}:{emailType}:{email}:{codeId}`
2. 未读消息：`UNREAD:{roomId}:{receiverUid}:{latestMsgId}`
3. 开课提醒：`LESSON_START:{lessonId}:{receiverUid}:{reminderMinutes}`
4. 课后总结：`LESSON_SUMMARY:{lessonId}:{receiverUid}:{emailType}`
5. 课后总结补发：`LESSON_SUMMARY_BACKFILL:{lessonId}:{receiverUid}:{emailType}`

## 4.5 邮件发送日志表 `email_send_log`

```sql
CREATE TABLE `email_send_log` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '发送日志id',
  `task_id` bigint(20) UNSIGNED NOT NULL COMMENT '任务id',
  `provider` varchar(64) DEFAULT NULL COMMENT '邮件服务商',
  `provider_message_id` varchar(128) DEFAULT NULL COMMENT '服务商消息id',
  `email` varchar(255) NOT NULL COMMENT '收件邮箱',
  `send_status` varchar(32) NOT NULL COMMENT '发送状态 SUCCESS/FAIL',
  `error_code` varchar(64) DEFAULT NULL COMMENT '错误码',
  `error_message` varchar(1024) DEFAULT NULL COMMENT '错误信息',
  `request_id` varchar(128) DEFAULT NULL COMMENT '请求id',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_provider_message_id` (`provider_message_id`),
  KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件发送日志表';
```

## 4.6 课后总结表建议 `lesson_summary`

若当前课后总结尚未有稳定存储表，建议新增。

```sql
CREATE TABLE `lesson_summary` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '课后总结id',
  `lesson_id` bigint(20) UNSIGNED NOT NULL COMMENT '课节id，对应tutor_appointment.id',
  `course_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '长期课程id',
  `teacher_uid` bigint(20) UNSIGNED NOT NULL COMMENT '教师uid',
  `student_uid` bigint(20) UNSIGNED NOT NULL COMMENT '学生uid',
  `title` varchar(255) DEFAULT NULL COMMENT '总结标题',
  `summary_status` varchar(32) NOT NULL DEFAULT 'GENERATING' COMMENT '状态 GENERATING/READY/FAILED',
  `summary_brief` varchar(1000) DEFAULT NULL COMMENT '邮件摘要',
  `summary_content` mediumtext DEFAULT NULL COMMENT '完整总结内容',
  `homework` varchar(2000) DEFAULT NULL COMMENT '作业/建议',
  `ready_at` datetime(3) DEFAULT NULL COMMENT '生成完成时间',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_lesson_summary` (`lesson_id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_student_ready` (`student_uid`, `summary_status`, `ready_at`),
  KEY `idx_teacher_ready` (`teacher_uid`, `summary_status`, `ready_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课后总结表';
```

---

## 5. 接口设计

## 5.1 用户邮箱状态查询

`GET /user/email`

响应：

```json
{
  "primaryEmail": {
    "emailMasked": "li***@gmail.com",
    "verifyStatus": "VERIFIED",
    "bounceStatus": "NORMAL",
    "verifiedAt": "2026-04-21T12:00:00"
  },
  "summaryEmail": {
    "emailMasked": "pa***@gmail.com",
    "verifyStatus": "VERIFIED",
    "bounceStatus": "NORMAL",
    "verifiedAt": "2026-04-21T12:00:00"
  },
  "canUseSummaryEmail": true,
  "tips": {
    "primaryEmailMissing": false,
    "summaryEmailMissing": false
  }
}
```

说明：

1. `summaryEmail` 仅学生用户返回，教师返回 `null`。
2. 前端也可从 `/user/me` 透出邮箱摘要，但建议单独接口便于独立刷新。

## 5.2 发送邮箱验证码

`POST /user/email/code`

请求：

```json
{
  "email": "parent@example.com",
  "emailType": "PRIMARY",
  "scene": "BIND"
}
```

响应：

```json
{
  "cooldownSeconds": 60,
  "expireSeconds": 600
}
```

校验：

1. 邮箱格式合法。
2. `PRIMARY` 不允许绑定到其他用户主邮箱。
3. `SUMMARY_ONLY` 仅学生用户允许。
4. 触发频控。

## 5.3 验证并绑定邮箱

`POST /user/email/verify`

请求：

```json
{
  "email": "parent@example.com",
  "emailType": "SUMMARY_ONLY",
  "code": "123456",
  "scene": "BIND",
  "bindSource": "MY_PAGE"
}
```

响应：

```json
{
  "emailType": "SUMMARY_ONLY",
  "emailMasked": "pa***@example.com",
  "verifyStatus": "VERIFIED",
  "verifiedAt": "2026-04-21T12:00:00"
}
```

行为：

1. 校验验证码。
2. 旧邮箱记录置为删除或失效。
3. 写入新的已验证邮箱记录。
4. 若满足补发条件，异步创建课后总结补发任务。

## 5.4 删除第二邮箱

`DELETE /user/email/summary`

说明：

1. 仅学生用户可调用。
2. 删除后不再向该邮箱发送课后总结。
3. 主邮箱不提供普通删除入口，只提供修改/换绑。

## 5.5 邮箱通知状态提示接口

`GET /user/email/reminder-hints`

请求参数：

1. `scene=MY_PAGE|CHAT|COURSE_LIST|COURSE_DETAIL|SUMMARY_DETAIL`
2. `courseId`
3. `lessonId`
4. `roomId`

响应：

```json
{
  "show": true,
  "level": "MIDDLE",
  "title": "绑定邮箱，避免错过重要提醒",
  "description": "绑定后可接收未读消息提醒、上课提醒和课后总结。",
  "actionText": "立即绑定",
  "actionTarget": "/account/email",
  "dismissible": true,
  "cooldownDays": 7
}
```

说明：

1. 前端可本地控制部分展示，但建议服务端返回核心决策，避免多端规则不一致。
2. 用户关闭提示后，前端调用埋点或保存到 `user_settings.settings_json`。

## 5.6 内部用户邮箱查询

`GET /internal/facade/users/{uid}/emails`

响应：

```json
{
  "userId": 10001,
  "userType": 2,
  "primaryEmail": {
    "email": "student@example.com",
    "verified": true,
    "bounceStatus": "NORMAL"
  },
  "summaryEmail": {
    "email": "parent@example.com",
    "verified": true,
    "bounceStatus": "NORMAL"
  }
}
```

用途：

1. `videoCall-IM-service` 触发未读邮件时查询收件邮箱。
2. 课后总结发送任务聚合教师和学生邮箱。

## 5.7 邮件打开与点击回传

`GET /api/v1/public/email/track/open?taskId={id}&token={token}`

`GET /api/v1/public/email/track/click?taskId={id}&token={token}&redirect={url}`

说明：

1. 打开追踪返回 1 像素透明图片。
2. 点击追踪记录 `clicked_at` 后跳转业务页。
3. `token` 使用服务端签名，避免任意篡改。

---

## 6. 任务调度设计

## 6.1 统一任务扫描器

建议新增 `EmailNotificationTaskScheduler`：

1. 每 30 秒扫描 `email_notification_task` 中 `status=PENDING` 且 `scheduled_at <= now()` 的任务。
2. 使用 `SELECT ... FOR UPDATE SKIP LOCKED` 或状态 CAS 更新抢占任务。
3. 将任务状态置为 `VALIDATING`。
4. 根据 `biz_type` 执行业务二次校验。
5. 校验通过后渲染模板并发送。
6. 成功置为 `SENT`。
7. 失败按重试策略更新 `scheduled_at`。

重试策略：

1. 第 1 次失败后 1 分钟重试。
2. 第 2 次失败后 5 分钟重试。
3. 第 3 次失败后 30 分钟重试。
4. 超过最大次数置为 `FAILED`。

## 6.2 消息 2 小时未读提醒任务

创建时机：

1. `videoCall-IM-service` 成功写入新消息后。
2. 对接收方创建延迟任务，`scheduled_at = message.create_time + 2小时`。

二次校验：

1. 消息存在且 `status=0`。
2. 消息不是撤回消息。
3. 房间状态有效。
4. `room_read_state.last_read_msg_id < latestMsgId`。
5. 收件人主邮箱已验证且退信状态正常。
6. 同用户同房间 24 小时内未发送过 `UNREAD_MESSAGE_REMINDER`。
7. 同用户全站当天未读提醒未超过 3 封。

消息摘要生成：

1. `TEXT` -> `你收到一条普通消息`
2. `IMG/FILE/SOUND/VIDEO` -> `你收到一条多媒体消息`
3. `SYSTEM.extra.bizType=TUTOR_APPLICATION` -> `你收到一条新的申请消息`
4. `SYSTEM.extra.bizType=LESSON_REQUEST` -> `你收到一条约课沟通消息`
5. `SYSTEM.extra.bizType=SCHEDULE_CHANGED` -> `你收到一条课程变更消息`
6. `SYSTEM.extra.bizType=LESSON_SUMMARY` -> `你收到一条课程结果通知`

## 6.3 开课提醒任务

创建时机：

1. 课节创建成功。
2. 课节确认成功。
3. 调课确认成功。
4. 站内提醒时间配置变更后，批量重建未来有效课节提醒任务。

提醒时间：

1. 从站内提醒配置读取，例如 `lesson.reminder.minutes=[30]`。
2. 邮件任务与站内任务共用配置。
3. `scheduled_at = lesson.start_time - reminderMinutes`。

二次校验：

1. `tutor_appointment.status=2` 或业务定义的已确认待开始状态。
2. 当前课节未取消、未拒绝、未完成。
3. `start_time` 与任务 payload 中记录的一致，若不一致则取消旧任务。
4. 课程未暂停、未结束。
5. 收件人主邮箱有效。

收件人：

1. 教师主邮箱。
2. 学生主邮箱。
3. 不发送到学生第二邮箱。

## 6.4 课后总结发送任务

创建时机：

1. 课节状态变为已完成。
2. 课后总结生成成功，`lesson_summary.summary_status=READY`。

收件人：

1. 教师主邮箱。
2. 学生主邮箱。
3. 学生第二邮箱。

二次校验：

1. 课节仍为已完成。
2. 总结状态为 `READY`。
3. 总结内容不为空。
4. 目标邮箱已验证且退信正常。
5. 同一 `lessonId + receiverUid + emailType` 未发送过。

## 6.5 补绑后课后总结补发任务

触发时机：

1. 主邮箱验证成功。
2. 学生第二邮箱验证成功。

补发查询：

1. 查询当前用户作为教师或学生最近一条 `READY` 总结。
2. `lesson_summary.ready_at >= now() - 24小时`。
3. 当前邮箱未对该总结发送过 `LESSON_SUMMARY` 或 `LESSON_SUMMARY_BACKFILL`。

任务创建：

1. `template_code=LESSON_SUMMARY_BACKFILL`。
2. `scheduled_at=now()`。
3. `task_key=LESSON_SUMMARY_BACKFILL:{lessonId}:{receiverUid}:{emailType}`。

---

## 7. 配置项

建议新增统一配置：

```yaml
email:
  enabled: true
  verify:
    expire-minutes: 10
    resend-cooldown-seconds: 60
    max-try-count: 5
  notification:
    unread-delay-minutes: 120
    unread-room-daily-limit: 1
    unread-user-daily-limit: 3
    max-retry-count: 3
  lesson:
    reminder-minutes: [30]
    summary-backfill-window-hours: 24
```

说明：

1. `lesson.reminder-minutes` 必须与站内提醒配置同源。
2. 若已有 Nacos 动态配置，应纳入 Nacos 而不是写死在配置文件。

---

## 8. 跨服务调用与事件

## 8.1 建议事件

1. `MessageCreatedEvent`
2. `LessonCreatedEvent`
3. `LessonRescheduledEvent`
4. `LessonCanceledEvent`
5. `LessonCompletedEvent`
6. `LessonSummaryReadyEvent`
7. `UserEmailVerifiedEvent`

## 8.2 一期实现建议

若暂不引入 MQ，可先使用服务内同步创建任务与定时扫描：

1. IM 写消息后在本服务创建未读任务。
2. 课节创建/调课/取消在 `tutor-appointment-service` 内维护提醒任务。
3. 课后总结生成成功后调用通知任务创建方法。
4. 后续再替换为 MQ 事件驱动。

---

## 9. 后台查询接口

建议管理后台增加：

1. `GET /admin/email/tasks`
2. `GET /admin/email/tasks/{id}`
3. `GET /admin/email/users/{uid}/emails`
4. `GET /admin/email/stats`
5. `POST /admin/email/tasks/{id}/retry`

筛选条件：

1. 用户 id
2. 邮箱
3. 模板编码
4. 业务类型
5. 状态
6. 时间范围

---

## 10. 安全与隐私

1. 邮箱地址前端仅脱敏展示。
2. 邮件中未读消息不展示完整正文。
3. 邮箱验证码只存 hash，不存明文。
4. 绑定主邮箱时校验唯一性，避免账号混淆。
5. 第二邮箱仅用于课后总结，不可用于登录、找回账号、账号安全验证。
6. 邮件回跳链接必须带短期签名或要求登录后查看详情。
7. 邮件模板不得展示手机号、微信号、详细地址等敏感信息。

---

## 11. 验收用例

### 11.1 邮箱绑定

1. 主邮箱发送验证码成功。
2. 验证码错误 5 次后作废。
3. 主邮箱绑定成功后 `/user/email` 返回 `VERIFIED`。
4. 学生可配置第二邮箱。
5. 教师不可配置第二邮箱。

### 11.2 未读提醒

1. 消息 2 小时未读发送邮件。
2. 2 小时内已读不发送。
3. 撤回消息不发送。
4. 同房间 24 小时只发送 1 封。
5. 邮件摘要不暴露原始正文。

### 11.3 开课提醒

1. 课节确认后按配置创建邮件提醒任务。
2. 调课后旧任务取消，新任务生成。
3. 取消课节后任务取消。
4. 第二邮箱不接收开课提醒。

### 11.4 课后总结

1. 总结 READY 后发送给教师主邮箱、学生主邮箱、学生第二邮箱。
2. 总结失败不发送。
3. 补绑邮箱后 24 小时内可补发最近一次总结。
4. 同一总结同一邮箱最多补发一次。

