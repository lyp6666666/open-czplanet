#!/bin/bash

# 仅用于 lyp 本地开发切换 codex 的账户 apikey

# 检查是否提供了参数（1 / 2 / 3）
if [ -z "$1" ]; then
  echo "❌ 请提供参数: 1 / 2 / 3"
  echo "用法: ./switch_key.sh [1|2|3]"
  exit 1
fi

# 根据传入的参数设置对应的环境变量
case "$1" in
  1)
    if [ -z "$CODEX_API_KEY_1" ]; then
      echo "❌ 没有找到账号1的环境变量 (CODEX_API_KEY_1)"
      exit 1
    fi
    KEY="$CODEX_API_KEY_1"
    ;;
  2)
    if [ -z "$CODEX_API_KEY_2" ]; then
      echo "❌ 没有找到账号2的环境变量 (CODEX_API_KEY_2)"
      exit 1
    fi
    KEY="$CODEX_API_KEY_2"
    ;;
  3)
    if [ -z "$CODEX_API_KEY_3" ]; then
      echo "❌ 没有找到账号3的环境变量 (CODEX_API_KEY_3)"
      exit 1
    fi
    KEY="$CODEX_API_KEY_3"
    ;;
  *)
    echo "❌ 用法: ./switch_key.sh [1|2|3]"
    exit 1
    ;;
esac

echo "✅ 正在切换到账号 $1"

FILE=~/.codex/auth.json

# 文件不存在直接报错（更安全）
if [ ! -f "$FILE" ]; then
  echo "❌ 未找到 $FILE"
  exit 1
fi

TMP_FILE=$(mktemp)

# 替换 key
jq --arg key "$KEY" '.OPENAI_API_KEY = $key' "$FILE" > "$TMP_FILE" && mv "$TMP_FILE" "$FILE"

echo "✅ 已切换完成"