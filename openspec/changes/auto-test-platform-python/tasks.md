## 1. 模块骨架与依赖管理

- [x] 1.1 新建 `qa/automation/` 目录与分层结构（core/api/ui/fixtures/tests/scripts）
- [x] 1.2 初始化 Python 依赖管理与锁定（pytest、requests、playwright、report 插件）
- [x] 1.3 配置 pytest 基础能力（markers、日志、JUnit XML 输出、用例发现规则）
- [x] 1.4 提供本地可执行入口脚本（smoke/regression）与环境变量说明

## 2. API 自动化框架落地

- [x] 2.1 实现统一 `ApiClient`（Session 复用、超时、BaseResponse 解析、统一断言）
- [x] 2.2 实现鉴权与登录封装（手机号验证码登录获取 token，并自动注入 Bearer Token）
- [x] 2.3 按领域实现 API clients（user/assets/chat/payment/jobs）并提供最小可用调用接口
- [x] 2.4 落地 API smoke 用例：/user/me、资料更新、上传、IM room+发消息、支付 prepay+查单
- [x] 2.5 落地 API regression 用例：错误验证码、无 token、支付回调验签失败/金额不一致、IM 参数校验与频控分支

## 3. UI 自动化框架落地

- [x] 3.1 集成 Playwright + pytest 并固定使用 Chrome 通道（headless 可配置）
- [x] 3.2 建立 POM/组件层封装（登录、首页/需求、聊天、收银台、上传组件）
- [x] 3.3 实现 UI 稳定性能力（统一等待策略、失败截图、可选 trace/控制台/网络摘要）
- [x] 3.4 落地 UI smoke 用例：登录→资料→头像上传
- [x] 3.5 落地 UI smoke 用例：需求入口→发起沟通→聊天收发消息（含 SSE 可观测断言）
- [x] 3.6 落地 UI smoke 用例：打开收银台→轮询查单→支付成功展示

## 4. 环境与数据治理

- [x] 4.1 提供依赖服务与后端就绪检查脚本（MySQL/Redis/MinIO、后端 8080，前端 5173 可选）
- [x] 4.2 复用 `sqlDoc/seed_dev_data.sql` 形成稳定测试基线，并在测试侧封装可引用的种子账号与数据定位
- [x] 4.3 提供测试数据创建与清理策略（用例级 teardown、幂等键/唯一前缀、避免数据污染）
- [x] 4.4 落地“验证码可测”方案（仅测试环境启用，不依赖外部短信；支持自动化获取 code）
- [x] 4.5 落地“支付回调可模拟”方案（构造签名参数调用回调接口，验证状态机幂等与业务解锁）

## 5. 报告与质量门禁

- [x] 5.1 定义并落地 smoke/regression、ui/api/e2e markers 与执行矩阵（PR/定时/发布前）
- [x] 5.2 输出并归档测试产物（JUnit XML；UI 失败截图/可选 trace）且与用例关联
- [x] 5.3 引入可配置的重试与 flaky 治理策略（仅对可重试错误，保留重试日志）

## 6. 主流程端到端用例（黄金链路）

- [x] 6.1 实现 E2E：学生创建/选择需求→教师发起沟通→双方实时收发→已读 ACK 与未读一致性
- [x] 6.2 实现 E2E：生成中介费订单→统一下单→收银台轮询→模拟回调→支付成功事件驱动解锁→聊天系统消息验证
