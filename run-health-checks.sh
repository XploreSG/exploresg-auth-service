#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"

export BASE_URL="${BASE_URL:-}"
export RETRIES="${RETRIES:-}"
export DELAY_SECONDS="${DELAY_SECONDS:-}"
export TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-}"
export INCLUDE_METRICS="${INCLUDE_METRICS:-}"

exec "$SCRIPT_DIR/scripts/test-health.sh"
