#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

mkdir -p artifacts

runner="pytest"
if command -v poetry >/dev/null 2>&1; then
  runner="poetry run pytest"
fi

reruns="${QA_RERUNS:-0}"
rerun_delay="${QA_RERUN_DELAY_S:-1}"
extra=""
if [[ "$reruns" != "0" ]]; then
  extra="--reruns ${reruns} --reruns-delay ${rerun_delay}"
fi

eval "$runner -m 'smoke' ${extra} --junitxml=artifacts/junit-smoke.xml"
