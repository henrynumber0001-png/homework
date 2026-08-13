#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd -- "$SCRIPT_DIR/../.." && pwd)
cd "$REPO_ROOT"

if [[ -n "$(git status --porcelain)" && "${ALLOW_DIRTY:-0}" != "1" ]]; then
  echo "Refusing to package a dirty worktree. Commit the release or set ALLOW_DIRTY=1 explicitly." >&2
  exit 1
fi

: "${VITE_TURNSTILE_SITE_KEY:?Set the production VITE_TURNSTILE_SITE_KEY before packaging}"
export VITE_API_BASE_URL="${VITE_API_BASE_URL:-/api}"
export VITE_ADMIN_API_BASE_URL="${VITE_ADMIN_API_BASE_URL:-/api/admin}"
export VITE_OAUTH_ENABLED="${VITE_OAUTH_ENABLED:-false}"

GIT_REVISION=$(git rev-parse --short=12 HEAD)
RELEASE_ID=${1:-"$(date -u +%Y%m%dT%H%M%SZ)-${GIT_REVISION}"}
if [[ ! "$RELEASE_ID" =~ ^[A-Za-z0-9._-]+$ ]]; then
  echo "Release ID may contain only letters, numbers, dots, underscores, and hyphens." >&2
  exit 1
fi

pnpm run test:frontend
pnpm build

STAGING_DIR=$(mktemp -d)
cleanup() {
  rm -rf -- "$STAGING_DIR"
}
trap cleanup EXIT

install -d "$STAGING_DIR/backend" "$STAGING_DIR/frontend/web-app" "$STAGING_DIR/frontend/web-admin"
install -m 0644 backend/web/web-app/target/web-app-0.0.1-SNAPSHOT.jar "$STAGING_DIR/backend/web-app.jar"
install -m 0644 backend/web/web-admin/target/web-admin-0.0.1-SNAPSHOT.jar "$STAGING_DIR/backend/web-admin.jar"
cp -R frontend/web-app/dist/. "$STAGING_DIR/frontend/web-app/"
cp -R frontend/web-admin/dist/. "$STAGING_DIR/frontend/web-admin/"
cp -R deploy "$STAGING_DIR/deploy"
printf '%s\n' "$RELEASE_ID" > "$STAGING_DIR/release.txt"
printf '%s\n' "$GIT_REVISION" > "$STAGING_DIR/git-revision.txt"

install -d artifacts
ARTIFACT="artifacts/homework-${RELEASE_ID}.tar.gz"
tar -C "$STAGING_DIR" -czf "$ARTIFACT" .

if command -v sha256sum >/dev/null 2>&1; then
  CHECKSUM=$(sha256sum "$ARTIFACT" | awk '{print $1}')
else
  CHECKSUM=$(shasum -a 256 "$ARTIFACT" | awk '{print $1}')
fi
printf '%s  %s\n' "$CHECKSUM" "$(basename "$ARTIFACT")" > "${ARTIFACT}.sha256"

echo "Created $ARTIFACT"
echo "Checksum: ${ARTIFACT}.sha256"
