#!/bin/sh

set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

DB_NAME="${DB_NAME:-ai_tutor}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-Aa123456}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-}"
OUTPUT_FORMAT="${OUTPUT_FORMAT:-text}"

find_mysql_container() {
  if [ -n "$MYSQL_CONTAINER" ]; then
    echo "$MYSQL_CONTAINER"
    return 0
  fi
  if command -v docker >/dev/null 2>&1; then
    detected="$(docker ps --format '{{.Names}}' | grep -E '^mysql$' | head -n 1 || true)"
    if [ -z "$detected" ]; then
      detected="$(docker ps --format '{{.Names}}' | grep -E '(^|_)mysql$' | head -n 1 || true)"
    fi
    if [ -n "$detected" ]; then
      echo "$detected"
      return 0
    fi
  fi
  return 1
}

mysql_query() {
  sql="$1"
  if [ -n "${MYSQL_CONTAINER_RESOLVED:-}" ]; then
    docker exec -i "$MYSQL_CONTAINER_RESOLVED" mysql -N -B -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$DB_NAME" -e "$sql"
    return 0
  fi
  mysql -N -B -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$DB_NAME" -e "$sql"
}

if MYSQL_CONTAINER_RESOLVED="$(find_mysql_container)"; then
  echo "[verify_db_schema_against_huoyue] 使用容器内 mysql 客户端: $MYSQL_CONTAINER_RESOLVED"
else
  MYSQL_CONTAINER_RESOLVED=""
  echo "[verify_db_schema_against_huoyue] 使用宿主机 mysql 客户端: ${MYSQL_HOST}:${MYSQL_PORT}"
fi
export MYSQL_CONTAINER_RESOLVED DB_NAME MYSQL_USER MYSQL_PASSWORD MYSQL_HOST MYSQL_PORT OUTPUT_FORMAT

python3 - <<'PY'
from pathlib import Path
import json
import re
import subprocess
import sys
import os

root = Path(os.getcwd())
schema_path = root / "sqlDoc" / "huoyue.sql"
sql = schema_path.read_text()
blocks = re.findall(r"CREATE TABLE `([^`]+)` \((.*?)\) ENGINE=", sql, flags=re.S)
expected = {}
for table, body in blocks:
    cols = []
    for line in body.splitlines():
        line = line.strip().rstrip(",")
        if line.startswith("`"):
            cols.append(line.split("`")[1])
    expected[table] = cols

shell = """mysql_query() {
  sql="$1"
  if [ -n "${MYSQL_CONTAINER_RESOLVED:-}" ]; then
    docker exec -i "$MYSQL_CONTAINER_RESOLVED" mysql -N -B -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$DB_NAME" -e "$sql"
    return 0
  fi
  mysql -N -B -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$DB_NAME" -e "$sql"
}
mysql_query "SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() ORDER BY TABLE_NAME, ORDINAL_POSITION"
"""
out = subprocess.check_output(
    ["sh", "-lc", shell],
    text=True,
    env=os.environ,
)

actual = {}
for raw in out.splitlines():
    if not raw.strip():
        continue
    table, column = raw.split("\t", 1)
    actual.setdefault(table, []).append(column)

missing_tables = []
missing_cols = []
for table, cols in expected.items():
    if table not in actual:
        missing_tables.append(table)
        continue
    miss = [c for c in cols if c not in actual[table]]
    if miss:
        missing_cols.append({"table": table, "columns": miss})

result = {
    "missing_tables": missing_tables,
    "missing_columns": missing_cols,
}

fmt = os.environ.get("OUTPUT_FORMAT", "text")
if fmt == "json":
    print(json.dumps(result, ensure_ascii=False, indent=2))
else:
    if not missing_tables and not missing_cols:
        print("[verify_db_schema_against_huoyue] OK: local schema matches sqlDoc/huoyue.sql")
    else:
        print("[verify_db_schema_against_huoyue] DETECTED DRIFT against sqlDoc/huoyue.sql")
        if missing_tables:
            print("[verify_db_schema_against_huoyue] Missing tables:")
            for table in missing_tables:
                print(f"  - {table}")
        if missing_cols:
            print("[verify_db_schema_against_huoyue] Missing columns:")
            for item in missing_cols:
                print(f"  - {item['table']}: {', '.join(item['columns'])}")

if missing_tables or missing_cols:
    sys.exit(1)
PY
