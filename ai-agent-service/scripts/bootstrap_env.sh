#!/usr/bin/env sh
set -e

cd "$(dirname "$0")/.."

UV_INSTALL_DIR="${UV_INSTALL_DIR:-$HOME/.local/bin}"
UV_PYTHON_VERSION="${UV_PYTHON_VERSION:-3.11}"
UV_PROJECT_ENVIRONMENT="${UV_PROJECT_ENVIRONMENT:-.venv}"
AI_AGENT_INSTALL_DEV_DEPS="${AI_AGENT_INSTALL_DEV_DEPS:-0}"
TENCENT_SPEECH_SDK_DIR="${TENCENT_SPEECH_SDK_DIR:-vendor/tencentcloud-speech-sdk-python}"

ensure_uv() {
  PATH="$UV_INSTALL_DIR:$PATH"
  export PATH

  if command -v uv >/dev/null 2>&1; then
    return 0
  fi

  mkdir -p "$UV_INSTALL_DIR"
  echo "[ai-agent/bootstrap] 未检测到 uv，开始安装到 $UV_INSTALL_DIR"
  curl -LsSf https://astral.sh/uv/install.sh | env UV_INSTALL_DIR="$UV_INSTALL_DIR" UV_NO_MODIFY_PATH=1 sh
  PATH="$UV_INSTALL_DIR:$PATH"
  export PATH

  if ! command -v uv >/dev/null 2>&1; then
    echo "[ai-agent/bootstrap] uv 安装失败" >&2
    exit 1
  fi
}

ensure_uv

install_tencent_speech_sdk() {
  if [ ! -f "$TENCENT_SPEECH_SDK_DIR/asr/speech_recognizer.py" ]; then
    echo "[ai-agent/bootstrap] 缺少腾讯实时语音 SDK vendor 目录：$TENCENT_SPEECH_SDK_DIR" >&2
    exit 1
  fi

  sdk_abs_path="$(cd "$TENCENT_SPEECH_SDK_DIR" && pwd)"
  site_packages="$("$UV_PROJECT_ENVIRONMENT/bin/python" - <<'PY'
import site

paths = site.getsitepackages()
print(paths[0])
PY
)"
  mkdir -p "$site_packages"
  printf "%s\n" "$sdk_abs_path" > "$site_packages/ai_agent_tencent_speech_sdk.pth"

  echo "[ai-agent/bootstrap] 已将腾讯实时语音 SDK 纳入 uv 环境"
  "$UV_PROJECT_ENVIRONMENT/bin/python" - <<'PY'
from asr import speech_recognizer
from common import credential
PY
}

echo "[ai-agent/bootstrap] 使用 uv 管理 Python 环境"
uv python install "$UV_PYTHON_VERSION" >/dev/null
if [ -d "$UV_PROJECT_ENVIRONMENT" ] && [ ! -f "$UV_PROJECT_ENVIRONMENT/.managed-by-uv" ]; then
  echo "[ai-agent/bootstrap] 检测到旧虚拟环境，重建为 uv 托管环境"
  rm -rf "$UV_PROJECT_ENVIRONMENT"
fi
if [ -d "$UV_PROJECT_ENVIRONMENT" ] && [ ! -x "$UV_PROJECT_ENVIRONMENT/bin/python" ]; then
  echo "[ai-agent/bootstrap] 检测到不可用虚拟环境，重建为当前机器可用环境"
  rm -rf "$UV_PROJECT_ENVIRONMENT"
fi
if [ ! -f "$UV_PROJECT_ENVIRONMENT/.managed-by-uv" ]; then
  uv venv --python "$UV_PYTHON_VERSION" "$UV_PROJECT_ENVIRONMENT" >/dev/null
  touch "$UV_PROJECT_ENVIRONMENT/.managed-by-uv"
fi
case "$AI_AGENT_INSTALL_DEV_DEPS" in
  1|true|yes)
    uv sync --directory . --python "$UV_PROJECT_ENVIRONMENT/bin/python" --all-extras --dev >/dev/null
    ;;
  0|false|no)
    uv sync --directory . --python "$UV_PROJECT_ENVIRONMENT/bin/python" --all-extras --no-dev >/dev/null
    ;;
  *)
    echo "[ai-agent/bootstrap] 不支持的 AI_AGENT_INSTALL_DEV_DEPS=$AI_AGENT_INSTALL_DEV_DEPS，可选值：1/0 true/false yes/no" >&2
    exit 1
    ;;
esac

install_tencent_speech_sdk
