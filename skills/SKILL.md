---
name: "ai-platform-context"
description: "当需要分析、修改、排查、评审或扩展 ai-tutor-platform 仓库中的任何内容时使用。动手前先根据受影响的模块或文件区域，路由到 skills/references 中对应的项目背景文档。完成并验证代码变更后，向 skills/references/change-log.md 追加一条简短记录，并把新的仓库专属坑点写入 skills/references/gotchas.md。"
---

# AI 平台项目上下文

这个 skill 是 `ai-tutor-platform` 仓库的项目操作指南。

凡是任务会涉及本仓库的代码、配置、测试或规格说明，都应使用它。

## 核心规则

- 以代码为准。一些文档和 OpenSpec 说明可能落后于当前实现。
- 以 `references/product-strategy.md` 作为业务阶段、首页定位、增长优先级与路线图表述的准绳。
- 除非代码和运行时验证都证明已经上线，否则不要把路线图中的 AI 能力写成“已交付”。
- 如果任务会修改数据库 schema、迁移 SQL 或表结构，必须在同一轮里同步更新 `sqlDoc/` 下对应文件，包括相关迁移文件和 `sqlDoc/huoyue.sql`，并在最终回复中明确说明这次同步。
- 如果任务会修改数据库 schema、迁移 SQL 或表结构，也必须在同一轮里把变更同步到共享远程服务器 `111.228.20.88`。
- 远程数据库同步只有在变更真正应用到 `111.228.20.88`，并且目标表或字段被验证存在后，才算完成。
- 修改代码前，先识别受影响区域，并阅读对应背景文档：
  - `references/module-map.md`
  - `references/business-flows.md`
  - `references/gotchas.md`
- 如果已经知道受影响文件，执行 `scripts/changed-area-check.sh <paths...>`，决定该读哪些背景文档、该做哪些验证。
- 如果还不清楚影响区域，执行 `scripts/project-snapshot.sh`，并检查最近的路由、控制器、服务或模块 `pom.xml`。

## 背景路由

- 认证、身份、网关、Feign 身份透传：
  先读 `references/module-map.md` 和 `references/gotchas.md`
- 用户、资料、需求/职位、日程、资源上传：
  先读 `references/module-map.md`，再读 `references/business-flows.md`
- 聊天、实时通信、合作提案、联系方式解锁、聊天内退款：
  先读 `references/business-flows.md`，再读 `references/gotchas.md`
- 支付、收银台、退款回调、佣金/分账：
  读取 `references/business-flows.md`、`references/payment-remote-testing.md`、`references/testing-matrix.md` 和 `references/gotchas.md`
- 管理后台后端或管理后台前端：
  读取 `references/module-map.md` 和 `references/testing-matrix.md`
- Web 前端或小程序：
  先读 `references/module-map.md`，再读 `references/business-flows.md` 中对应流程
- 首页、落地页、品牌文案、增长表达、角色定位、产品规划或页面优化：
  先读 `references/product-strategy.md`，再读 `references/module-map.md`，最后读 `references/business-flows.md` 中对应流程
- 本地启动、远程测试、基础设施、Nacos、Docker、环境变量或运行时配置排查：
  先读 `references/commands.md`，再读 `references/runtime-config.md`，然后读 `references/payment-remote-testing.md`，最后读 `references/gotchas.md`
- 实时课堂、LiveKit、课堂准备页、课堂页、双端入会、远端音视频不可见/不可听：
  先读 `references/live-classroom-media.md`，再读 `references/testing-matrix.md` 和 `references/gotchas.md`
- 测试、QA 自动化、回归方案：
  读取 `references/testing-matrix.md`

## 默认工作流

1. 根据模块或变更文件路径给请求分类。
2. 在提出方案或动手前，先读匹配的背景文档。
3. 做出符合现有仓库模式的、最小但完整的改动。
4. 按 `references/testing-matrix.md` 运行最小但有意义的验证。
5. 如果学到了新的仓库专属规则、坑点或捷径，就更新：
   - `references/gotchas.md`
   - `references/commands.md`
   - `references/business-flows.md`
   - `references/testing-matrix.md`
6. 变更验证通过后，在 `references/change-log.md` 追加一条简洁的日期记录。

## 持续改进约定

在真实项目工作中使用这个 skill 时：

- 让 `SKILL.md` 保持简短；持续演化的细节放进 `references/`
- 优先补一条准确、锋利的说明，而不是大段重写
- 只记录仓库专属知识，不写通用框架常识
- 如果旧文档有误但这轮没修，就把不一致记录到 `references/gotchas.md`
- 如果某个命令或路径查找在两次以上任务中重复出现，就把它提升到 `references/commands.md` 或封装成脚本

## 参考文档

- 仓库结构与模块归属提示：
  `references/module-map.md`
- 产品与技术流程图：
  `references/business-flows.md`
- 产品阶段、增长优先级、首页定位与路线图表达：
  `references/product-strategy.md`
- 本地命令与启动流程：
  `references/commands.md`
- 运行环境、配置导入链与 Nacos 查询：
  `references/runtime-config.md`
- 支付回调拓扑、共享测试服务器与线上验证流程：
  `references/payment-remote-testing.md`
- 不同变更类型的验证建议：
  `references/testing-matrix.md`
- 实时课堂线上媒体链路、LiveKit 端口要求和远程验证方法：
  `references/live-classroom-media.md`
- 已知陷阱、过时文档与跨模块注意事项：
  `references/gotchas.md`
- 这个 skill 的持续维护记录：
  `references/change-log.md`

## 脚本

- `scripts/project-snapshot.sh`
  输出仓库主要模块与入口文件的紧凑概览
- `scripts/changed-area-check.sh <paths...>`
  根据变更文件路径推断应该阅读的背景文档和验证重点
