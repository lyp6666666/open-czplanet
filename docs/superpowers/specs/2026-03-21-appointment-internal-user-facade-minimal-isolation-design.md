# Appointment InternalUserFacade 最小隔离设计

- 日期：2026-03-21
- 项目：ai-platform
- 模块：`tutor-appointment-service`
- 目标：在不改变外部 API 行为的前提下，完成 `InternalUserFacadeController` 与 `mapper` 的直接依赖隔离，并以测试门禁验证改造有效。

## 1. 范围与非目标

### 1.1 本次范围（仅此）

1. `InternalUserFacadeController` 不再直接依赖任何 `*Mapper`。
2. 新增 `InternalUserFacadeService` / `InternalUserFacadeServiceImpl` 承接控制器原有 mapper 访问逻辑。
3. 统一内网 token 校验入口（复用已有 guard；若缺失则新增 `InternalTokenGuard`）。
4. 补齐并收紧对应测试门禁：
   - `InternalUserFacadeController` 委派测试
   - `AppointmentBoundaryArchTest` 覆盖 internal controller
5. 通过双层验收：
   - 目标测试通过
   - `./mvnw -pl tutor-appointment-service test` 全量通过

### 1.2 非目标（本次不做）

1. 不扩展到 `InternalAdminFacadeController`。
2. 不推进 appointment 全域 `api/application/domain/infrastructure` 完整分层约束。
3. 不修改任何外部 API 路径、入参、返回结构、错误码语义。
4. 不触及 `videoCall-IM-service`、`ai-tutor-admin`、`payment-service`。

## 2. 目标架构与依赖边界

### 2.1 组件职责

1. `InternalUserFacadeController`
   - 负责请求入口、参数接收、调用服务、包装 `BaseResponse`。
   - 不包含 mapper 字段、不包含 `.mapper.` 依赖。
2. `InternalUserFacadeService`（接口）
   - 定义 controller 所需用例方法（按现有 endpoint 对齐）。
3. `InternalUserFacadeServiceImpl`
   - 承载原 controller 的 mapper 查询/更新与响应对象组装。
4. `InternalTokenGuard`
   - 统一校验 `X-Internal-Token`，保持现有鉴权语义。

### 2.2 依赖方向

1. `controller -> InternalUserFacadeService`
2. `InternalUserFacadeServiceImpl -> mapper/model`
3. 约束：`controller` 不得依赖 `..mapper..`

## 3. 数据流与错误处理

### 3.1 请求处理链路

1. controller 接收请求
2. 执行 token 校验（guard）
3. 执行参数合法性校验（保持现有 `ThrowUtils.throwIf` 语义）
4. 委派至 `InternalUserFacadeService`
5. service 内部执行 mapper 访问与结果组装
6. controller 通过 `ResultUtils.success(...)` 返回

### 3.2 兼容性要求

1. endpoint 路径不变。
2. 入参命名与必填规则不变。
3. 返回结构和空值语义不变（包含 `null`/`false` 的既有行为）。
4. 业务错误码语义不变。

### 3.3 错误处理规则

1. 参数非法：保持 `ErrorCode.PARAMS_ERROR`。
2. 内网 token 非法：保持 `ErrorCode.NO_AUTH_ERROR`。
3. 不引入新的对外错误码。

## 4. 测试与门禁

### 4.1 目标测试（必须）

1. `InternalUserFacadeControllerTest`
   - 验证 controller 各 endpoint 仅委派 service。
   - 验证 service 调用参数正确。
2. `AppointmentBoundaryArchTest`
   - internal controller 纳入门禁覆盖。
   - 规则：controller 层不得依赖 `.mapper.`。

### 4.2 全量回归（必须）

执行：`./mvnw -pl tutor-appointment-service test`

要求：测试全通过；否则本次优化不视为完成。

## 5. 验收标准

满足以下条件即验收通过：

1. `InternalUserFacadeController` 代码中无 mapper import、无 mapper 注入字段。
2. 对外 API 行为无变化（路径、参数、返回、错误码语义一致）。
3. 目标测试通过。
4. `tutor-appointment-service` 模块全量测试通过。

## 6. 风险与应对

1. 风险：校验逻辑位置变动导致细微行为漂移。  
   应对：校验语义保持不变，仅做位置抽取，不更改判断条件。
2. 风险：门禁范围收紧后暴露历史遗留依赖。  
   应对：仅对本次目标 controller 收口；若出现额外违规，明确记录并后续分批处理。

