#!/bin/sh

set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-Aa123456}"
DB_NAME="${DB_NAME:-ai_tutor}"
DB_CONTAINER="${DB_CONTAINER:-mysql}"
BOOTSTRAP_APPLY_HISTORICAL_MIGRATIONS="${BOOTSTRAP_APPLY_HISTORICAL_MIGRATIONS:-0}"

find_mysql_container() {
  if docker ps --format '{{.Names}}' | grep -Fx "$DB_CONTAINER" >/dev/null 2>&1; then
    echo "$DB_CONTAINER"
    return 0
  fi

  docker ps --format '{{.Names}} {{.Ports}}' | awk -v needle=":${MYSQL_PORT}->3306/tcp" '
    index($0, needle) {
      print $1
      exit
    }
  '
}

MYSQL_EXEC_MODE=""
MYSQL_CONTAINER=""

resolve_mysql_executor() {
  MYSQL_CONTAINER="$(find_mysql_container || true)"
  if [ -n "$MYSQL_CONTAINER" ]; then
    MYSQL_EXEC_MODE="docker"
    echo "[db_bootstrap_if_missing] 使用容器内 mysql 客户端: $MYSQL_CONTAINER"
    return 0
  fi

  if command -v mysql >/dev/null 2>&1; then
    MYSQL_EXEC_MODE="host"
    echo "[db_bootstrap_if_missing] 使用宿主机 mysql 客户端: ${MYSQL_HOST}:${MYSQL_PORT}"
    return 0
  fi

  echo "[db_bootstrap_if_missing] 未找到可用 mysql 客户端，也未找到运行中的 MySQL 容器"
  exit 1
}

mysql_exec() {
  if [ "$MYSQL_EXEC_MODE" = "docker" ]; then
    docker exec "$MYSQL_CONTAINER" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --default-character-set=utf8mb4 "$@"
    return 0
  fi

  mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --default-character-set=utf8mb4 "$@"
}

mysql_exec_stdin() {
  if [ "$MYSQL_EXEC_MODE" = "docker" ]; then
    docker exec -i "$MYSQL_CONTAINER" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --default-character-set=utf8mb4 "$@"
    return 0
  fi

  mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --default-character-set=utf8mb4 "$@"
}

database_exists() {
  mysql_exec -N -e "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '${DB_NAME}'" \
    | grep -Fx "$DB_NAME" >/dev/null 2>&1
}

table_exists() {
  table_name="$1"
  mysql_exec -N -e "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = '${DB_NAME}' AND TABLE_NAME = '${table_name}'" \
    | grep -Fx "$table_name" >/dev/null 2>&1
}

ensure_database() {
  if database_exists; then
    echo "[db_bootstrap_if_missing] 数据库已存在: $DB_NAME"
    return 0
  fi

  echo "[db_bootstrap_if_missing] 创建数据库: $DB_NAME"
  mysql_exec -e "CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
}

apply_baseline() {
  echo "[db_bootstrap_if_missing] 导入基础表结构: sqlDoc/huoyue.sql"
  mysql_exec_stdin "$DB_NAME" < "$ROOT_DIR/sqlDoc/huoyue.sql"
}

apply_migrations() {
  for f in "$ROOT_DIR"/sqlDoc/migrations/*.sql; do
    if [ ! -f "$f" ]; then
      continue
    fi
    echo "[db_bootstrap_if_missing] 应用迁移: $(basename "$f")"
    mysql_exec_stdin "$DB_NAME" < "$f"
  done
}

should_apply_historical_migrations() {
  case "$BOOTSTRAP_APPLY_HISTORICAL_MIGRATIONS" in
    1|true|yes)
      return 0
      ;;
    0|false|no)
      return 1
      ;;
    *)
      echo "[db_bootstrap_if_missing] 不支持的 BOOTSTRAP_APPLY_HISTORICAL_MIGRATIONS=$BOOTSTRAP_APPLY_HISTORICAL_MIGRATIONS，可选值：1/0 true/false yes/no"
      exit 1
      ;;
  esac
}

apply_live_class_migration_if_needed() {
  if table_exists "live_class_session"; then
    echo "[db_bootstrap_if_missing] 实时课堂表已存在，跳过课堂增量迁移"
    return 0
  fi

  live_class_sql="$ROOT_DIR/sqlDoc/migrations/20260420_live_class_service_init.sql"
  if [ ! -f "$live_class_sql" ]; then
    echo "[db_bootstrap_if_missing] 未找到课堂迁移文件: $(basename "$live_class_sql")"
    exit 1
  fi

  echo "[db_bootstrap_if_missing] 补齐实时课堂表结构: $(basename "$live_class_sql")"
  mysql_exec_stdin "$DB_NAME" < "$live_class_sql"
}

apply_seed() {
  echo "[db_bootstrap_if_missing] 导入开发种子数据: sqlDoc/seed_dev_data.sql"
  sed "s/^USE ai_tutor;$/USE \`${DB_NAME}\`;/" "$ROOT_DIR/sqlDoc/seed_dev_data.sql" | mysql_exec_stdin "$DB_NAME"
}

resolve_mysql_executor

if ! mysql_exec -N -e "SELECT 1" >/dev/null 2>&1; then
  echo "[db_bootstrap_if_missing] MySQL 不可用，请先确认 ${MYSQL_HOST}:${MYSQL_PORT} 或目标容器可连接"
  exit 1
fi

ensure_database

if table_exists "user"; then
  echo "[db_bootstrap_if_missing] 检测到核心表已存在，跳过基线初始化，按需补齐增量表结构"
  apply_live_class_migration_if_needed
  exit 0
fi

apply_baseline
if should_apply_historical_migrations; then
  echo "[db_bootstrap_if_missing] 已启用历史迁移回放，将继续执行 sqlDoc/migrations/*.sql"
  apply_migrations
else
  echo "[db_bootstrap_if_missing] 空库已导入最新基线，默认跳过历史迁移回放；如需强制重放，请设置 BOOTSTRAP_APPLY_HISTORICAL_MIGRATIONS=1"
fi
apply_seed

echo "[db_bootstrap_if_missing] 开发库初始化完成: $DB_NAME"
