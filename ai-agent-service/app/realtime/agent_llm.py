from __future__ import annotations

import json
from typing import Dict

import httpx

from app.core.config import get_settings
from app.observability import record_realtime_agent_llm_call
from app.realtime.patches import SummaryPatchWriter


class RealtimeAgentLLMClient:
    def generate_summary_patch(self, *, context: Dict, decision: Dict) -> Dict:
        settings = get_settings()
        provider = settings.llm_provider.strip().lower()
        record_realtime_agent_llm_call(
            "summary_patch_writer",
            str(decision.get("modelTier") or "unknown"),
        )
        if provider in {"template", "mock", ""}:
            return SummaryPatchWriter().write(context=context, decision=decision)
        return self._complete_patch(context=context, decision=decision)

    def _complete_patch(self, *, context: Dict, decision: Dict) -> Dict:
        settings = get_settings()
        if not settings.llm_base_url or not settings.llm_api_key:
            return SummaryPatchWriter().write(context=context, decision=decision)
        prompt = self._prompt(context=context, decision=decision)
        body = {
            "model": settings.llm_model or settings.llm_ark_endpoint_id,
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "你是课堂实时纪要 Patch Writer。只能输出合法 JSON 对象。"
                        "不得输出完整 minutesOutline，只能输出增量 patch。"
                        "新增展示内容必须引用 evidenceEventIds。"
                    ),
                },
                {"role": "user", "content": prompt},
            ],
            "temperature": 0.2,
            "response_format": {"type": "json_object"},
        }
        headers = {"Authorization": f"Bearer {settings.llm_api_key}"}
        try:
            with httpx.Client(timeout=settings.llm_timeout_seconds) as client:
                response = client.post(
                    settings.llm_base_url.rstrip("/") + "/chat/completions",
                    json=body,
                    headers=headers,
                )
                response.raise_for_status()
                content = response.json()["choices"][0]["message"]["content"]
            patch = json.loads(content)
            patch.setdefault("provider", "openai-compatible")
            return patch
        except Exception:
            return SummaryPatchWriter().write(context=context, decision=decision)

    def _prompt(self, *, context: Dict, decision: Dict) -> str:
        schema = {
            "patchId": "string",
            "patchType": "append_section|update_section|append_item|merge_section|mark_question|mark_homework|noop",
            "targetSectionId": "string optional",
            "section": {
                "title": "string",
                "summary": "string",
                "startSegment": 1,
                "endSegment": 2,
                "items": [
                    {
                        "title": "string",
                        "detail": "string",
                        "evidenceEventIds": ["evt-id"],
                    }
                ],
            },
            "appendItems": [
                {"title": "string", "detail": "string", "evidenceEventIds": ["evt-id"]}
            ],
            "studentQuestions": [{"text": "string", "evidenceEventIds": ["evt-id"]}],
            "homeworkCandidates": [{"text": "string", "evidenceEventIds": ["evt-id"]}],
            "keyPoints": [{"text": "string", "evidenceEventIds": ["evt-id"]}],
        }
        safe_context = {
            "decision": decision,
            "projection": context.get("projection"),
            "recentTurns": context.get("recentTurns"),
            "recentEvents": context.get("recentEvents"),
            "studentLearningState": context.get("studentLearningState"),
        }
        return (
            "任务：根据课堂 Memory 生成实时纪要增量 patch。\n"
            "规则：\n"
            "1. 不要重写完整 minutesOutline。\n"
            "2. 如果是新阶段，输出 append_section；如果延续当前阶段，输出 update_section。\n"
            "3. 没有足够教学信息时输出 noop。\n"
            "4. 所有新增 item/question/homework/keyPoint 必须引用 recentEvents 中存在的 evidenceEventIds。\n"
            "5. 面向老师和学生展示，不要出现 ASR、LLM、模型、置信度等技术词。\n"
            f"输出 schema：{json.dumps(schema, ensure_ascii=False)}\n"
            f"输入：{json.dumps(safe_context, ensure_ascii=False)}"
        )
