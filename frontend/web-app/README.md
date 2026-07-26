# HomeWork 用户端前端

本工程位于 HomeWork monorepo 的 `frontend/web-app`。接口和业务规则以
`backend/web/web-app` 为准，当前覆盖登录注册、首页、面试题库、认证题库、
最新 Hit、个人中心、消息和会员等用户端功能。

## 技术栈

- React 19、TypeScript、Vite
- React Router
- TanStack Query、Axios
- React Hook Form、Zod
- Tailwind CSS 4、Radix UI
- Vitest、Testing Library、MSW、Playwright

## 本地启动

要求 Node.js 20 和 pnpm 10。建议在仓库根目录安装依赖：

```bash
cd ../..
pnpm install
cp frontend/web-app/.env.example frontend/web-app/.env.local
pnpm dev:web
```

开发地址默认为 `http://127.0.0.1:5173`。Vite 会把 `/api` 请求代理到 `http://127.0.0.1:8080`，因此联调前需要先启动后端。

## 环境变量

`.env.example` 列出了全部前端配置：

- `VITE_API_BASE_URL`：API 基础路径，本地开发保持 `/api`
- `VITE_TURNSTILE_SITE_KEY`：Cloudflare Turnstile 站点密钥
- `VITE_OAUTH_ENABLED`：第三方登录总开关，首发保持 `false`
- Google、Apple、微信、QQ OAuth 的客户端标识和回调地址，仅在联调时配置

首发只开放邮箱登录和注册。OAuth 实现代码暂时保留，但总开关关闭；
后续真实联调通过后，再打开总开关并逐个配置对应平台，未配置的平台不会显示。

## 常用命令

```bash
pnpm --filter homework-web-app lint
pnpm --filter homework-web-app typecheck
pnpm --filter homework-web-app test
pnpm --filter homework-web-app build
pnpm --filter homework-web-app format:check
pnpm --filter homework-web-app test:e2e
```

`test:e2e` 需要先安装 Playwright 浏览器，并由测试脚本自行启动 Vite。普通单元和组件测试不依赖真实后端。

## 目录结构

```text
src/
├── app/                 # 应用入口、路由和全局 Provider
├── features/            # 按业务模块组织的页面、接口和类型
├── layouts/             # 登录后始终存在的 AppShell 导航
├── shared/              # 通用接口层、组件、Hooks 和工具
└── styles/              # 全局主题与响应式样式
```

桌面端使用固定顶部导航，移动端使用顶部品牌区和固定底部导航。登录、注册和 OAuth 回调页不显示用户导航。

## 当前边界

- “最新 Hit”按后端现有顺序展示，不在前端虚构热门排序。
- 原型中没有后端接口的功能暂不实现。
- Hit 编辑、删除、举报和图片上传不在当前版本范围内。
- OAuth 当前默认关闭；Turnstile 和支付只能在配置真实平台凭证后完成端到端联调。
