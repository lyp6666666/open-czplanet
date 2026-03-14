## Context

本仓库为前后端同仓（monorepo）的家教撮合平台，后端为 Maven 多模块 Spring Boot（聚合启动 `ai-tutor-starter`，默认 8080），前端为 Vue3 + Vite（默认 5173）。核心业务包含：手机号验证码登录、资料/头像上传（MinIO）、需求发布与浏览、1v1 IM（消息分页 + SSE 实时推送 + 已读未读）、支付统一下单与回调闭环（支付成功事件驱动业务解锁）。

当前缺少覆盖主流程的自动化测试体系，导致：
- 关键链路回归依赖人工，变更风险难以量化
- UI 与 API 的联动场景（IM 实时、支付回调）缺少可重复验证
- 缺少 smoke/regression 分层与 CI 质量门禁的标准化执行方式

本设计目标是在同一 Git 仓库内新增独立模块 `qa/automation/`（Python 为主），建立 UI 自动化 + 接口自动化的分层覆盖，并能在本地与 CI 稳定运行，覆盖三条主流程：
- 登录 + 资料 + 上传
- 发需求 + 沟通 IM（含实时消息与已读未读）
- 支付解锁闭环（收银台轮询 + 回调模拟 + 解锁验证）

## Goals / Non-Goals

**Goals:**
- 在仓库内提供独立、可复用、可扩展的 `qa/automation/` 自动化测试模块，具备清晰分层（api/ui/core/fixtures）与工程化约束（格式、命名、配置）
- UI 自动化基于 Playwright + pytest，执行浏览器固定为 Chrome（Playwright channel=chrome），覆盖关键用户旅程与核心断言点
- 接口自动化基于 pytest + requests，覆盖关键接口链路与典型异常分支，并与 UI 用例共享鉴权、数据装配与断言工具
- 提供可重复执行的环境与数据治理方案：依赖服务启动方式、测试账号与初始化数据来源、数据隔离/幂等清理策略
- 提供质量门禁能力：smoke/regression 分层、标准化输出（JUnit XML）与失败定位信息（截图/网络日志/请求响应摘要）

**Non-Goals:**
- 不做全量覆盖（例如所有页面/所有接口的穷举），优先保障“主流程可用性”与“核心写路径正确性”
- 不做性能压测、混沌测试、安全渗透测试等非功能测试（后续可扩展）
- 不做多浏览器矩阵（默认只跑 Chrome；跨浏览器可作为增量能力）
- 不引入外部真实支付/短信依赖作为稳定性前提（CI 必须可离线或可控）

## Decisions

### 1) 新增模块位置与工程化形态：`qa/automation/` 独立 Python 项目

**决策：**
- 在仓库根目录新增 `qa/automation/`，作为独立 Python 项目（与 Java/Node 工程解耦）
- 以 `pytest` 作为统一测试执行器；UI 与 API 均以 pytest 用例形态交付
- 依赖管理采用 `pyproject.toml + poetry.lock`（可替换为企业内部制品源），确保可复现安装与版本锁定

**原因：**
- 测试体系与业务工程解耦，便于独立迭代与 CI 调度
- pytest 生态成熟，便于分层、fixture 复用、标记分组与报告对接

**备选：**
- requirements.txt + pip：入门更快但版本锁定与依赖治理能力弱
- 仅用 Playwright（含 API）一把梭：UI/API 同工具链但对接口层抽象与断言复用不如 requests 体系直观

### 2) 目录分层与职责边界（可维护性优先）

**决策：**
`qa/automation/` 内采用明确分层：
- `core/`：配置加载、日志、通用断言、重试/等待、HTTP/SSE 工具、时间/随机/ID 工具
- `api/`：requests 客户端封装（Session、BaseResponse 解析、鉴权注入），按领域拆分 client（user/assets/chat/payment/jobs）
- `ui/`：Playwright 封装（页面对象 POM/组件对象），统一选择器策略与通用动作（登录、导航、表单、上传）
- `fixtures/`：pytest fixtures（base_url、auth_token、seed 用户、数据创建与清理、截图与 trace）
- `tests/`：用例分层（`tests/api/*`、`tests/ui/*`、`tests/e2e/*`），并用 marker 标注 smoke/regression
- `scripts/`：本地/CI 启动编排（启动依赖、健康检查、跑 smoke/regression）

**原因：**
- 大部分不稳定来自“用例直接堆实现细节”，分层可让用例只表达业务意图，减少选择器与 HTTP 细节耦合

### 3) UI 自动化：Playwright + pytest，Chrome 通道，稳定性策略前置

**决策：**
- Playwright 使用 `channel=chrome`，确保贴近真实用户浏览器
- 选择器优先级：`data-testid`（若现有页面无则逐步补齐） > role/label > 文本/结构选择器（最后手段）
- 所有 UI 用例默认开启失败截图、trace（按需保留）、网络请求摘要（仅失败收集）
- 异步场景采用“显式等待 + 业务可观测点”：
  - SSE：以“收到 ready 事件 + 收到 message 事件”为可观测点，而非固定 sleep
  - 支付：以“轮询接口状态 SUCCESS + 页面状态变更”为可观测点

**原因：**
- 端到端用例最贵也最脆弱，必须把稳定性策略作为框架能力而非用例里散落的 sleep

**备选：**
- Selenium：生态成熟但维护成本更高、等待与并发能力弱于 Playwright

### 4) API 自动化：requests Session + 领域 client，统一处理 BaseResponse 与鉴权

**决策：**
- 所有 API 调用经由 `requests.Session`，统一注入：
  - `base_url`、超时、重试策略（仅幂等 GET/查询类）
  - `Authorization: Bearer <token>`（登录后自动写入 session header）
  - 统一解析后端 `BaseResponse`（code/message/data），并提供断言工具（code==0、错误码校验）
- 领域 client 按模块拆分，避免一堆散落的 url 字符串

**原因：**
- 让接口用例只关心“业务输入输出”，把协议细节收敛在 client 层，提高可维护性

### 5) 测试数据策略：优先复用现有种子数据 + 增量按需创建

**决策：**
- 本地/CI 统一使用仓库提供的初始化 SQL（`sqlDoc/seed_dev_data.sql`），获得稳定账号与业务数据基线
- 用例中新增数据（如新需求贴、会话消息）必须可追踪并可清理：
  - 优先通过业务 API 创建并记录 id；结束后按 id 清理（或通过状态标记/软删）
  - 若缺少清理 API，采用“测试专用标识字段/前缀” + 定时清理策略（后续补齐 admin/clean API）

**原因：**
- 完全依赖随机创建会导致数据不稳定与用例间串扰；完全依赖固定数据会导致无法覆盖写路径

### 6) 登录验证码（OTP）可测性：引入 QA 模式避免外部短信依赖

**决策：**
- 自动化测试必须不依赖外部短信服务（Spug）；为此引入“QA/Test 模式”：
  - 仅在 `qa`/`test` profile 或显式环境变量开启时，提供可获取验证码的能力（例如新增仅内网可用的调试接口，或允许 `/user/sendcode` 在 QA 模式返回 code）
  - 生产环境保持现有行为，不暴露验证码

**原因：**
- 当前实现发送验证码会依赖短信 token 与外部网络，CI 不可控；同时 `/user/sendcode` 不返回 code，无法做黑盒自动化登录

**备选：**
- 直接在测试侧用 JWT secret 伪造 token：实现简单但安全边界更弱、对密钥管理要求更高，不作为默认方案

### 7) 支付闭环可测性：使用“本地可签名回调模拟”替代真实渠道

**决策：**
- 复用现有 YunGouOS 回调验签逻辑：测试侧使用已知 `appKey` 构造签名参数，调用 `/payment/notify/yungouos` 触发状态迁移与事件链路
- 支付用例断言以“支付单状态 SUCCESS + 业务侧解锁可观测结果（系统消息/状态字段）”为准

**原因：**
- 不接真实支付渠道即可验证闭环关键逻辑（验签、幂等、事件驱动）

### 8) 报告与门禁：smoke 作为 PR 门禁，regression 作为发布/定时门禁

**决策：**
- pytest markers：
  - `@pytest.mark.smoke`：5-10 分钟内可跑完的最小闭环（核心链路）
  - `@pytest.mark.regression`：更全覆盖（含边界与异常分支）
  - `@pytest.mark.ui` / `@pytest.mark.api`：执行维度
- 默认输出 JUnit XML；Allure 作为可选增强（不作为强依赖）
- CI 中按需分阶段：先跑 API smoke，再跑 UI smoke；回归在 nightly 或发布前跑全量

## Risks / Trade-offs

- [OTP 测试模式引入安全风险] → 仅在 qa/test profile 生效 + 环境隔离 + 默认关闭 + 明确代码扫描规则禁止生产开启
- [UI 用例天然易抖动] → 选择器策略升级（testid）、统一等待与重试、trace/截图定位、对异步事件用可观测点断言
- [IM 实时与 MQ 依赖导致不稳定] → E2E 优先验证 SSE 端到端（消息发送→推送→ACK）；MQ 作为可选路径并提供降级断言
- [测试数据互相污染] → 固定种子数据 + 用例创建数据带前缀/标签 + teardown 清理 + 幂等重试策略
- [环境差异（本地/CI/不同 OS）] → 统一用 docker compose 启动依赖，健康检查后再执行；Playwright 固定浏览器版本与启动参数

## Migration Plan

1) 先落地 `qa/automation/` 工程骨架与 API smoke（鉴权/资料/上传/支付查单）作为最小可用
2) 落地 UI smoke（登录→首页→需求→聊天→收银台）并打通 CI 可执行
3) 引入 QA 模式解决 OTP 获取，保证 CI 可离线/可控运行
4) 扩展 regression 覆盖与边界用例，完善报表与失败定位能力

回滚策略：
- 若自动化在短期内影响 CI 稳定性，可先仅保留 API smoke 作为门禁，UI 用例转为 nightly；模块本身可独立迭代，不影响业务发布

## Open Questions

- CI 平台与运行环境：GitHub Actions / GitLab CI / Jenkins？是否有固定 Runner（含 Docker、可运行浏览器）？
- OTP 的 QA 模式采取哪种形式更合适：新增调试接口 vs QA 模式回传 code？是否需要 IP 白名单/内网网关保护？
- E2E 是否要求“全自动启动前端与后端”（测试框架编排）还是“外部已启动后直接跑”（更简单）？
- 对 MQ 的要求：E2E 是否必须覆盖 RocketMQ 推送链路，还是仅保证 SSE 端到端即可？
