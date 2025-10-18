#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
RETRIES=${RETRIES:-30}
DELAY_SECONDS=${DELAY_SECONDS:-1}
TIMEOUT_SECONDS=${TIMEOUT_SECONDS:-5}
INCLUDE_METRICS=${INCLUDE_METRICS:-false}

join_url() {
  local base="$1" path="$2"
  base="${base%/}"
  if [[ "$path" != /* ]]; then path="/$path"; fi
  printf "%s%s" "$base" "$path"
}

test_endpoint() {
  local url="$1" name="$2" expect="$3" required="$4"
  local attempt=0 code=0 passed=false err content
  local start_ts=$(date +%s.%3N || date +%s)
  while (( attempt < RETRIES )); do
    attempt=$((attempt+1))
    err=""
    content=""
    if content=$(curl -sS -m "$TIMEOUT_SECONDS" -H 'Accept: application/json, text/plain, */*' -w "\n%{http_code}" "$url" 2>curl.err); then
      :
    else
      err=$(cat curl.err || true)
    fi
    rm -f curl.err || true
    code=$(printf "%s" "$content" | tail -n1)
    body=$(printf "%s" "$content" | sed '$d')
    if [[ "$code" =~ ^[0-9]{3}$ ]] && (( code >= 200 && code < 300 )); then
      if [[ -z "$expect" || "$body" == *"$expect"* ]]; then
        passed=true
        break
      else
        err="Response missing expected text '$expect'"
      fi
    else
      if [[ -z "$err" ]]; then err="Unexpected status code $code"; fi
    fi
    if (( attempt < RETRIES )); then sleep "$DELAY_SECONDS"; fi
  done
  local end_ts=$(date +%s.%3N || date +%s)
  local duration=$(awk -v s="$start_ts" -v e="$end_ts" 'BEGIN{printf "%.3f", (e-s)}' 2>/dev/null || printf "%s" "$(( ${end_ts%.*} - ${start_ts%.*} ))")

  printf "%s|%s|%s|%s|%s|%s\n" "$name" "$url" "$required" "$passed" "$code" "$duration"
  if [[ "$passed" != "true" && -n "$err" ]]; then
    printf "error:%s\n" "$err"
  fi
}

declare -a TESTS=(
  "/actuator/health|Actuator Health|UP|true"
  "/actuator/health/liveness|Liveness|UP|true"
  "/actuator/health/readiness|Readiness|UP|true"
  "/api/v1/check/ping|Ping|pong|false"
)

if [[ "$INCLUDE_METRICS" == "true" ]]; then
  TESTS+=("/actuator/info|Actuator Info||false")
  TESTS+=("/actuator/prometheus|Prometheus Metrics|#|false")
fi

echo "==> Health check: $BASE_URL"
echo "    Retries: $RETRIES, Delay: ${DELAY_SECONDS}s, Timeout per call: ${TIMEOUT_SECONDS}s"

failed_required=0
failed_optional=0

for t in "${TESTS[@]}"; do
  IFS='|' read -r path name expect required <<< "$t"
  url=$(join_url "$BASE_URL" "$path")
  line=$(test_endpoint "$url" "$name" "$expect" "$required")
  errline=""
  if read -t 0.1 -r errline; then :; fi < <(true) # noop
  IFS='|' read -r n u req pass code duration <<< "$line"
  if [[ "$pass" == "true" ]]; then
    printf "✅ %s [%s] (%ss) -> %s\n" "$n" "$code" "$duration" "$u"
  else
    if [[ "$req" == "true" ]]; then
      printf "❌ %s [%s] (%ss) -> %s\n" "$n" "${code:--}" "$duration" "$u"
      failed_required=$((failed_required+1))
    else
      printf "⚠️  %s [%s] (%ss) -> %s\n" "$n" "${code:--}" "$duration" "$u"
      failed_optional=$((failed_optional+1))
    fi
  fi
done

echo
if (( failed_required == 0 )); then
  echo "All required health checks passed."
else
  echo "$failed_required required health check(s) failed."
fi
if (( failed_optional > 0 )); then
  echo "$failed_optional optional check(s) failed (not blocking)."
fi

if (( failed_required > 0 )); then exit 1; else exit 0; fi
