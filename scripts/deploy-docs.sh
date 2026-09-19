#!/usr/bin/env bash
# Deploy static site (docs/) to your server over SSH using rsync.
#
# Prerequisites: SSH access to the server (key-based auth recommended).
#
# Usage:
#   export DEPLOY_HOST="user@your-server.com"
#   export DEPLOY_PATH="/path/to/web/root"   # e.g. /var/www/copdfuel.com/html
#   ./scripts/deploy-docs.sh
#
# Optional: dry run (no changes)
#   DRY_RUN=1 ./scripts/deploy-docs.sh
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -z "${DEPLOY_HOST:-}" ]]; then
  echo "Error: Set DEPLOY_HOST to user@hostname (example: export DEPLOY_HOST=ubuntu@copdfuel.com)"
  exit 1
fi

REMOTE_PATH="${DEPLOY_PATH:-/var/www/html}"
RSYNC=(rsync -avz --delete --exclude '.git')
if [[ -n "${DRY_RUN:-}" ]]; then
  RSYNC+=(--dry-run)
  echo "Dry run only."
fi

"${RSYNC[@]}" "docs/" "${DEPLOY_HOST}:${REMOTE_PATH}/"
echo "Done. Uploaded docs/ to ${DEPLOY_HOST}:${REMOTE_PATH}/"
