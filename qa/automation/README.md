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

## 执行

```bash
cd qa/automation
./scripts/ensure_backend.sh
./scripts/run_smoke.sh
./scripts/run_regression.sh
```

产物输出：
- `artifacts/junit-*.xml`
- `artifacts/ui/*.png`（UI 失败截图）

## 常用环境变量

- `QA_API_BASE_URL`：后端地址（默认 http://localhost:8080）
- `QA_WEB_BASE_URL`：前端地址（默认 http://localhost:5173）
- `QA_HEADLESS`：是否无头（默认 true）
- `QA_LOGIN_MODE`：`otp` 或 `jwt`（默认 otp）
- `QA_JWT_SECRET`：当 `QA_LOGIN_MODE=jwt` 时用于直签 token
