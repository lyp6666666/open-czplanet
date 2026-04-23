# 腾讯云邮件模板

目录下文件可直接作为腾讯云邮件推送的 HTML 模板上传使用。

模板编码对应关系：

- `EMAIL_VERIFY_CODE.html`
- `UNREAD_MESSAGE_REMINDER.html`
- `LESSON_START_REMINDER.html`
- `LESSON_SUMMARY.html`
- `LESSON_SUMMARY_BACKFILL.html`
- `EMAIL_CHANGED_NOTICE.html`

说明：

- 文件名按项目中的 `template_code` 命名。
- 模板变量采用 `{{variable}}` 形式，占位后续由服务端渲染。
- `EMAIL_CHANGED_NOTICE` 当前在设计文档中已定义，但代码里暂未看到发送逻辑，可先在腾讯云侧预创建备用。
