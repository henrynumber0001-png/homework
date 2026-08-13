#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run this script as root." >&2
  exit 1
fi
if [[ $# -ne 2 ]]; then
  echo "Usage: deploy-release.sh ARTIFACT_PATH RELEASE_ID" >&2
  exit 1
fi

ARTIFACT_PATH=$1
RELEASE_ID=$2
DEPLOY_ROOT=/opt/homework
RELEASE_DIR="$DEPLOY_ROOT/releases/$RELEASE_ID"
CURRENT_LINK="$DEPLOY_ROOT/current"

if [[ ! -f "$ARTIFACT_PATH" ]]; then
  echo "Artifact not found: $ARTIFACT_PATH" >&2
  exit 1
fi
CHECKSUM_PATH="${ARTIFACT_PATH}.sha256"
if [[ -f "$CHECKSUM_PATH" ]]; then
  (
    cd -- "$(dirname -- "$ARTIFACT_PATH")"
    sha256sum --check "$(basename -- "$CHECKSUM_PATH")"
  )
else
  echo "Warning: checksum file not found: $CHECKSUM_PATH" >&2
fi
if [[ ! "$RELEASE_ID" =~ ^[A-Za-z0-9._-]+$ ]]; then
  echo "Invalid release ID." >&2
  exit 1
fi
if [[ -e "$RELEASE_DIR" ]]; then
  echo "Release directory already exists: $RELEASE_DIR" >&2
  exit 1
fi
ARCHIVE_LIST=$(tar -tzf "$ARTIFACT_PATH")
if grep -Eq '(^/|(^|/)\.\.(/|$))' <<< "$ARCHIVE_LIST"; then
  echo "Artifact contains an unsafe path." >&2
  exit 1
fi
for environment_file in /etc/homework/web-app.env /etc/homework/web-admin.env; do
  if [[ ! -r "$environment_file" ]]; then
    echo "Missing environment file: $environment_file" >&2
    exit 1
  fi
  if grep -Eq 'replace-with-|example\.com' "$environment_file"; then
    echo "Environment file still contains placeholders: $environment_file" >&2
    exit 1
  fi
done

PREVIOUS_RELEASE=""
if [[ -L "$CURRENT_LINK" ]]; then
  PREVIOUS_RELEASE=$(readlink -f "$CURRENT_LINK")
fi

install -d -o homework -g homework -m 0750 "$RELEASE_DIR"
tar -xzf "$ARTIFACT_PATH" -C "$RELEASE_DIR"
chown -R homework:homework "$RELEASE_DIR"

for required_path in \
  "$RELEASE_DIR/backend/web-app.jar" \
  "$RELEASE_DIR/backend/web-admin.jar" \
  "$RELEASE_DIR/frontend/web-app/index.html" \
  "$RELEASE_DIR/frontend/web-admin/index.html"; do
  if [[ ! -f "$required_path" ]]; then
    echo "Release is missing: $required_path" >&2
    exit 1
  fi
done

install -o root -g root -m 0644 "$RELEASE_DIR/deploy/systemd/homework-web-app.service" /etc/systemd/system/homework-web-app.service
install -o root -g root -m 0644 "$RELEASE_DIR/deploy/systemd/homework-web-admin.service" /etc/systemd/system/homework-web-admin.service
systemctl daemon-reload

ln -sfn "$RELEASE_DIR" "${CURRENT_LINK}.next"
mv -Tf "${CURRENT_LINK}.next" "$CURRENT_LINK"

systemctl restart homework-web-app.service
systemctl restart homework-web-admin.service

health_check() {
  local health_url=$1
  local attempt
  for attempt in $(seq 1 45); do
    if curl --fail --silent --show-error "$health_url" | grep -q '"status":"UP"'; then
      return 0
    fi
    sleep 2
  done
  return 1
}

if health_check http://127.0.0.1:8080/actuator/health \
  && health_check http://127.0.0.1:8081/actuator/health; then
  if systemctl is-active --quiet nginx; then
    nginx -t
    systemctl reload nginx
  fi
  echo "Release $RELEASE_ID is healthy and active."
  exit 0
fi

echo "Health check failed for release $RELEASE_ID." >&2
systemctl status homework-web-app.service homework-web-admin.service --no-pager >&2 || true
if [[ -n "$PREVIOUS_RELEASE" && -d "$PREVIOUS_RELEASE" ]]; then
  echo "Restoring previous application release: $PREVIOUS_RELEASE" >&2
  ln -sfn "$PREVIOUS_RELEASE" "${CURRENT_LINK}.next"
  mv -Tf "${CURRENT_LINK}.next" "$CURRENT_LINK"
  systemctl restart homework-web-app.service homework-web-admin.service
  echo "Application files were rolled back. Database migrations were not reversed." >&2
fi
exit 1
