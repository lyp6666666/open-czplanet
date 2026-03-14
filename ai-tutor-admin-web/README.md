# ai-tutor-admin-web（管理端前端）

## 环境要求

- Node.js >= 20
- npm >= 9

## 安装依赖

```bash
cd /Users/bytedance/lyp/project/huoyue/ai_platform/ai-tutor-admin-web
npm install
```

## 启动开发环境

先启动后端（聚合入口）：

```bash
cd /Users/bytedance/lyp/project/huoyue/ai_platform
./mvnw -pl ai-tutor-starter -am spring-boot:run
```

再启动管理端前端：

```bash
cd /Users/bytedance/lyp/project/huoyue/ai_platform/ai-tutor-admin-web
npm run dev -- --host 0.0.0.0 --port 5174
```

浏览器访问：

- http://localhost:5174/

默认通过 Vite 代理把 `/api/*` 转发到后端 `http://localhost:8080`（见 `vite.config.ts`）。

