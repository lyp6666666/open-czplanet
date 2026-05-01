from __future__ import annotations

from app.llm.provider import get_llm_provider
from app.realtime.memory import build_lesson_report_memory_context


class LessonReportPipeline:
    def generate(self, payload: dict) -> dict:
        payload = self._enrich_with_realtime_memory(payload)
        provider = get_llm_provider()
        report = provider.generate_lesson_report(payload)
        report.setdefault("version", "v1")
        return report

    def _enrich_with_realtime_memory(self, payload: dict) -> dict:
        lesson_id = payload.get("lessonId")
        if lesson_id is None:
            return payload
        memory = build_lesson_report_memory_context(int(lesson_id))
        if not memory.get("episodes") and not memory.get("studentQuestions") and not memory.get("homeworkEvents"):
            return payload
        enriched = dict(payload)
        enriched["realtimeMemory"] = memory
        episode_text = "\n".join(
            f"- {item.get('title')}: {item.get('summary')}"
            for item in memory.get("episodes") or []
            if item.get("summary")
        )
        questions = "\n".join(
            f"- {item}" for item in memory.get("studentQuestions") or [] if item
        )
        homework = "\n".join(f"- {item}" for item in memory.get("homeworkEvents") or [] if item)
        if episode_text:
            enriched["teacherNotes"] = "\n\n".join(
                part for part in [payload.get("teacherNotes"), "实时课堂阶段记忆：\n" + episode_text] if part
            )
        if questions:
            enriched["studentPerformance"] = "\n\n".join(
                part for part in [payload.get("studentPerformance"), "课堂疑问：\n" + questions] if part
            )
        if homework and not payload.get("homework"):
            enriched["homework"] = homework
        return enriched
