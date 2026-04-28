# 提案：重建微信小程序端

## Why

当前 `ai-tutor-miniprogram` 已存在 Uni-app 项目、基础页面和部分 API 封装，但整体仍处于“功能散装、流程未闭环、与 Web 端业务主链路不一致”的状态。虽然 `npm run type-check` 当前可以通过，但从产品可用性看，小程序端缺少统一状态机、统一导航、统一角色准入、支付与聊天门禁、课程履约入口和端到端验收标准，导致实际业务基本不可用。

仓库内历史归档 OpenSpec 已覆盖“新增小程序”“小程序功能补全”“Mock 集成”等早期意图，但这些文档已经落后于当前业务：Web 端和后端已演进出申请、信息费支付、聊天解锁、试课合作、正式课表、退款、邮箱提醒等复杂闭环。小程序端需要按当前线上业务重新设计和开发，而不是继续局部修补。

## What Changes

- 将微信小程序端重建为一个稳定的 Uni-app 微信小程序客户端，保留可复用工程基础，但按当前业务重新梳理页面、状态、接口和验收流程。
- 明确小程序首期支持两类用户：学生/家长、教师；机构端和管理端不进入首期。
- 重建小程序端核心流程：
  - 启动、配置、登录、角色初始化、用户资料刷新
  - 学生找老师、教师详情、收藏、发起申请
  - 学生发布需求、管理需求、查看申请
  - 教师浏览需求、需求详情、发起申请、收藏需求
  - 教师入驻/认证状态、资料维护
  - 申请中心、通过/拒绝、信息费支付、聊天解锁
  - 聊天列表、聊天室、消息收发、系统业务卡片、未读/已读
  - 试课合作、改期/取消、试课结果、正式课表、我的课程
  - 账号设置、邮箱提醒、资源上传、错误恢复
- 使用当前后端真实接口为基准，明确每个流程的接口依赖、状态流转和前后端校验点。
- 建立小程序端质量门禁：类型检查、微信端构建、核心流程冒烟、Mock/Dev 环境策略、发布前验收清单。

## Scope

### In Scope

- 前端项目：`ai-tutor-miniprogram`
- 后端接口依赖：
  - `tutor-appointment-service`: `/user/*`, `/api/v1/parent/*`, `/api/v1/tutor/*`, `/api/v1/assets/*`, `/api/v1/schedule/*`, `/teacher/verification/*`
  - `videoCall-IM-service`: `/chat/*`, `/chat/application/*`, `/chat/collaboration/*`, `/courses/*`
  - `payment-service`: `/payment/prepay`, `/payment/orders/{orderNo}`, `/payment/dev/orders/{orderNo}/mock-success`
- 小程序端发布与联调规范，包括本地、开发、线上预发布配置。

### Out of Scope

- 管理后台小程序化。
- 机构端完整迁移。
- 原生直播课堂/白板能力迁移。
- 推翻现有后端业务模型重新设计。
- 首期不要求完整复刻 Web 端所有高级筛选和运营位，但必须保留可扩展接口契约。

## Capabilities

### New Capabilities

- `miniprogram-app-shell`: 小程序启动、路由、配置、鉴权、角色和全局错误治理。
- `miniprogram-student-flow`: 学生/家长找老师、发布需求、管理需求、发起申请和查看申请。
- `miniprogram-tutor-flow`: 教师入驻、浏览需求、申请沟通、收藏需求和资料状态治理。
- `miniprogram-application-payment-chat`: 申请、信息费支付、聊天解锁、IM 和业务卡片闭环。
- `miniprogram-course-flow`: 试课合作、课程、课表、试课结果和退款入口。
- `miniprogram-release-quality`: 小程序构建、联调、Mock、冒烟和发布验收标准。

### Modified Capabilities

- `wechat-auth`: 从“可登录”升级为“登录后角色、资料、认证状态、Mock/真实模式一致”的完整认证能力。
- `wechat-miniprogram-frontend`: 从早期页面补全升级为按当前业务主链路重建。

## Impact

- `ai-tutor-miniprogram/src/pages.json`: 路由、分包、TabBar、页面标题和登录跳转策略需要重整。
- `ai-tutor-miniprogram/src/stores`: 需要拆分或重构用户、角色、申请、聊天、课程等状态。
- `ai-tutor-miniprogram/src/api`: 需要按后端领域重新封装，并补齐类型。
- `ai-tutor-miniprogram/src/pages`: 需要按流程重做页面，不再以零散 Demo 页面为准。
- 后端原则上不新增大业务模型，但需要在开发中发现“小程序缺口接口”时补充轻量适配或字段。
- 测试需要新增小程序端基础自动化/冒烟文档，并至少保留 `npm run type-check` 和 `npm run build:mp-weixin` 作为最低门禁。
