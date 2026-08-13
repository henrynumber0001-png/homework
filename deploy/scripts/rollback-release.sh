#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run this script as root." >&2
  exit 1
fi
if [[ $# -ne 1 ]]; then
  echo "Usage: rollback-release.sh RELEASE_ID" >&2
  exit 1
fi

RELEASE_ID=$1
if [[ ! "$RELEASE_ID" =~ ^[A-Za-z0-9._-]+$ ]]; then
  echo "Invalid release ID." >&2
  exit 1
fi

RELEASE_DIR="/opt/homework/releases/$RELEASE_ID"
CURRENT_LINK=/opt/homework/current
if [[ ! -d "$RELEASE_DIR" ]]; then
  echo "Unknown release: $RELEASE_DIR" >&2
  exit 1
fi

ln -sfn "$RELEASE_DIR" "${CURRENT_LINK}.next"
mv -Tf "${CURRENT_LINK}.next" "$CURRENT_LINK"
systemctl restart homework-web-app.service homework-web-admin.service

curl --fail --retry 20 --retry-delay 2 --retry-connrefused http://127.0.0.1:8080/actuator/health
curl --fail --retry 20 --retry-delay 2 --retry-connrefused http://127.0.0.1:8081/actuator/health
echo
echo "Rolled application files back to $RELEASE_ID. Database migrations were not reversed."
