## 1. 数据库与模型

- [x] 1.1 为 teacher_profile 增加 default_greeting 字段并同步实体映射
- [x] 1.2 在 IM 库创建 room_read_state 表与索引并同步实体映射

## 2. 资料服务（tutor-appointment-service）

- [x] 2.1 扩展 TeacherExtInfo 支持 defaultGreeting 更新入参
- [x] 2.2 扩展 TeacherProfile 支持 defaultGreeting 回显出参
- [x] 2.3 更新 TeacherProfileMapper.xml 读写 default_greeting
- [x] 2.4 为 /user/me 与 /user/updateUserInfo 增加覆盖测试

## 3. IM 服务（videoCall-IM-service）

- [x] 3.1 新增发起沟通接口（room 获取/创建 + 首次招呼语幂等发送）
- [x] 3.2 完善 room_read_state 的已读上报与未读数查询接口
- [x] 3.3 增加 SSE 实时消息流接口与连接管理组件
- [x] 3.4 补齐 RocketMQ MsgSendConsumer，将新消息推送到在线连接
- [x] 3.5 增加 IM 端到端集成测试（创建 room→发消息→拉取校验→已读/未读校验）

## 4. 前端（ai-tutor-web）

- [x] 4.1 首页热门需求卡片增加“立即沟通”并打通跳转到聊天页
- [x] 4.2 顶部头像下拉增加“默认打招呼语”设置入口与弹窗
- [x] 4.3 发起沟通统一改为调用 start 接口并透传 greeting
- [x] 4.4 聊天页接入实时消息流并与分页拉取兼容
- [ ] 4.5 增加前端 e2e/组件测试覆盖核心链路（可选，按现有测试框架落地）
