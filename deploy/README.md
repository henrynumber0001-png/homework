# HomeWork deployment runbook

This directory deploys the two static frontends behind Nginx and runs the two Spring Boot APIs as hardened systemd services. The server never needs Node.js, pnpm, or Maven; release artifacts are built on a workstation or in CI.

## Layout

```text
/opt/homework/releases/<release-id>/
├── backend/web-app.jar
├── backend/web-admin.jar
├── frontend/web-app/
├── frontend/web-admin/
└── deploy/

/opt/homework/current -> /opt/homework/releases/<release-id>
/etc/homework/web-app.env
/etc/homework/web-admin.env
```

Only Nginx ports 80 and 443 are public. Java ports 8080 and 8081 and MySQL port 3306 stay private.

## 1. Required values

Collect these values before provisioning:

- Server public IP, SSH user, and SSH key.
- User-facing domain and administration subdomain.
- Server region; mainland China domains need the appropriate filing before public service.
- MySQL location: local MySQL 8 or a managed private endpoint.
- Production Cloudflare Turnstile site key and secret.
- Private COS region, bucket name, and least-privilege CAM credentials.
- Two independent JWT secrets and three independent database passwords.
- Optional OAuth, LLM, and WeChat Pay production credentials.

Generate JWT and database secrets independently, for example:

```bash
openssl rand -base64 48
```

## 2. Database baseline

Flyway migrations live at `backend/common/src/main/resources/db/migration`.

- `V1__schema_baseline.sql` is a structure-only baseline generated from the current 50-table MySQL 8 schema.
- V2 aligns question type comments, and V3 removes a redundant legacy exam-answer index when present.
- Legacy scripts in the repository-level `sql/` directory are already represented by V1 and must not be replayed on a fresh database.

Create the database and accounts using `database/bootstrap-users.sql.template` after replacing all placeholders. Application accounts receive DML privileges; the dedicated migrator account owns DDL changes.

For a fresh, empty database:

```text
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=false
```

For an existing non-empty database without `flyway_schema_history`:

1. Stop all writers and take a verified backup.
2. Compare its schema with V1.
3. Set `FLYWAY_BASELINE_ON_MIGRATE=true` for the first successful deployment only.
4. Verify that Flyway baselined version 1 and applied later migrations.
5. Change the setting back to `false` and restart both services.

Do not run `baseline-on-migrate=true` against an unknown or partially migrated schema.

To transfer an existing local database, create an encrypted full backup, copy it through SSH, restore it into an empty private database, and then use the existing-database procedure above. Do not place a data dump in Git or in a public object bucket.

## 3. Build a release locally

The frontend variables are embedded at build time. Export production values in the shell running the package command. At minimum:

```bash
export VITE_API_BASE_URL=/api
export VITE_ADMIN_API_BASE_URL=/api/admin
export VITE_TURNSTILE_SITE_KEY='<production-site-key>'
export VITE_OAUTH_ENABLED=false
```

Then build and package a clean Git commit:

```bash
deploy/scripts/package-release.sh
```

This runs frontend tests and the complete build, then creates a versioned archive and SHA-256 checksum under the ignored `artifacts/` directory. A dirty worktree is rejected by default.

## 4. Prepare an Ubuntu host

Install Java 21, Nginx, curl, and the MySQL client. Install MySQL Server only when the database intentionally lives on the same host.

Copy the `deploy/` directory to the server, then run:

```bash
sudo deploy/scripts/install-host.sh
```

Edit both generated files and replace all placeholders:

```text
/etc/homework/web-app.env
/etc/homework/web-admin.env
```

The files are owned by root and readable by the `homework` service group. Store payment and TLS private keys under `/etc/homework/keys/` with equally restrictive permissions.

## 5. Configure DNS and initial HTTP

Create DNS A records for both domains pointing to the light server public IP. Render an HTTP configuration while issuing certificates:

```bash
sudo deploy/scripts/render-nginx.sh \
  http app.example.com admin.example.com \
  /etc/nginx/conf.d/homework.conf
```

The template exposes `/.well-known/acme-challenge/` from `/var/www/certbot`. You can use an ACME client or install certificates downloaded from Tencent Cloud.

After certificates exist, render the HTTPS configuration:

```bash
sudo deploy/scripts/render-nginx.sh \
  https app.example.com admin.example.com \
  /etc/nginx/conf.d/homework.conf \
  /absolute/path/app-fullchain.pem \
  /absolute/path/app-private-key.pem \
  /absolute/path/admin-fullchain.pem \
  /absolute/path/admin-private-key.pem
```

Update `ADMIN_PUBLIC_BASE_URL`, OAuth callbacks, and `WECHAT_PAY_NOTIFY_URL` to the same final HTTPS domains.

## 6. Deploy

Upload both the artifact and its adjacent `.sha256` file through SCP/SFTP. Use the exact release ID contained in the artifact filename:

```bash
sudo deploy/scripts/deploy-release.sh \
  /tmp/homework-<release-id>.tar.gz \
  <release-id>
```

The deployment performs these operations:

1. Verifies the checksum and archive paths.
2. Extracts a new immutable release directory.
3. Atomically moves the `current` symlink.
4. Restarts both APIs.
5. Waits for both Actuator health checks.
6. Reloads Nginx only after the APIs are healthy.
7. Restores the previous application release when health checks fail.

Database migrations are forward-only and are not automatically reversed by an application rollback.

## 7. Verify and roll back

Run local service and optional public-domain checks:

```bash
sudo deploy/scripts/verify-host.sh app.example.com admin.example.com
```

Inspect logs with:

```bash
journalctl -u homework-web-app -u homework-web-admin --since today
```

Roll application files back to a retained release:

```bash
sudo deploy/scripts/rollback-release.sh <previous-release-id>
```

After the infrastructure checks pass, manually exercise registration/login and Turnstile, admin login, COS upload and signed reads, database writes, and every enabled OAuth/payment callback.
