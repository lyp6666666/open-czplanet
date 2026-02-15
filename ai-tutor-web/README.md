# ai-tutor-web（前端）

## 环境要求

- Node.js >= 20
- npm >= 9

## 安装依赖

```bash
cd /Users/bytedance/lyp/project/huoyue/ai_platform/ai-tutor-web
npm install
```

## 启动开发环境

```bash
npm run dev -- --host 0.0.0.0 --port 5173
```

浏览器访问：

- http://localhost:5173/

## 后端联调说明

默认通过 Vite 代理把 `/api/*` 转发到后端：

- 后端地址：`http://localhost:8080`
- 代理配置：`vite.config.ts`

因此：

- 后端不启动时，控制台可能出现 `proxy ECONNREFUSED`，属于正常现象
- 启动后端（监听 8080）后，首页会自动请求本仓库的未登录首页接口（`/api/v1/public/...`）

如需指定固定后端地址（绕过代理），可通过环境变量配置：

```bash
VITE_API_BASE_URL=http://localhost:8080 npm run dev -- --host 0.0.0.0 --port 5173
```

## 质量门禁

```bash
npm run typecheck
npm run lint
npm test
npm run build
```

