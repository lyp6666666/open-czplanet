# 后端启动说明

本项目后端为 Maven 多模块 Spring Boot 工程，推荐启动聚合入口 `ai-tutor-starter`（会把 `tutor-appointment-service` 与 `videoCall-IM-service` 一起拉起到同一个进程里）。

## 0. Docker 安装（macOS）

如果你本机执行 `docker` 提示 `command not found`，先安装 Docker Desktop：

```bash
brew install --cask docker
open -a Docker
```

首次启动 Docker Desktop 可能会弹窗要求授权/安装辅助组件，按提示完成即可。完成后用下面命令确认 Docker 已就绪：

```bash
docker info
docker compose version
```

> 如果 `docker info` 报 “daemon is not running”，通常是 Docker Desktop 还没启动完成，稍等一会儿再执行即可。

## 1. 环境依赖

- JDK：17+
- MySQL：8.x（或兼容版本）
- Redis：6.x（或兼容版本）
- （可选）RocketMQ：如果你要验证 MQ 相关逻辑，需要本地 `name-server`（默认 `127.0.0.1:9876`）
- Docker Desktop：用于一键启动本地依赖（MySQL/Redis/MinIO/RabbitMQ）

## 1.1 Docker 一键启动本地依赖（推荐）

项目内已提供 `docker compose` 配置，用于本地一键拉起：
- MySQL（`root/Aa123456`，自动创建数据库 `ai_tutor` 并导入 `sqlDoc/huoyue.sql`、`sqlDoc/seed_dev_data.sql`）
- Redis（`requirepass=123456`）
- MinIO（`minioadmin/minioadmin`，自动创建 bucket `ai-tutor-assets` 并设置 public read）
- RabbitMQ（管理台端口 `15672`）

启动命令：

```bash
cd /Users/bytedance/lyp/project/huoyue/ai_platform/Dockerfile
docker compose up -d
```

如果你在拉取镜像阶段遇到 `context deadline exceeded` / `connection timed out`（无法访问 Docker Hub），需要先在 Docker Desktop 配置网络：
- 公司网络：在 Docker Desktop Settings -> Proxies 配置 HTTP/HTTPS 代理（按公司要求）
- 家庭/公共网络：在 Docker Desktop Settings -> Docker Engine 配置 registry mirror（按你的网络环境选择可用镜像源）

本仓库默认已把 `docker-compose.yml` 的镜像地址切换到 `docker.m.daocloud.io`（Docker Hub 镜像加速域名），以避免部分网络环境无法直连 Docker Hub 导致的拉取超时。

配置完成后重启 Docker Desktop，再执行：

```bash
cd /Users/bytedance/lyp/project/huoyue/ai_platform/Dockerfile
docker compose pull
docker compose up -d
```

常用访问入口：
- MySQL：`localhost:3306`
- Redis：`localhost:6379`（密码 `123456`）
- MinIO API：`http://localhost:9000`，Console：`http://localhost:9001`（账号/密码 `minioadmin/minioadmin`）
- RabbitMQ Console：`http://localhost:15672`（默认 guest/guest）

如需清空本地数据（会删除 MySQL/Redis/MinIO 的本地卷数据）：

```bash
cd /Users/bytedance/lyp/project/huoyue/ai_platform/Dockerfile
docker compose down -v
```

## 2. 初始化数据库

1) 创建数据库（默认库名 `ai_tutor`）：

```sql
CREATE DATABASE IF NOT EXISTS ai_tutor DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2) 导入初始化 SQL：

- SQL 文件：`sqlDoc/huoyue.sql`

> 具体账号、密码、连接串以各模块 `application.yml` 为准，你可以按自己本机环境修改。

> 如果你使用上面的 Docker 一键启动：MySQL 容器会自动创建数据库并导入初始化 SQL；通常无需手动执行本节。

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

## 4.1 MinIO 资源准备（首次必做）

后端已默认启用 MinIO，并且首页 Banner 默认从 MinIO 读取。因此首次启动本地环境时，需要把前端内置的 `public/banners/*` 同步到 MinIO（只需执行一次）：

```bash
cd /Users/bytedance/lyp/project/huoyue/ai_platform
bash scripts/minio_sync_assets.sh
```

执行完成后，再打开前端首页，Banner 即可正常展示；头像上传也会直接写入 MinIO。

> 脚本默认通过 `host.docker.internal:9000` 访问本机 MinIO（适配 macOS Docker Desktop）。如果你在 Linux 上执行，可显式指定 `MINIO_ENDPOINT` 为你的可达地址（例如 `http://127.0.0.1:9000` 或 compose 网络内的 `http://minio:9000`）。

## 7. 常见问题

### 7.1 简历页上传头像报错：No static resource api/v1/assets/upload

如果后端日志出现：
`NoResourceFoundException: No static resource api/v1/assets/upload`

通常说明你当前运行的后端进程里没有注册上传接口（常见原因是后端没重启/起错了模块/起的是旧进程）。

用下面命令快速验证后端是否具备上传接口：

```bash
curl -sS http://localhost:8080/api/v1/assets/upload
```

预期返回类似：
`{"code":40100,"data":null,"message":"缺少 Authorization: Bearer token"}`

如果不是这个响应，请按顺序处理：
1) 确保你启动的是 `ai-tutor-starter`（推荐）或 `tutor-appointment-service`，而不是其他进程占用了 8080
2) 重启后端进程（IDEA/终端都可）
3) 再次执行上面的 curl 校验接口是否存在

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
