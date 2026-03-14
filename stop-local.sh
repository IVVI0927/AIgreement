#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="$ROOT_DIR/.run/pids.env"

if [[ ! -f "$PID_FILE" ]]; then
  echo "No PID file found: $PID_FILE"
  exit 0
fi

# shellcheck disable=SC1090
source "$PID_FILE"

stop_pid() {
  local name="$1"
  local pid="${2:-}"
  if [[ -z "$pid" ]]; then
    return 0
  fi
  if kill -0 "$pid" >/dev/null 2>&1; then
    echo "Stopping $name (pid=$pid)"
    kill "$pid" >/dev/null 2>&1 || true
  else
    echo "$name already stopped (pid=$pid)"
  fi
}

stop_pid "frontend" "${FRONTEND_PID:-}"
stop_pid "api-gateway" "${API_GATEWAY_PID:-}"
stop_pid "contract-service" "${CONTRACT_SERVICE_PID:-}"
stop_pid "llm-service" "${LLM_SERVICE_PID:-}"
stop_pid "discovery-service" "${DISCOVERY_SERVICE_PID:-}"

rm -f "$PID_FILE"
echo "Stopped local stack."
