# 项目说明（OpenSpec）

## 1. 项目概览

- 项目目标：实现“家教直聘（BOSS直聘式）”MVP：学生/家长发布需求，老师浏览需求并直接开聊，完成沟通闭环
- 仓库形态：前后端同仓（monorepo），后端为 Maven 多模块 Spring Boot 工程，前端为 Vue3 + Vite 工程

相关规划可参考：[plan.md](file:///Users/bytedance/lyp/project/huoyue/ai_platform/.trae/documents/plan.md)

## 2. 目录结构（关键模块）

- 前端：`ai-tutor-web/`（Vue3 + Vite + TypeScript + Pinia）
- 后端聚合启动：`ai-tutor-starter/`（推荐启动入口）
- 家教业务服务：`tutor-appointment-service/`
- IM 服务：`videoCall-IM-service/`
- 公共依赖：`ai-tutor-common/`
- MQ 相关：`ai-tutor-mq/`
- 数据库初始化：`sqlDoc/huoyue.sql`

## 3. 技术栈

### 3.1 前端

- 语言与框架：TypeScript、Vue 3、Vue Router、Pinia
- 构建与开发：Vite
- HTTP：Axios（统一封装 Bearer Token）
- 质量与测试：ESLint（flat config）、vue-tsc 类型检查、Vitest + MSW（mock）

前端命令（在 `ai-tutor-web/` 下）：

- `npm run dev`：本地开发（默认端口 5173）
- `npm run build`：构建（包含 `vue-tsc -b`）
- `npm run lint`：lint
- `npm run typecheck`：类型检查
- `npm run test`：单测

### 3.2 后端

- 语言与框架：Java 21、Spring Boot 3.2.x
- ORM/数据访问：MyBatis、MyBatis-Plus
- 数据库：MySQL 8.x（或兼容版本）
- 缓存：Redis（lettuce）
- 鉴权：JWT（拦截器统一鉴权）
- API 文档：springdoc-openapi（Swagger UI）
- 消息队列（可选）：RocketMQ（用于验证 MQ 相关逻辑）

后端命令（推荐使用 Maven Wrapper `./mvnw`）：

- 聚合启动（推荐，在 `ai-tutor-starter/` 下）：
  - `sh ../mvnw -am spring-boot:run`
- 或在仓库根目录显式指定 mainClass：
  - `sh ./mvnw -pl ai-tutor-starter -am spring-boot:run -Dspring-boot.run.mainClass=com.ai.tutor.TutorStartApplication`
- 运行测试：
  - `sh ./mvnw test`

默认地址：

- 后端：`http://localhost:8080`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

## 4. 本地联调与环境约定

### 4.1 端口与代理

- 后端默认端口：8080（见 `ai-tutor-starter/src/main/resources/application.yml`）
- 前端默认端口：5173（Vite）
- 前端代理规则：将 `/api` 与 `/user` 代理到 `http://localhost:8080`（见 `ai-tutor-web/vite.config.ts`）

### 4.2 数据库与初始化

- 默认数据库名：`ai_tutor`
- 初始化 SQL：`sqlDoc/huoyue.sql`

### 4.3 配置与密钥

- 本地 `application.yml` 里的账号/密码为开发默认值；提交代码时不要新增真实密钥、Token、私钥等敏感信息
- JWT Secret 支持环境变量覆盖（示例：`JWT_SECRET_PRIMARY`）

## 5. API 设计与实现约定

- REST 为主，路径前缀主要包括：
  - `/user/*`：登录/注册/资料相关
  - `/api/v1/*`：业务 API（如家长需求、老师服务等）
  - `/chat/*`：IM 相关
- 认证：统一使用 Bearer Token（前端在 Axios 封装层注入）
- 角色与准入：教师/学生端能力需做前后端双向约束（路由守卫 + 后端鉴权/拦截器校验）

## 6. 代码风格与协作约定

- 交流语言：默认中文（除非明确要求切换语言）
- 前端：
  - ESLint 作为主要风格约束；尽量遵循既有代码结构与命名方式
  - TypeScript 变量未使用时使用 `_` 前缀以通过 lint 规则
- 后端：
  - 以 Spring Boot 约定优于配置；优先复用已有的 controller/service/mapper 分层与返回体风格
- 变更方式：
  - 尽量修改现有文件以贴合既有模式，新增文件只在确实需要新模块/新能力时引入
- 提交与分支：
  - 当前仓库未发现强制的提交规范；建议采用简洁明确的提交信息（可选 Conventional Commits）

## 7. 质量门禁（在交付前的最低检查）

- 前端：`npm run lint`、`npm run typecheck`、`npm run test`
- 后端：`sh ./mvnw test`

