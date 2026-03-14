## Why

当前仓库虽具备较完整的业务闭环（登录资料、IM 实时沟通、支付解锁等），但缺少覆盖主流程的“可重复、可回归、可度量”的自动化测试体系，导致回归依赖人工、线上风险与交付成本偏高。需要在同一仓库内建立一套大厂级自动化测试模块，形成 UI + API 的分层覆盖与稳定的质量门禁。

## What Changes

- 在仓库内新增独立模块 `qa/automation/`（Python 为主），提供统一的测试工程骨架、配置分层与可执行入口
- 建立 UI 自动化（Playwright + pytest，Chrome 通道）覆盖核心用户旅程：登录/资料/上传、发需求→发起沟通→实时消息/已读未读、支付收银台轮询→成功解锁
- 建立接口自动化（pytest + requests）覆盖关键 API 链路与风控分支：鉴权、资料更新、对象存储上传、IM 会话/消息、支付统一下单/查单/回调幂等
- 提供可复用的环境与数据装配能力（本地/CI）：依赖服务启动、测试账号与数据初始化、幂等清理与隔离策略
- 输出标准化测试结果与报表（JUnit XML/Allure 可选）、失败定位信息与可追溯的用例分层（smoke/regression）

## Capabilities

### New Capabilities

- `qa-automation-module`: 新增 `qa/automation/` 测试模块（Python 工程化、配置、命令入口与通用工具）
- `qa-ui-automation`: 基于 Playwright 的端到端 UI 自动化框架与主流程用例（POM/组件化封装、稳定性策略）
- `qa-api-automation`: 基于 requests 的接口自动化框架与主流程用例（会话、鉴权、支付闭环、IM 读写一致性）
- `qa-test-env-and-data`: 测试环境与数据治理（docker compose 依赖、fixture、数据隔离、幂等与清理）
- `qa-reporting-and-gates`: 测试分层、报表与质量门禁（smoke/regression、失败重试策略、CI 集成约定）

### Modified Capabilities

（无）

## Impact

- Codebase：新增 `qa/automation/` 目录与 Python 依赖（Playwright/pytest/requests 等）以及对应的配置与脚本入口
- Dependencies：引入浏览器运行时（Chrome）与 Playwright 驱动；本地/CI 需要可启动 MySQL/Redis/MinIO（以及可选 MQ）
- APIs：不新增业务 API；测试会覆盖并约束既有接口行为（鉴权、IM、支付、上传等）
- Dev/CI：增加可选的自动化测试门禁执行步骤（smoke 作为 PR gate，regression 作为定期/发布前 gate）
