from __future__ import annotations

from functools import lru_cache
from pathlib import Path
from typing import Optional

from dotenv import load_dotenv
from pydantic_settings import BaseSettings, SettingsConfigDict


ROOT_DIR = Path(__file__).resolve().parents[2]
load_dotenv(ROOT_DIR / ".env")


class Settings(BaseSettings):
    env: str = "dev"
    host: str = "0.0.0.0"
    port: int = 18086
    database_url: str = "sqlite:///./.data/ai_agent.db"
    redis_url: str = "redis://:123456@127.0.0.1:6379/2"
    queue_name: str = "ai-agent"
    use_async_worker: bool = True
    internal_token: Optional[str] = None

    llm_provider: str = "template"
    llm_base_url: Optional[str] = None
    llm_api_key: Optional[str] = None
    llm_model: Optional[str] = None
    llm_timeout_seconds: float = 30.0
    llm_ark_endpoint_id: Optional[str] = "ep-20260420222959-g4vrz"

    task_default_timeout_seconds: int = 180
    report_max_teacher_notes_chars: int = 12000
    chat_summary_max_messages: int = 300
    realtime_stage_summary_interval_seconds: int = 300
    realtime_stage_min_segments: int = 8
    realtime_transcript_buffer_size: int = 500
    realtime_min_audio_rms: float = 0.012
    realtime_silence_pause_seconds: int = 20

    tencent_asr_enabled: bool = False
    tencent_asr_app_id: Optional[str] = None
    tencent_asr_secret_id: Optional[str] = None
    tencent_asr_secret_key: Optional[str] = None
    tencent_asr_engine_model_type: str = "16k_zh"
    tencent_asr_voice_format: int = 1
    tencent_asr_need_vad: int = 1
    tencent_speech_sdk_path: Optional[str] = None

    model_config = SettingsConfigDict(
        env_prefix="AI_AGENT_",
        extra="ignore",
        case_sensitive=False,
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()
