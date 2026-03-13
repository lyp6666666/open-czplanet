## 1. 项目设置

- [x] 1.1 创建 `ai-tutor-miniprogram` 目录并初始化 Vue 3 + TypeScript 的 Uni-app 项目
- [x] 1.2 为微信小程序配置 `manifest.json`（设置 appid 占位符）
- [x] 1.3 安装并配置 `uView Plus`（或兼容的 UI 库）
- [x] 1.4 设置 Pinia 用于状态管理并定义 `useUserStore`
- [x] 1.5 创建 `src/utils/request.ts` 包装 `uni.request` 并带有拦截器

## 2. 后端认证

- [x] 2.1 向 `tutor-appointment-service` 添加 `wechat-miniprogram` 依赖（如果 API 调用需要）
- [x] 2.2 创建 `WechatAuthService` 处理 `jscode2session` 交换
- [x] 2.3 更新 `User` 实体以包含 `wechat_openid`
- [x] 2.4 实现 `/api/auth/wechat-login` 端点以返回 JWT

## 3. 前端认证与个人资料

- [x] 3.1 在 `useUserStore` 中实现 `uni.login()` 逻辑
- [x] 3.2 创建带有登录按钮的“我的”页面（`pages/me/index.vue`）
- [x] 3.3 实现“登录”动作以调用后端并存储 JWT
- [x] 3.4 登录后在“我的”页面显示用户信息（昵称、头像）

## 4. 核心功能（首页与导师）

- [x] 4.1 创建带有横幅和搜索栏的“首页”（`pages/home/index.vue`）
- [x] 4.2 使用 `u-list` 或类似组件创建“导师列表”组件
- [x] 4.3 集成 `/api/tutors` 端点以获取并显示导师
- [x] 4.4 创建“导师详情”页面（`pages/tutor/detail.vue`）

## 5. 支付集成

- [x] 5.1 更新 `payment-service` 中的 `WechatPaymentStrategy` 以支持 `JSAPI` 交易类型
- [x] 5.2 调整 `/api/payment/create` 端点以支持小程序参数
- [x] 5.3 在导师详情页面创建“预约课程”流程
- [x] 5.4 实现带有后端参数的 `uni.requestPayment` 调用
