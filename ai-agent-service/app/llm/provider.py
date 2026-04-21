from __future__ import annotations

from app.core.config import get_settings
from app.llm.base import LLMProvider
from app.llm.openai_compatible_provider import OpenAICompatibleProvider
from app.llm.template_provider import TemplateLLMProvider


def get_llm_provider() -> LLMProvider:
    settings = get_settings()
    provider = settings.llm_provider.strip().lower()
    if provider in {"template", "mock", ""}:
        return TemplateLLMProvider()
    if provider in {
        "openai-compatible",
        "openai_compatible",
        "deepseek",
        "qwen",
        "volcengine-ark",
        "volcengine_ark",
    }:
        return OpenAICompatibleProvider(settings)
    raise ValueError(f"unsupported_llm_provider:{settings.llm_provider}")
