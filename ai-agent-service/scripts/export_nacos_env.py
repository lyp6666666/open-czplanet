#!/usr/bin/env python3
from __future__ import annotations

import os
import sys
import urllib.parse
import urllib.request

import yaml


def shell_quote(value: str) -> str:
    return "'" + value.replace("'", "'\"'\"'") + "'"


def main() -> int:
    server_addr = os.getenv("NACOS_SERVER_ADDR", "127.0.0.1:8848").strip()
    namespace = (
        os.getenv("NACOS_CONFIG_NAMESPACE")
        or os.getenv("NACOS_NAMESPACE")
        or "481e4376-4576-4b18-ac19-f61e170ca3ae"
    ).strip()
    group = os.getenv("AI_AGENT_NACOS_GROUP", "DEFAULT_GROUP").strip()
    profile = os.getenv("SPRING_PROFILES_ACTIVE", "dev").strip() or "dev"
    data_id = os.getenv("AI_AGENT_NACOS_DATA_ID", f"ai-agent-service-{profile}.yaml").strip()
    timeout = float(os.getenv("AI_AGENT_NACOS_FETCH_TIMEOUT", "5").strip() or "5")

    query = urllib.parse.urlencode(
        {
            "tenant": namespace,
            "group": group,
            "dataId": data_id,
        }
    )
    url = f"http://{server_addr}/nacos/v1/cs/configs?{query}"
    try:
        with urllib.request.urlopen(url, timeout=timeout) as response:
            content = response.read().decode("utf-8")
    except Exception as exc:
        print(f"echo '[ai-agent] failed to fetch nacos config: {exc}' >&2")
        return 1

    if not content.strip() or content.strip() == "config data not exist":
        print(f"echo '[ai-agent] nacos config missing: {data_id}' >&2")
        return 1

    raw = yaml.safe_load(content) or {}
    root = raw.get("ai-agent") or {}
    service = root.get("service") or {}
    database = root.get("database") or {}
    redis_cfg = root.get("redis") or {}
    llm = root.get("llm") or {}
    task = root.get("task") or {}
    realtime = root.get("realtime") or {}
    tencent_asr = root.get("tencent-asr") or {}

    mappings: dict[str, object] = {
        "AI_AGENT_PORT": service.get("port"),
        "AI_AGENT_INTERNAL_TOKEN": service.get("internal-token"),
        "AI_AGENT_DATABASE_URL": database.get("url"),
        "AI_AGENT_REDIS_URL": redis_cfg.get("url"),
        "AI_AGENT_QUEUE_NAME": redis_cfg.get("queue-name"),
        "AI_AGENT_USE_ASYNC_WORKER": redis_cfg.get("use-async-worker"),
        "AI_AGENT_LLM_PROVIDER": llm.get("provider"),
        "AI_AGENT_LLM_BASE_URL": llm.get("base-url"),
        "AI_AGENT_LLM_API_KEY": llm.get("api-key"),
        "AI_AGENT_LLM_MODEL": llm.get("model"),
        "AI_AGENT_LLM_ARK_ENDPOINT_ID": llm.get("ark-endpoint-id"),
        "AI_AGENT_LLM_TIMEOUT_SECONDS": llm.get("timeout-seconds"),
        "AI_AGENT_TASK_DEFAULT_TIMEOUT_SECONDS": task.get("default-timeout-seconds"),
        "AI_AGENT_REPORT_MAX_TEACHER_NOTES_CHARS": task.get("report-max-teacher-notes-chars"),
        "AI_AGENT_CHAT_SUMMARY_MAX_MESSAGES": task.get("chat-summary-max-messages"),
        "AI_AGENT_REALTIME_STAGE_SUMMARY_INTERVAL_SECONDS": realtime.get("stage-summary-interval-seconds"),
        "AI_AGENT_REALTIME_STAGE_MIN_SEGMENTS": realtime.get("stage-min-segments"),
        "AI_AGENT_REALTIME_TRANSCRIPT_BUFFER_SIZE": realtime.get("transcript-buffer-size"),
        "AI_AGENT_TENCENT_ASR_ENABLED": tencent_asr.get("enabled"),
        "AI_AGENT_TENCENT_ASR_APP_ID": tencent_asr.get("app-id"),
        "AI_AGENT_TENCENT_ASR_SECRET_ID": tencent_asr.get("secret-id"),
        "AI_AGENT_TENCENT_ASR_SECRET_KEY": tencent_asr.get("secret-key"),
        "AI_AGENT_TENCENT_ASR_ENGINE_MODEL_TYPE": tencent_asr.get("engine-model-type"),
        "AI_AGENT_TENCENT_ASR_VOICE_FORMAT": tencent_asr.get("voice-format"),
        "AI_AGENT_TENCENT_ASR_NEED_VAD": tencent_asr.get("need-vad"),
        "AI_AGENT_TENCENT_SPEECH_SDK_PATH": tencent_asr.get("speech-sdk-path"),
    }

    for key, value in mappings.items():
        if value is None:
            continue
        if isinstance(value, bool):
            text = "true" if value else "false"
        else:
            text = str(value)
        print(f"export {key}={shell_quote(text)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
