#!/usr/bin/env bash
set -euo pipefail

nginx -t
systemctl is-active homework-web-app.service
systemctl is-active homework-web-admin.service
curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health
echo
curl --fail --silent --show-error http://127.0.0.1:8081/actuator/health
echo

if [[ $# -eq 2 ]]; then
  curl --fail --silent --show-error --head "https://$1/" | head -n 1
  curl --fail --silent --show-error --head "https://$2/" | head -n 1
elif [[ $# -ne 0 ]]; then
  echo "Usage: verify-host.sh [APP_DOMAIN ADMIN_DOMAIN]" >&2
  exit 1
fi
