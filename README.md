# HomeWork

HomeWork 采用单仓库管理用户端、管理端和共享后端模块。仓库原有 Git 历史来自
`homework-backend`；原来的独立 `homework-frontend` 已并入本仓库。

## 目录结构

```text
homework/
├── backend/
│   ├── common/                 # 后端公共能力
│   ├── model/                  # 实体与枚举
│   └── web/
│       ├── web-app/            # 用户端 API（默认 8080）
│       └── web-admin/          # 管理端 API（默认 8081）
├── frontend/
│   ├── web-app/                # React 用户端（默认 5173）
│   └── web-admin/              # Vue 管理端（默认 5174）
├── docs/                       # 设计与接口文档
├── sql/                        # 数据库初始化与迁移脚本
├── package.json                # 跨项目常用命令
├── pnpm-workspace.yaml         # 前端 workspace
└── backend/pom.xml             # 后端 Maven reactor
```

后端继续保持原有 Maven 多模块关系，前端统一使用一个 pnpm workspace 和一个
`pnpm-lock.yaml`。构建产物、依赖目录、IDE 配置和本地环境变量均不进入 Git。

## 环境要求

- JDK 21
- Maven 3.9+
- Node.js 20
- pnpm 10

## 安装前端依赖

```bash
pnpm install
```

本地配置从对应示例复制，真实密钥只写入被忽略的本地文件：

```bash
cp frontend/web-app/.env.example frontend/web-app/.env.local
cp frontend/web-admin/.env.example frontend/web-admin/.env.local
```

## 常用命令

```bash
# 同时启动两个前端
pnpm dev

# 单独启动用户端或管理端前端
pnpm dev:web
pnpm dev:admin

# 前端检查
pnpm lint
pnpm typecheck
pnpm test:frontend
pnpm build:frontend

# 后端检查
pnpm test:backend
pnpm build:backend

# 全部测试或全部构建
pnpm test
pnpm build
```

后端本地启动前需准备相应的 `application-local.yml` 和外部服务。首次构建后可分别
运行：

```bash
mvn -f backend/pom.xml install -DskipTests
mvn -f backend/web/web-app/pom.xml spring-boot:run
mvn -f backend/web/web-admin/pom.xml spring-boot:run
```

更具体的前端配置和测试方式见
[`frontend/web-app/README.md`](frontend/web-app/README.md) 与
[`frontend/web-admin/README.md`](frontend/web-admin/README.md)。
