#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat >&2 <<'USAGE'
Usage:
  render-nginx.sh http APP_DOMAIN ADMIN_DOMAIN OUTPUT_PATH
  render-nginx.sh https APP_DOMAIN ADMIN_DOMAIN OUTPUT_PATH APP_CERT APP_KEY ADMIN_CERT ADMIN_KEY
USAGE
}

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run this script as root." >&2
  exit 1
fi

if [[ $# -lt 4 ]]; then
  usage
  exit 1
fi

MODE=$1
APP_DOMAIN=$2
ADMIN_DOMAIN=$3
OUTPUT_PATH=$4
SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
DEPLOY_ROOT=$(cd -- "$SCRIPT_DIR/.." && pwd)

for domain_name in "$APP_DOMAIN" "$ADMIN_DOMAIN"; do
  if [[ ! "$domain_name" =~ ^[A-Za-z0-9.-]+$ ]]; then
    echo "Invalid domain: $domain_name" >&2
    exit 1
  fi
done

TEMP_CONFIG=$(mktemp)
cleanup() {
  rm -f -- "$TEMP_CONFIG"
}
trap cleanup EXIT

if [[ "$MODE" == "http" && $# -eq 4 ]]; then
  TEMPLATE="$DEPLOY_ROOT/nginx/homework-http.conf.template"
  sed -e "s|__APP_DOMAIN__|$APP_DOMAIN|g" \
      -e "s|__ADMIN_DOMAIN__|$ADMIN_DOMAIN|g" \
      "$TEMPLATE" > "$TEMP_CONFIG"
elif [[ "$MODE" == "https" && $# -eq 8 ]]; then
  APP_CERT=$5
  APP_KEY=$6
  ADMIN_CERT=$7
  ADMIN_KEY=$8
  for certificate_path in "$APP_CERT" "$APP_KEY" "$ADMIN_CERT" "$ADMIN_KEY"; do
    if [[ "$certificate_path" != /* ]]; then
      echo "Certificate paths must be absolute: $certificate_path" >&2
      exit 1
    fi
  done
  TEMPLATE="$DEPLOY_ROOT/nginx/homework-https.conf.template"
  sed -e "s|__APP_DOMAIN__|$APP_DOMAIN|g" \
      -e "s|__ADMIN_DOMAIN__|$ADMIN_DOMAIN|g" \
      -e "s|__APP_CERTIFICATE__|$APP_CERT|g" \
      -e "s|__APP_CERTIFICATE_KEY__|$APP_KEY|g" \
      -e "s|__ADMIN_CERTIFICATE__|$ADMIN_CERT|g" \
      -e "s|__ADMIN_CERTIFICATE_KEY__|$ADMIN_KEY|g" \
      "$TEMPLATE" > "$TEMP_CONFIG"
else
  usage
  exit 1
fi

install -o root -g root -m 0644 "$TEMP_CONFIG" "$OUTPUT_PATH"
nginx -t
if systemctl is-active --quiet nginx; then
  systemctl reload nginx
fi
echo "Installed Nginx configuration at $OUTPUT_PATH"
