# 领域数据表操作隔离设计（appointment + IM 并行）

- 日期：2026-03-21
- 项目：ai-platform
- 范围：`tutor-appointment-service`、`videoCall-IM-service`
- 目标：在不改变外部 API 的前提下，实现“每个领域的数据表操作隔离”，并通过强门禁（ArchUnit + CI）保证约束长期生效。

## 1. 已确认决策

1. 并行范围：`tutor-appointment-service` 与 `videoCall-IM-service` 同步推进。
2. 隔离强度：强约束，架构违规直接导致 CI 失败。
3. 领域划分沿用既有方案：
   - appointment：Identity / Marketplace / Booking
   - IM：Conversation / Deal
4. 对外兼容：外部 API 路径、入参、返回体、错误码语义保持 100% 不变。
5. 交付方式：每个模块一个大 PR（共两个 PR）。

## 2. 现状问题与改造目标

## 2.1 现状问题（当前代码可见）

1. 部分 `controller` 直接依赖 `mapper`，导致表现层直接操作数据表。
2. 模块内部跨领域访问主要靠“直接拿 mapper”完成，边界弱、耦合高。
3. 现有分层偏“controller/service/mapper”平铺结构，领域职责不清晰。
4. 缺少可执行的架构门禁，规则主要靠人审查，容易回退。

## 2.2 本次改造目标

1. 单表单写主责：每张表只有一个领域负责写入。
2. 控制器零 Mapper：`controller` 层不再直接依赖 `mapper`。
3. 领域依赖可证明：依赖方向由 ArchUnit 规则校验并在 CI 执行。
4. 行为零变化：对外接口行为与语义保持一致。

## 3. 目标架构

两模块统一采用模块内分领域分层结构：

```text
<module>
  ├─ <domain-a>
  │   ├─ api              // controller
  │   ├─ application      // 用例编排/事务边界
  │   ├─ domain           // 领域规则、领域接口
  │   └─ infrastructure   // mapper/持久化实现/外部客户端
  ├─ <domain-b>
  └─ shared               // 模块内通用（最小化）
```

统一约束（编译期依赖契约）：

1. `api` 仅依赖 `application`（以及本域 DTO/VO），不依赖 `domain/infrastructure/mapper`。
2. `application` 依赖 `domain` 与端口接口（port），不依赖任何 `mapper`。
3. `domain` 不依赖 `api/application/infrastructure`，只保留纯领域规则与模型。
4. `infrastructure` 依赖 `domain`（以及端口接口）来实现持久化与外部适配。
5. 运行时调用链可体现为：`api -> application -> domain(port) -> infrastructure(adapter)`。
6. 跨领域访问必须通过目标领域公开的应用服务/端口契约，不允许直连他域 `infrastructure` 与 `mapper`。

## 4. 表归属与写入主责

## 4.1 appointment（Identity / Marketplace / Booking）

- Identity：
  - `user`
  - `teacher_profile`
  - `student_profile`
  - `organization_profile`
  - `organization_account`
  - `user_settings`
- Marketplace：
  - `student_job_posting`
  - `teacher_job_posting`
  - `position_post`
  - `parent_favorite_tutor`
  - `tutor_favorite_demand`
- Booking：
  - `tutor_appointment`（及后续履约扩展表）

## 4.2 IM（Conversation / Deal）

- Conversation：
  - `room`
  - `message`
  - `room_read_state`
- Deal：
  - `tutor_application`
  - `collaboration_proposal`
  - `brokerage_order`
  - `application_brokerage_order`

规则：上述“写入主责”是强约束。非主责领域不得直接写该表。
其中“写”定义为：`insert / update / delete / soft-delete / upsert / status-change / batch-write`。

## 5. 领域依赖方向

## 5.1 appointment

1. `identity` 为基础域。
2. `marketplace` 与 `booking` 可依赖 `identity` 提供的领域接口。
3. `booking` 读取 `marketplace` 数据时必须走领域接口，不得直接使用 `marketplace` mapper。

## 5.2 IM

1. `deal -> conversation` 允许（例如读取会话基础信息）。
2. `conversation -> deal` 禁止直接依赖；若需要成交能力，使用 `DealAccessPolicy` 等领域接口或门面调用。

## 5.3 依赖规则矩阵（用于 ArchUnit 落地）

### appointment

| 调用方 | identity | marketplace | booking |
|---|---|---|---|
| identity | Allow（同域） | Forbidden（除契约外） | Forbidden（除契约外） |
| marketplace | Allow（仅契约） | Allow（同域） | Forbidden（除契约外） |
| booking | Allow（仅契约） | Allow（仅契约） | Allow（同域） |

说明：`Allow（仅契约）` 指只能依赖目标域的 `application/domain port`，禁止依赖目标域 `infrastructure/mapper`。

### IM

| 调用方 | conversation | deal |
|---|---|---|
| conversation | Allow（同域） | Forbidden（除契约外） |
| deal | Allow（仅契约） | Allow（同域） |

说明：IM 中 `conversation -> deal` 默认禁止，例外必须通过明确契约并在规则中白名单声明。

## 6. 组件拆分与迁移路径

## 6.1 appointment 迁移重点

优先处理高风险入口并保持行为不变：

1. `UserController`
2. `InternalUserFacadeController`
3. `ContactsController`
4. `OrganizationPublicController`

迁移动作：

1. 抽取 `IdentityApplicationService`、`MarketplaceQueryService`、`BookingQueryService`。
2. 控制器改为仅调用 application service。
3. 原 mapper 下沉到对应领域 `infrastructure`，通过仓储接口对上暴露能力。

## 6.2 IM 迁移重点

优先处理当前表层直连点：

1. `ChatController`
2. `InternalFacadeController`

并明确服务归属：

1. `conversation`：`ChatServiceImpl`、`ChatRoomServiceImpl`、`ChatReadServiceImpl`
2. `deal`：`TutorApplicationService`、`CollaborationProposalService`、`BrokerageOrderService`、`ContactUnlockService`

迁移动作：

1. 控制器改为 application service 依赖。
2. `conversation` 侧涉及成交判断逻辑，通过 `DealAccessPolicy` 访问，避免反向依赖 `deal.infrastructure`。

## 6.3 两模块统一执行步骤

1. 建立新分层骨架和桥接适配器（先可编译、可运行）。
2. 控制器依赖切换到 application service。
3. application 改为依赖领域接口与仓储端口；infrastructure 实现这些端口。
4. 清理残留 `controller/service -> mapper` 直连。
5. 最后收口命名与包结构，删除临时桥接代码。

## 7. 错误处理与兼容策略

1. 外部 API 的 `BaseResponse` 与 `ErrorCode` 语义保持不变。
2. `application` 层负责异常映射和边界转换。
3. `domain` 层只抛业务异常，不感知 HTTP 协议细节。
4. `infrastructure` 层将底层异常转换为业务可判定错误（依赖不可用、超时、数据缺失等）。

## 8. 测试与门禁方案

## 8.1 架构测试（新增，强制）

每个模块新增 `architecture/*BoundaryArchTest.java`，覆盖至少以下规则：

1. `controller` 不得依赖 `..mapper..`。
2. 分层依赖必须满足以下编译期约束：
   - `api` 不能依赖 `domain/infrastructure/mapper`
   - `application` 不能依赖 `mapper`
   - `domain` 不能依赖 `api/application/infrastructure`
   - `infrastructure` 不允许被 `api/application/domain` 反向依赖
3. appointment：
   - 按“appointment 依赖规则矩阵”逐项校验，尤其禁止跨域直连 `*.infrastructure` 与 `..mapper..`
4. IM：
   - 按“IM 依赖规则矩阵”逐项校验，尤其 `conversation` 不得依赖 `deal.infrastructure` 与 `deal.mapper`

## 8.2 行为回归测试（重点接口）

1. appointment：
   - `/user/me`
   - `/user/card`
   - `/internal/facade/v1/users/**`
2. IM：
   - `/chat/msg`
   - `/internal/facade/v1/brokerage/orders/{id}/payable`

要求：回归测试覆盖改造前已有行为，避免 API 语义漂移。

## 8.3 CI 门禁

CI 并行执行：

1. `./mvnw -pl tutor-appointment-service test`
2. `./mvnw -pl videoCall-IM-service test`

门禁策略：

1. 任一架构测试失败，直接阻断合并。
2. 任一重点回归测试失败，直接阻断合并。

## 9. PR 交付与验收

## 9.1 交付

1. PR-A：`tutor-appointment-service` 大 PR
2. PR-B：`videoCall-IM-service` 大 PR

每个 PR 必须附带：

1. 领域表写入主责清单
2. 架构规则说明与测试结果
3. 重点接口回归结果

## 9.2 验收标准

1. 两模块 `controller` 中不再出现 `mapper` 依赖。
2. ArchUnit 规则在 CI 中启用并稳定通过。
3. 对外 API 行为与语义无变更。
4. 表写入主责清晰，且未出现跨领域直写违规。

## 10. 风险与应对

1. 大 PR 冲突风险高：
   - 采取“先骨架、再迁移、最后清理”顺序，减少返工。
2. 回归面广：
   - 重点接口优先自动化回归，避免人工漏测。
3. 临时桥接代码遗留：
   - 设置清理清单，PR 合入前必须完成 bridge 清零。

## 11. 非目标

1. 本阶段不拆库。
2. 本阶段不改外部 API 契约。
3. 本阶段不引入分布式事务。
4. 本阶段不处理 `payment/admin` 全量重构，仅保持现有契约兼容。
