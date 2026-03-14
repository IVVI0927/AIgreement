#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT_DIR/.run"
LOG_DIR="$RUN_DIR/logs"
PID_FILE="$RUN_DIR/pids.env"

mkdir -p "$LOG_DIR"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1"
    exit 1
  fi
}

require_port() {
  local host="$1"
  local port="$2"
  if ! nc -z "$host" "$port" >/dev/null 2>&1; then
    echo "Service not reachable at $host:$port"
    return 1
  fi
  return 0
}

start_proc() {
  local name="$1"
  local cmd="$2"
  local log_file="$LOG_DIR/$name.log"
  local key
  key="$(echo "$name" | tr '[:lower:]-' '[:upper:]_')"
  echo "Starting $name ..."
  nohup bash -lc "$cmd" >"$log_file" 2>&1 &
  local pid="$!"
  echo "$name pid=$pid log=$log_file"
  echo "${key}_PID=$pid" >>"$PID_FILE"
}

for c in mvn npm nc nohup bash; do
  require_cmd "$c"
done

if [[ -f "$PID_FILE" ]]; then
  echo "Existing local run detected at $PID_FILE"
  echo "Please run ./stop-local.sh first."
  exit 1
fi

touch "$PID_FILE"

if ! require_port "127.0.0.1" "5432"; then
  echo "Please start PostgreSQL first (expected: localhost:5432)."
  rm -f "$PID_FILE"
  exit 1
fi

if ! require_port "127.0.0.1" "6379"; then
  echo "Please start Redis first (expected: localhost:6379)."
  rm -f "$PID_FILE"
  exit 1
fi

start_proc "discovery-service" "cd '$ROOT_DIR' && mvn -pl microservices/discovery-service spring-boot:run"
start_proc "llm-service" "cd '$ROOT_DIR' && EUREKA_SERVER_URL='http://admin:admin@localhost:8761/eureka/' LLAMA_BASE_URL='http://localhost:11434' mvn -pl microservices/llm-service spring-boot:run"
start_proc "contract-service" "cd '$ROOT_DIR' && EUREKA_SERVER_URL='http://admin:admin@localhost:8761/eureka/' DATABASE_URL='jdbc:postgresql://localhost:5432/legaldb' DB_USER='legaluser' DB_PASSWORD='legalpass' mvn -pl microservices/contract-service spring-boot:run"
start_proc "api-gateway" "cd '$ROOT_DIR' && EUREKA_SERVER_URL='http://admin:admin@localhost:8761/eureka/' mvn -pl microservices/api-gateway spring-boot:run"
start_proc "frontend" "cd '$ROOT_DIR/frontend' && npm run dev -- --host 0.0.0.0 --port 3000"

cat <<EOF

Started local stack in background.
PID file: $PID_FILE
Logs dir:  $LOG_DIR

Health URLs:
  http://localhost:8761/actuator/health
  http://localhost:8083/actuator/health
  http://localhost:8081/actuator/health
  http://localhost:8080/actuator/health
Frontend:
  http://localhost:3000

Stop all:
  ./stop-local.sh
EOF
