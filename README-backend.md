# 后端启动说明

本项目后端为 Maven 多模块 Spring Boot 工程，推荐启动聚合入口 `ai-tutor-starter`（会把 `tutor-appointment-service` 与 `videoCall-IM-service` 一起拉起到同一个进程里）。

## 1. 环境依赖

- JDK：17+
- MySQL：8.x（或兼容版本）
- Redis：6.x（或兼容版本）
- （可选）RocketMQ：如果你要验证 MQ 相关逻辑，需要本地 `name-server`（默认 `127.0.0.1:9876`）

## 2. 初始化数据库

1) 创建数据库（默认库名 `ai_tutor`）：

```sql
CREATE DATABASE IF NOT EXISTS ai_tutor DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2) 导入初始化 SQL：

- SQL 文件：`sqlDoc/huoyue.sql`

> 具体账号、密码、连接串以各模块 `application.yml` 为准，你可以按自己本机环境修改。

## 2.1 数据库升级（已有旧库时必做）

如果你之前已创建过 `ai_tutor` 库且导入过旧版 SQL，需要执行以下增量变更（否则会出现“首页热门需求/需求广场接口 500 / Unknown column”一类错误）：

```sql
ALTER TABLE student_job_posting
  ADD COLUMN stage_code varchar(32) NULL COMMENT '授课学段：PRESCHOOL/PRIMARY/JUNIOR/SENIOR/OTHER',
  ADD COLUMN education_requirement varchar(32) NULL COMMENT '学历要求：TOP2/C985/C211/DOUBLE_FIRST_CLASS/FIRST_TIER/BACHELOR/OVERSEAS/QS50 等';

CREATE TABLE IF NOT EXISTS tutor_favorite_demand (
  id bigint(20) NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  tutor_id bigint(20) NOT NULL COMMENT '教师用户ID（user.id）',
  demand_id bigint(20) NOT NULL COMMENT '需求贴ID（student_job_posting.id）',
  create_time datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tutor_demand (tutor_id, demand_id),
  KEY idx_tutor_id (tutor_id),
  KEY idx_demand_id (demand_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 3. 配置说明（按需修改）

默认配置文件：

- 聚合启动：`ai-tutor-starter/src/main/resources/application.yml`
- 单模块启动：`tutor-appointment-service/src/main/resources/application.yml`
- IM 模块：`videoCall-IM-service/src/main/resources/application.yml`

你通常需要关注：

- `spring.datasource.*`（MySQL 连接）
- `spring.data.redis.*`（Redis 连接）
- `jwt.secrets`（可用环境变量 `JWT_SECRET_PRIMARY` 覆盖）
- `server.port`（聚合启动默认 `8080`）

## 4. 启动方式（推荐：聚合启动）

推荐在模块目录执行（避免父工程缺少 mainClass 导致的 `Unable to find a suitable main class` 报错）：

```bash
cd /Users/bytedance/lyp/project/huoyue/ai_platform/ai-tutor-starter
sh ../mvnw -am spring-boot:run
```

如果你希望在项目根目录执行，也可以显式指定 mainClass：

```bash
cd /Users/bytedance/lyp/project/huoyue/ai_platform
sh ./mvnw -pl ai-tutor-starter -am spring-boot:run -Dspring-boot.run.mainClass=com.ai.tutor.TutorStartApplication
```

启动成功后默认端口：

- 后端：`http://localhost:8080`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

## 5. 启动方式（备选：单模块启动）

如果你只想启动家教业务模块（不聚合 IM 模块）：

```bash
cd /Users/bytedance/lyp/project/huoyue/ai_platform
sh ./mvnw -pl tutor-appointment-service -am spring-boot:run
```

> 注意：`tutor-appointment-service` 默认未显式指定 `server.port`，会使用 Spring Boot 默认端口 8080。若你同时启动多个模块，请给不同进程指定不同端口，例如：
>
> ```bash
> sh ./mvnw -pl tutor-appointment-service -am spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
> ```

## 6. 前后端联调

前端默认把 `/api/*` 代理到 `http://localhost:8080`（见 `ai-tutor-web/vite.config.ts`）。

因此联调顺序建议：

1) 启动后端（8080）
2) 启动前端（5173），打开首页 `http://localhost:5173/`
