#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run this script as root." >&2
  exit 1
fi

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
DEPLOY_ROOT=$(cd -- "$SCRIPT_DIR/.." && pwd)

for command_name in java nginx systemctl curl tar; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Missing required command: $command_name" >&2
    exit 1
  fi
done

if ! getent group homework >/dev/null; then
  groupadd --system homework
fi
if ! id homework >/dev/null 2>&1; then
  useradd --system --gid homework --home-dir /nonexistent --shell /usr/sbin/nologin homework
fi

install -d -o homework -g homework -m 0750 /opt/homework/releases
install -d -o homework -g homework -m 0750 /var/lib/homework/admin-imports
install -d -o root -g homework -m 0750 /etc/homework /etc/homework/keys
install -d -o www-data -g www-data -m 0755 /var/www/certbot

if [[ ! -e /etc/homework/web-app.env ]]; then
  install -o root -g homework -m 0640 "$DEPLOY_ROOT/env/web-app.env.example" /etc/homework/web-app.env
  echo "Created /etc/homework/web-app.env; replace every placeholder before deployment."
fi
if [[ ! -e /etc/homework/web-admin.env ]]; then
  install -o root -g homework -m 0640 "$DEPLOY_ROOT/env/web-admin.env.example" /etc/homework/web-admin.env
  echo "Created /etc/homework/web-admin.env; replace every placeholder before deployment."
fi

install -o root -g root -m 0644 "$DEPLOY_ROOT/systemd/homework-web-app.service" /etc/systemd/system/homework-web-app.service
install -o root -g root -m 0644 "$DEPLOY_ROOT/systemd/homework-web-admin.service" /etc/systemd/system/homework-web-admin.service
systemctl daemon-reload
systemctl enable homework-web-app.service homework-web-admin.service

echo "Host directories and systemd services are installed. Configure environment files and Nginx next."
