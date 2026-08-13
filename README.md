# HomeWork

HomeWork is a monorepo containing the user-facing application, the administration
console, and their shared backend modules.

## Project Structure

```text
homework/
├── backend/
│   ├── common/                 # Shared backend utilities
│   ├── model/                  # Entities and enums
│   └── web/
│       ├── web-app/            # User API (default port: 8080)
│       └── web-admin/          # Admin API (default port: 8081)
├── frontend/
│   ├── web-app/                # React user application (default port: 5173)
│   └── web-admin/              # Vue admin console (default port: 5174)
├── docs/                       # Design and API documentation
├── sql/                        # Database initialization and migration scripts
├── package.json                # Cross-project commands
├── pnpm-workspace.yaml         # Frontend workspace configuration
└── backend/pom.xml             # Backend Maven reactor
```

The backend retains its Maven multi-module structure. Both frontend applications
are managed through a single pnpm workspace and one `pnpm-lock.yaml`. Build
outputs, dependency directories, IDE settings, and local environment variables
are excluded from Git.

## Core Documentation

- [System Design Overview (English)](system-design-en.md)
- [System Design Overview](docs/system-design.md)
- [Main API Reference](docs/api-reference.md)

Additional topic-specific documentation remains available in `docs/`, including
detailed rules for administration, messaging, membership payments, and file
storage.

## Production Deployment

The Tencent Cloud light-server deployment assets and the step-by-step runbook
are in [`deploy/README.md`](deploy/README.md). The supported layout uses Nginx,
two systemd-managed Spring Boot services, Flyway migrations, versioned release
directories, health checks, and application rollback.

## Requirements

- JDK 21
- Maven 3.9+
- Node.js 20
- pnpm 10

## Install Frontend Dependencies

```bash
pnpm install
```

Create local configuration files from the provided examples. Store real secrets
only in the ignored local files:

```bash
cp frontend/web-app/.env.example frontend/web-app/.env.local
cp frontend/web-admin/.env.example frontend/web-admin/.env.local
```

## Common Commands

```bash
# Start both frontend applications
pnpm dev

# Start the user application or admin console separately
pnpm dev:web
pnpm dev:admin

# Frontend checks
pnpm lint
pnpm typecheck
pnpm test:frontend
pnpm build:frontend

# Backend checks
pnpm test:backend
pnpm build:backend

# Run all tests or build the entire project
pnpm test
pnpm build
```

Before starting the backend locally, prepare the appropriate
`application-local.yml` files and required external services. After the initial
build, run the applications separately:

```bash
mvn -f backend/pom.xml install -DskipTests
mvn -f backend/web/web-app/pom.xml spring-boot:run
mvn -f backend/web/web-admin/pom.xml spring-boot:run
```

For detailed frontend configuration and testing instructions, see
[`frontend/web-app/README.md`](frontend/web-app/README.md) and
[`frontend/web-admin/README.md`](frontend/web-admin/README.md).
