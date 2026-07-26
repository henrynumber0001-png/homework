# HomeWork 管理后台前端

面向桌面端的后台管理工程，核心流程是“先进入题库，再管理题目”。题库只能手动创建，题目支持表单创建与 Excel 批量导入。

本工程位于 HomeWork monorepo 的 `frontend/web-admin`，对应的后端模块是
`backend/web/web-admin`。

## 已实现

- 独立管理员登录、邀请激活、权限路由和二次认证。
- 题库卡片列表、手动创建、编辑、发布、下架和删除。
- 题库工作台、题目筛选、多选批量发布/下架、失败项反馈和拖拽排序。
- 简答、单选和多选题表单，以及腾讯云 COS 题目图片上传、替换和明确删除。
- Excel 模板下载、拖拽上传、逐行预检、错误列表和确认导入。
- 数据概览、用户、社区、会员与订单、套餐、管理员和操作日志基础页面。

## 本地启动

```bash
cd ../..
pnpm install
pnpm dev:admin
```

默认前端地址为 `http://localhost:5174`，开发服务器会将 `/api/admin` 代理到 `http://localhost:8081`。

如需连接其他后端地址，复制 `.env.example` 为 `.env.local`，修改 `VITE_ADMIN_PROXY_TARGET`。

后端管理员邀请默认使用 `http://localhost:5174/admin/invitation?token=...`，前端也兼容
`/invitation/:token` 形式。

## 质量检查

```bash
pnpm --filter homework-web-admin typecheck
pnpm --filter homework-web-admin test
pnpm --filter homework-web-admin build
```
