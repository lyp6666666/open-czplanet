## 1. 基础架构

- [ ] 1.1 修改 `userStore`，增加 `currentRole` 状态和切换逻辑
- [ ] 1.2 封装 `jobs` 相关 API 请求 (`create`, `list`, `mine`)
- [ ] 1.3 封装 `chat` 相关 API 请求 (`listRooms`, `listMessages`, `send`)

## 2. 角色切换与首页

- [ ] 2.1 将原 `pages/home/index` 拆分为 `ParentHome` 组件
- [ ] 2.2 实现 `TutorHome` 组件（展示需求广场列表）
- [ ] 2.3 修改 `pages/home/index`，根据 `currentRole` 动态渲染组件
- [ ] 2.4 在 `pages/me/index` 增加角色切换入口（如果是双重身份）或申请成为教师入口

## 3. 需求管理（家长端）

- [ ] 3.1 创建“发布需求”页面 `pages/post/index`
- [ ] 3.2 创建“我的需求”页面 `pages/my-jobs/index`
- [ ] 3.3 创建“需求详情”页面 `pages/job/detail`

## 4. 聊天功能

- [ ] 4.1 创建“聊天列表”页面 `pages/chat/list`
- [ ] 4.2 创建“聊天室”页面 `pages/chat/room`
- [ ] 4.3 实现聊天列表和消息的轮询机制
- [ ] 4.4 在导师详情页/需求详情页添加入口跳转到聊天室
