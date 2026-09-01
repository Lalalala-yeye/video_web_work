#!/usr/bin/env bash
# 在 CI / 本机起齐五个业务服务 + 网关（对外 8081）。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-ci}"
export MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
export MYSQL_PORT="${MYSQL_PORT:-3306}"
export MYSQL_DATABASE="${MYSQL_DATABASE:-doinb}"
export MYSQL_USER="${MYSQL_USER:-root}"
export MYSQL_PASSWORD="${MYSQL_PASSWORD:-test}"
export JWT_SECRET="${JWT_SECRET:-ci-test-secret-at-least-32-characters!!}"
export DOINB_INTERNAL_TOKEN="${DOINB_INTERNAL_TOKEN:-doinb-internal-dev-token}"
export DOINB_USER_URL="${DOINB_USER_URL:-http://127.0.0.1:8082}"
export DOINB_VIDEO_URL="${DOINB_VIDEO_URL:-http://127.0.0.1:8083}"
export DOINB_LIVE_URL="${DOINB_LIVE_URL:-http://127.0.0.1:8084}"
export DOINB_INTERACT_URL="${DOINB_INTERACT_URL:-http://127.0.0.1:8085}"
export DOINB_MESSAGE_URL="${DOINB_MESSAGE_URL:-http://127.0.0.1:8086}"
export UPLOAD_PATH="${UPLOAD_PATH:-/tmp/doinb-uploads}"
export APP_VERSION="${APP_VERSION:-ci}"
mkdir -p "$UPLOAD_PATH"

start_jar() {
  local name="$1" port="$2"
  local jar="$ROOT/${name}/target/${name}-0.0.1-SNAPSHOT.jar"
  if [ ! -f "$jar" ]; then
    echo "missing $jar" >&2
    exit 1
  fi
  nohup java -jar "$jar" --server.port="$port" --spring.profiles.active=ci \
    > "/tmp/${name}.log" 2>&1 &
  echo $! > "/tmp/${name}.pid"
}

wait_health() {
  local port="$1" name="$2"
  for i in $(seq 1 60); do
    if curl -sf "http://127.0.0.1:${port}/health" >/dev/null; then
      echo "${name} is up on ${port}"
      return 0
    fi
    sleep 2
  done
  echo "${name} failed to start" >&2
  tail -80 "/tmp/${name}.log" || true
  return 1
}

start_jar doinb-user 8082
start_jar doinb-video 8083
start_jar doinb-live 8084
start_jar doinb-interact 8085
start_jar doinb-message 8086
start_jar doinb-gateway 8081

wait_health 8082 doinb-user
wait_health 8083 doinb-video
wait_health 8084 doinb-live
wait_health 8085 doinb-interact
wait_health 8086 doinb-message
wait_health 8081 doinb-gateway
echo "all services up"
