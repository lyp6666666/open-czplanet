# ai-tutor-admin 管理端接入说明

## 启动方式（统一入口）

管理端后端服务独立启动：

```bash
cd /Users/bytedance/lyp/project/huoyue/ai_platform/ai-tutor-admin
SERVER_PORT=18084 SPRING_PROFILES_ACTIVE=dev sh ../mvnw -am spring-boot:run
```

默认端口为 `18084`（以 [application.yml](file:///Users/bytedance/lyp/project/huoyue/ai_platform/ai-tutor-admin/src/main/resources/application.yml) 为准）。

## 访问入口（URL）

- 后端服务：`http://localhost:18084`
- 管理端前端：`http://localhost:5174/`（项目：`ai-tutor-admin-web`）
- Swagger UI：`http://localhost:18084/swagger-ui.html`
- OpenAPI JSON：`http://localhost:18084/v3/api-docs`
- 管理端 API 前缀：`/api/admin`

## 管理端前端启动（推荐）

```bash
cd /Users/bytedance/lyp/project/huoyue/ai_platform/ai-tutor-admin-web
npm install
npm run dev -- --host 0.0.0.0 --port 5174
```

## 默认账号

管理员表结构在 `sqlDoc/huoyue.sql`，默认管理员账号数据在 `sqlDoc/seed_dev_data.sql`。

- 用户名/密码：`admin` / `huoyuejiajiao`

如果管理员登录返回 `Table 'ai_tutor.sys_admin_user' doesn't exist`，说明你本地库还没有导入最新的 `huoyue.sql`，需要导入后重试。

## 鉴权方式

- 登录接口返回 `token`
- 后续访问管理端接口时，在请求头携带：
  - `Authorization: Bearer <token>`

登录接口：
- `POST /api/admin/auth/login`

## 常用接口

### 仪表盘

- `GET /api/admin/dashboard/stats`

### 学生需求（作业贴）审核

- `GET /api/admin/jobs/pending?page=1&size=10`
- `POST /api/admin/jobs/approve/{id}`
- `POST /api/admin/jobs/reject`（body：`{ "id": 1, "reason": "xxx" }`）

### 教师认证审核

- `GET /api/admin/verification/pending?page=1&size=10`
- `GET /api/admin/verification/details/{userId}`
- `POST /api/admin/verification/approve`（body：`{ "userId": 1, "type": "REALNAME" }` 或 `EDU`）
- `POST /api/admin/verification/reject`（body：`{ "userId": 1, "type": "EDU", "reason": "xxx" }`）

### 退款纠纷处理

- `GET /api/admin/refund/disputes?page=1&size=10`
- `GET /api/admin/refund/details/{orderId}`（返回订单信息与聊天记录）
- `POST /api/admin/refund/approve`（body：`{ "orderId": 1 }`）
- `POST /api/admin/refund/reject`（body：`{ "orderId": 1, "reason": "xxx" }`）

### 用户管理（教师/学生）

- `GET /api/admin/users/teachers?q=&page=1&size=10`
- `GET /api/admin/users/students?q=&page=1&size=10`
- `GET /api/admin/users/{id}`
- `POST /api/admin/users`（创建）
- `PUT /api/admin/users/{id}`（修改）
- `DELETE /api/admin/users/{id}`（删除：实际为拉黑并禁用资料）
