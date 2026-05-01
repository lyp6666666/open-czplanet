# QA Automation

本模块用于在同一仓库内落地 UI 自动化（Playwright + Chrome）与接口自动化（pytest + requests），覆盖登录/资料/上传、IM 聊天、支付收银台等主流程。

## 目录

- `core/`：配置、HTTP 客户端、鉴权与通用断言
- `api/`：领域 API clients
- `ui/`：Playwright 页面对象（POM）
- `tests/api`：接口用例
- `tests/ui`：UI 用例
- `tests/e2e`：端到端黄金链路用例
- `scripts/`：执行脚本与环境就绪检查

## 环境准备

### 1) 启动后端（建议 qa profile）

需要启用 qa/test profile 才能使用以下测试辅助接口：
- `GET /internal/debug/sms-code`（获取验证码）
- `POST /internal/debug/payment/yungouos-sign`（生成回调签名）

示例：

```bash
export SPRING_PROFILES_ACTIVE=qa
./mvnw -pl ai-tutor-starter -am spring-boot:run
```

### 2) 启动前端（跑 UI 用例需要）

```bash
pnpm -C ai-tutor-web dev
```

### 3) 支付 mock（可选）

如需在本地跑支付相关 UI/API 用例，推荐使用 mock 模式避免外部依赖：

```bash
export PAYMENT_ENABLED=true
export PAYMENT_YUNGOUOS_BASE_URL=mock://
```

## 安装依赖

优先使用 Poetry：

```bash
cd qa/automation
poetry install
poetry run playwright install chromium
```

也支持直接 pip（自行维护锁定版本）：

```bash
pip install -r requirements.txt
playwright install chromium
```

如果当前机器没有全局 `pytest` / `poetry` 命令，至少需要先准备一个可执行环境，例如：

```bash
cd qa/automation
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
playwright install chromium
pytest --collect-only tests/api/test_course_lesson_smoke.py
```

如果 QA 环境未开放 `GET /internal/debug/sms-code`，可以改为手工验证码模式：

```bash
export QA_LOGIN_MODE=otp
export QA_SMS_CODE=<刚收到的验证码>
```

这样自动化会跳过短信读取接口，直接使用 `QA_SMS_CODE` 登录。

## 执行

```bash
cd qa/automation
./scripts/ensure_backend.sh
./scripts/run_smoke.sh
./scripts/run_regression.sh
```

### 资金链路 P0 门禁

先确认 117 测试机已导入 `sqlDoc/qa_seed_data.sql`，且服务以 `qa`/`test` profile 启动。

```bash
cd qa/automation
export QA_API_BASE_URL=http://117.72.111.39:18080
export SPRING_PROFILES_ACTIVE=qa
export PAYMENT_ENABLED=true
export QA_LOGIN_MODE=otp
export QA_FUNDS_TEACHER_USER_ID=910102
export QA_FUNDS_TEACHER_PHONE=18611721002
export QA_FUNDS_STUDENT_USER_ID=910002
export QA_FUNDS_STUDENT_PHONE=18611720002
export QA_FUNDS_DEMAND_ID=940002
export QA_FUNDS_BROKERAGE_ORDER_ID=980001
export QA_FUNDS_REFUND_REQUEST_ID=985001
pytest -m funds tests/api/test_funds_p0.py
```

如需覆盖后台退款详情校验，额外设置：

```bash
export QA_ADMIN_USERNAME=admin
export QA_ADMIN_PASSWORD=<测试机后台密码>
```

产物输出：
- `artifacts/junit-*.xml`
- `artifacts/ui/*.png`（UI 失败截图）

### 课程 / 课节动作自动化

新增的 API client / smoke 用例：

- `api/course_client.py`
- `api/schedule_client.py`
- `api/appointment_client.py`
- `api/live_client.py`
- `tests/api/test_course_lesson_smoke.py`

建议环境变量：

```bash
export QA_API_BASE_URL=http://117.72.111.39:18080
export QA_LOGIN_MODE=otp
export QA_SMS_CODE=<测试手机收到的验证码>
export QA_COURSE_SMOKE_COURSE_ID=982001
export QA_COURSE_SMOKE_TEACHER_USER_ID=910103
export QA_COURSE_SMOKE_TEACHER_PHONE=18611721003
export QA_COURSE_SMOKE_STUDENT_USER_ID=910003
export QA_COURSE_SMOKE_STUDENT_PHONE=18611720003
export QA_COURSE_SMOKE_ACCEPTED_EVENT_ID=983001
export QA_COURSE_SMOKE_COMPLETED_EVENT_ID=983002
export QA_COURSE_SMOKE_LIVE_SESSION_ID=984002
```

执行：

```bash
cd qa/automation
pytest tests/api/test_course_lesson_smoke.py
```

说明：

- 用例现在会先探测课程 seed 是否真实存在。
- 如果测试环境未导入 `982001 / 983001 / 983002 / 984002` 这套 QA 课程链路，测试会以 `skip` 收口，并明确提示是“环境缺 seed”，而不是报前端接口脚本失败。
- 2026-05-01 对 `http://117.72.111.39:18080` 的实测结果：
  - 本地后门账号 `29999999999 / 1886`、`19999999999 / 1668` 可正常登录；
  - 但它们仅有资料/支付联调数据，没有 `courses` / `schedule events`；
  - 课程 smoke 目标 seed `910103 / 910003 / 982001 / 983001 / 984002` 当前在 117 上未命中，`/user/me` 返回 `40400`，课程详情返回“课程不存在”。

## 常用环境变量

- `QA_API_BASE_URL`：后端地址（默认 http://localhost:8080）
- `QA_WEB_BASE_URL`：前端地址（默认 http://localhost:5173）
- `QA_HEADLESS`：是否无头（默认 true）
- `QA_LOGIN_MODE`：`otp` 或 `jwt`（默认 otp）
- `QA_JWT_SECRET`：当 `QA_LOGIN_MODE=jwt` 时用于直签 token
