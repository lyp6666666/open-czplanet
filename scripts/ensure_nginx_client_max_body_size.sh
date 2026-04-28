#!/bin/sh

set -eu

NGINX_CONF="${NGINX_CONF:-}"
CLIENT_MAX_BODY_SIZE="${CLIENT_MAX_BODY_SIZE:-25m}"

if [ "$(id -u)" != "0" ]; then
  echo "[ensure_nginx_client_max_body_size] 请使用 root 执行"
  exit 1
fi

if [ -z "$NGINX_CONF" ]; then
  echo "[ensure_nginx_client_max_body_size] 缺少 NGINX_CONF"
  exit 1
fi

CONF_PATH="$(readlink -f "$NGINX_CONF" 2>/dev/null || printf '%s' "$NGINX_CONF")"

if [ ! -f "$CONF_PATH" ]; then
  echo "[ensure_nginx_client_max_body_size] 配置文件不存在: $CONF_PATH"
  exit 1
fi

TMP_FILE="$(mktemp)"
BACKUP_FILE="${CONF_PATH}.bak.$(date +%Y%m%d%H%M%S)"

awk -v size="$CLIENT_MAX_BODY_SIZE" '
function count_open_brace(text,    tmp) {
  tmp = text
  return gsub(/\{/, "", tmp)
}

function count_close_brace(text,    tmp) {
  tmp = text
  return gsub(/\}/, "", tmp)
}

function flush_server_block(    i, line, inserted, found) {
  if (server_line_count == 0) {
    return
  }

  found = 0
  for (i = 1; i <= server_line_count; i++) {
    if (server_lines[i] ~ /^[[:space:]]*client_max_body_size[[:space:]]+[^;]+;/) {
      found = 1
      break
    }
  }

  inserted = 0
  for (i = 1; i <= server_line_count; i++) {
    line = server_lines[i]
    if (line ~ /^[[:space:]]*client_max_body_size[[:space:]]+[^;]+;/) {
      sub(/client_max_body_size[[:space:]]+[^;]+;/, "client_max_body_size " size ";", line)
      print line
      continue
    }

    print line
    if (i == 1 && !found && !inserted) {
      print "    client_max_body_size " size ";"
      inserted = 1
    }
  }

  delete server_lines
  server_line_count = 0
}

BEGIN {
  in_server = 0
  server_depth = 0
  server_line_count = 0
}

{
  if (!in_server) {
    if ($0 ~ /^[[:space:]]*server[[:space:]]*\{[[:space:]]*$/) {
      in_server = 1
      server_depth = 0
      server_line_count = 0
    } else {
      print $0
      next
    }
  }

  server_line_count++
  server_lines[server_line_count] = $0
  server_depth += count_open_brace($0)
  server_depth -= count_close_brace($0)

  if (in_server && server_depth == 0) {
    flush_server_block()
    in_server = 0
  }
}

END {
  if (in_server) {
    flush_server_block()
  }
}
' "$CONF_PATH" >"$TMP_FILE"

cp "$CONF_PATH" "$BACKUP_FILE"
mv "$TMP_FILE" "$CONF_PATH"

nginx -t
systemctl enable nginx >/dev/null 2>&1 || true
systemctl reload nginx || systemctl restart nginx

echo "[ensure_nginx_client_max_body_size] 已更新: $CONF_PATH"
echo "[ensure_nginx_client_max_body_size] 备份: $BACKUP_FILE"
echo "[ensure_nginx_client_max_body_size] client_max_body_size=$CLIENT_MAX_BODY_SIZE"
