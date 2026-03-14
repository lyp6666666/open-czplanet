## ADDED Requirements

### Requirement: 在同一仓库内提供独立的自动化测试模块
系统 MUST 在当前 Git 仓库内新增独立模块目录 `qa/automation/`，并将其作为 UI 自动化与接口自动化的统一入口与共享基础设施承载层。

#### Scenario: 仓库中存在标准化的测试模块结构
- **WHEN** 开发者拉取本仓库并进入仓库根目录
- **THEN** 仓库 MUST 存在 `qa/automation/` 目录，且其下包含用于组织测试的 `core/`、`api/`、`ui/`、`fixtures/`、`tests/`、`scripts/` 等子目录

### Requirement: 测试模块具备可复现的依赖管理与可执行入口
测试模块 MUST 提供可复现的 Python 依赖管理与统一的测试执行入口，确保在本地与 CI 环境可一致运行。

#### Scenario: 可在无业务代码改动的情况下安装依赖并执行测试
- **WHEN** 在 `qa/automation/` 目录执行依赖安装与测试命令
- **THEN** 测试框架 MUST 能完成依赖安装并可执行至少一个 smoke 用例集合

### Requirement: 配置分层与环境切换
测试模块 MUST 支持通过环境变量或配置文件切换测试目标环境，包括但不限于：后端 base URL、前端 base URL、是否启用 headless、默认浏览器通道、超时时间与重试策略。

#### Scenario: 通过环境变量切换目标后端地址
- **WHEN** 执行测试时提供环境变量（例如 `QA_API_BASE_URL`）
- **THEN** API 与 UI 用例 MUST 使用该地址作为请求目标，而不依赖硬编码

### Requirement: 用例分层与标记能力
测试模块 MUST 支持对用例进行分层与分组（例如 smoke/regression、ui/api/e2e），以便在 CI 按需选择执行集合。

#### Scenario: 基于 marker 选择性执行 smoke 用例
- **WHEN** 执行 pytest 并指定 `-m smoke`
- **THEN** 测试运行器 MUST 仅执行 smoke 用例集合并输出明确的执行统计
