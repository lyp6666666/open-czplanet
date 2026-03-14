## ADDED Requirements

### Requirement: 用例分层与执行集管理（smoke/regression）
系统 MUST 支持将用例分层为 smoke 与 regression，并提供一致的执行入口以便在本地与 CI 按需运行不同层级。

#### Scenario: PR 门禁仅执行 smoke 用例集
- **WHEN** CI 以门禁模式执行测试并选择 smoke 集合
- **THEN** 测试系统 MUST 仅执行 smoke 用例并输出明确的通过/失败结果

### Requirement: 标准化测试结果输出（JUnit XML）
系统 MUST 生成标准化测试结果文件（JUnit XML），用于 CI 平台展示、趋势统计与失败用例检索。

#### Scenario: 执行测试后生成 JUnit XML 文件
- **WHEN** 执行任意测试集合（smoke 或 regression）
- **THEN** 测试运行器 MUST 产出 JUnit XML 文件且包含用例名称、耗时与失败信息

### Requirement: UI 失败产物归档（截图/可选 trace）
系统 MUST 在 UI 用例失败时生成并归档失败产物（至少截图；可选 trace），并确保产物与失败用例可关联定位。

#### Scenario: CI 中可定位到失败用例的截图文件
- **WHEN** UI 用例在 CI 执行失败
- **THEN** CI 产物中 MUST 包含对应截图文件且可由用例名称或用例 ID 定位

### Requirement: 重试与不稳定治理策略
系统 MUST 提供对不稳定用例的治理能力，包括：对特定错误进行有限次数重试、对 flaky 用例打标与隔离执行，并确保门禁策略对 flaky 的处理可配置。

#### Scenario: 对临时性失败进行有限次数重试
- **WHEN** 用例因网络抖动或临时性资源问题失败且满足可重试条件
- **THEN** 测试系统 MUST 在限定次数内重试并输出最终判定结果与重试日志
