---
name: "ai-platform-git-commit-workflow"
description: "在 ai-platform 仓库中进行任何 Git 提交、推送、分支合并、创建 MR/PR、发布相关操作时必须使用。尤其是用户说“提交”“commit”“push”“推送”“合并”“MR”“PR”“发布到 master/生产”时，先遵循本 skill，再执行 Git 命令。"
---

# AI Platform Git 提交流程

这个 skill 约束本仓库的 Git 提交、推送、MR/PR 与生产发布流程。任何涉及提交代码或推动分支的操作都要遵循它。

## 核心原则

- 每次提交的 commit subject 必须包含中文，确保 `git log --oneline` 也能直接看懂本次改动。
- 英文 Conventional Commit 只能作为可选前缀，例如 `feat(web): 新增客服浮窗配置`；不能只写英文 subject。
- commit body 和最终回复也必须用中文说明本次改动、验证和风险。
- 日常开发默认提交到 `dev` 分支，并推送到 `origin/dev`。
- `master` 是生产发布分支。`master` 更新会触发自动部署到生产服务器。
- 默认不要直接向 `master` 提交或推送。需要通过 MR/PR 从 `dev` 合并到 `master`。
- 每次提交前必须先更新远程信息并拉取目标分支最新代码，默认执行 `git fetch origin` 和 `git pull --ff-only origin dev`，确认没有落后和冲突后再 stage/commit。
- 推送前只 stage 本次任务相关文件，不能顺手提交用户已有改动、临时截图、构建产物、日志或无关文件。
- 不得用 `git reset --hard`、`git checkout --`、`git clean` 等破坏性命令处理用户改动，除非用户明确要求。
- 发现工作区有无关改动时，保留它们，并在最终说明中明确“未提交/未触碰”。
- 涉及密钥、Token、私钥、生产密码、真实用户隐私数据时，必须停止提交并提醒用户处理。

## 标准流程

1. 查看当前分支、远端和工作区状态：

```bash
git branch --show-current
git remote -v
git status --short
```

2. 确认目标分支：

- 如果当前不在 `dev`，默认先询问用户是否切到 `dev`，或创建 `codex/...` 工作分支。
- 如果用户明确要求直接提交当前分支，可以按用户要求执行，但最终说明要标明当前分支。
- 如果用户要求操作 `master`，必须提醒：`master` 会触发生产自动部署；除非用户再次明确确认，否则不要直接推送。

3. 提交前更新远程信息并拉取最新代码：

```bash
git fetch origin
git pull --ff-only origin dev
git rev-list --left-right --count HEAD...origin/dev
```

- 默认目标是 `origin/dev`；如果用户明确指定其它目标分支，把命令里的 `dev` 替换为目标分支。
- `git pull --ff-only` 失败时，先阅读冲突或非快进原因，不能跳过后继续提交。
- `git rev-list --left-right --count` 应确认本地没有落后远端；如有落后，先完成同步。

4. 复核变更范围：

```bash
git diff -- <本次相关文件>
git diff --stat
```

- 只 stage 本次任务相关文件；如果用户明确要求“提交当前所有本地改动”，也要先排除临时截图、缓存、构建产物、日志和密钥文件。
- 对新增文件要确认不是临时文件、截图、`dist/`、`node_modules/`、日志、缓存或测试残留。

5. 提交前验证：

- 前端 Web 变更：至少运行 `npm --prefix ai-tutor-web run typecheck`，有 UI 交互时尽量做浏览器/Playwright 验证。
- 管理端前端变更：至少运行 `npm --prefix ai-tutor-admin-web run typecheck`。
- 小程序变更：至少运行对应 `npm` 类型检查/构建命令；若项目脚本缺失，要说明无法运行的原因。
- Java 后端变更：优先运行受影响模块测试，例如 `./mvnw -pl <module> -Dtest=<TestName> test`；大范围变更再考虑 `./mvnw test`。
- 文档/skill 变更：至少检查文件内容、路径和 Markdown/frontmatter 格式；如无可执行测试，在最终说明中写清楚。

6. 定向 stage：

```bash
git add <本次相关文件1> <本次相关文件2>
git status --short
```

确认 staged 文件只包含本次变更。

7. 写提交信息。

推荐格式：

```text
<type>(<scope>): <中文短说明>

中文说明：
- 做了什么：
- 为什么：
- 验证：
- 风险/注意：
```

示例：

```text
feat(web): 新增客服浮窗

中文说明：
- 做了什么：新增用户端客服浮窗，支持微信/QQ 展示、复制和拖动。
- 为什么：让大多数页面可以快速找到人工客服联系方式。
- 验证：npm run typecheck；相关文件 eslint；本地 Playwright 交互检查。
- 风险/注意：联系方式目前来自前端配置，后续可迁移到后台配置。
```

常用 type：

- `feat`：新增功能
- `fix`：修复问题
- `refactor`：重构，不改变行为
- `test`：测试
- `docs`：文档
- `chore`：工程配置、脚本、杂项
- `style`：纯格式或样式调整

8. commit：

```bash
git commit -m "<subject>" -m "<中文说明正文>"
```

不要进入交互式编辑器写 commit message。

9. 推送：

```bash
git push origin dev
```

- 默认推送 `dev`。
- 如果在特性分支，推送当前分支，并说明后续应向 `dev` 或 `master` 创建 MR/PR。
- 推送失败时，先读错误信息；不要盲目 force push。需要 `--force-with-lease` 时必须说明原因并获得用户确认。

10. MR/PR 到 `master`：

- 日常发布路径是：`dev` 提交并推送 -> 创建 MR/PR -> 合并到 `master` -> 自动部署生产。
- 创建 MR/PR 前，说明 `master` 合并会触发生产部署。
- MR/PR 描述必须包含中文：
  - 变更内容
  - 验证方式
  - 风险与回滚建议
  - 是否涉及数据库/配置/生产环境

## 生产发布注意事项

- `master` 会触发 GitHub Actions 自动部署生产，具体链路以 `.github/workflows/deploy-prod.yml` 为准。
- 生产应用目录是 `111.228.20.88:/opt/ai-platform-prod`。
- 除非用户明确要求，不要手动登录生产机改代码绕过 Git 流程。
- 涉及数据库 schema、Nacos、支付、退款、登录鉴权、网关、安全策略的变更，在 MR/PR 说明中必须突出风险和回滚方式。

## 最终回复要求

提交/推送完成后，用中文简洁说明：

- 提交哈希和提交信息
- 推送到哪个远端分支
- 已运行的验证命令
- 哪些无关改动未提交
- 如果下一步需要 MR/PR，提醒从 `dev` 合并到 `master`，并说明合并后会触发生产部署

如果实际执行了 stage、commit、push，按 Codex 桌面协议在最终回复中附带对应 Git directive。
