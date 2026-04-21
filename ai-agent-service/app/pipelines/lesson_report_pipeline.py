from __future__ import annotations

from app.llm.provider import get_llm_provider


class LessonReportPipeline:
    def generate(self, payload: dict) -> dict:
        provider = get_llm_provider()
        report = provider.generate_lesson_report(payload)
        report.setdefault("version", "v1")
        return report
