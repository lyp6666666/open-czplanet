from __future__ import annotations

from app.llm.provider import get_llm_provider


class ChatSummaryPipeline:
    def generate(self, payload: dict) -> dict:
        provider = get_llm_provider()
        summary = provider.generate_chat_summary(payload)
        summary.setdefault("version", "v1")
        return summary
