from __future__ import annotations

import time
from difflib import SequenceMatcher
from typing import Dict, List

from app.core.config import get_settings
from app.core.id_generator import new_task_id
from app.realtime.agent_models import TECHNICAL_TERMS, clean_text, evidence_event_ids, unique_texts

SUPPORTED_PATCH_TYPES = {
    "append_section",
    "update_section",
    "append_item",
    "merge_section",
    "mark_question",
    "mark_homework",
    "noop",
}


class SummaryPatchWriter:
    def write(self, *, context: Dict, decision: Dict) -> Dict:
        events = context.get("recentEvents") or []
        state = context.get("state") or {}
        outline = state.get("minutesOutline") or []
        patch_type = "update_section" if outline else "append_section"
        if "topic_transition" in (decision.get("triggerReasons") or []) and outline:
            patch_type = "append_section"

        summary_events = [
            event for event in events if event.get("type") not in {"classroom_management"}
        ]
        if not summary_events:
            return {
                "patchId": new_task_id("patch"),
                "patchType": "noop",
                "reason": "信息密度不足",
            }

        title = self._title(summary_events, state)
        summary = self._summary(summary_events, title)
        item = self._item(summary_events)
        questions = self._field_events(summary_events, "student_question")
        homework = self._field_events(summary_events, "homework_assigned")
        key_points = self._key_points(summary_events)
        patch = {
            "patchId": new_task_id("patch"),
            "patchType": patch_type,
            "currentTopic": self._topic(summary_events, state),
            "summary": summary,
            "studentQuestions": questions,
            "homeworkCandidates": homework,
            "keyPoints": key_points,
        }
        if patch_type == "append_section":
            patch["section"] = {
                "title": title,
                "summary": summary,
                "startSegment": self._start_segment(summary_events),
                "endSegment": self._end_segment(summary_events),
                "items": [item] if item else [],
            }
        else:
            patch["targetSectionId"] = outline[-1].get("id")
            patch["appendItems"] = [item] if item else []
        return patch

    def _title(self, events: List[Dict], state: Dict) -> str:
        if any(event.get("type") in {"example_started", "practice_or_drill"} for event in events):
            return "例题求解步骤"
        topic = self._topic(events, state)
        if topic:
            return f"{topic}理解"
        if any(event.get("type") == "homework_assigned" for event in events):
            return "课后任务整理"
        return "课堂阶段总结"

    def _topic(self, events: List[Dict], state: Dict) -> str | None:
        for event in reversed(events):
            if event.get("topic"):
                return str(event.get("topic"))
        return state.get("currentTopic")

    def _summary(self, events: List[Dict], title: str) -> str:
        texts = [clean_text(event.get("text"), 80) for event in events[:4]]
        joined = "；".join(text for text in texts if text)
        if not joined:
            return title
        return clean_text(joined, 220)

    def _item(self, events: List[Dict]) -> Dict | None:
        event = next((item for item in events if item.get("type") != "student_question"), None)
        if not event:
            event = events[0] if events else None
        if not event:
            return None
        title = "本段重点"
        if event.get("type") == "homework_assigned":
            title = "课后任务"
        elif event.get("type") in {"example_started", "practice_or_drill"}:
            title = "解题步骤"
        return {
            "title": title,
            "detail": clean_text(event.get("text"), 220),
            "evidenceEventIds": [event.get("eventId")],
        }

    def _field_events(self, events: List[Dict], event_type: str) -> List[Dict]:
        return [
            {
                "text": clean_text(event.get("text"), 220),
                "evidenceEventIds": [event.get("eventId")],
            }
            for event in events
            if event.get("type") == event_type
        ][-5:]

    def _key_points(self, events: List[Dict]) -> List[Dict]:
        return [
            {
                "text": clean_text(event.get("text"), 220),
                "evidenceEventIds": [event.get("eventId")],
            }
            for event in events
            if event.get("type") in {"teacher_emphasis", "concept_explained"}
        ][-5:]

    def _start_segment(self, events: List[Dict]) -> int:
        ranges = [
            (event.get("evidence") or {}).get("segmentRange") or [0, 0]
            for event in events
        ]
        return min((int(item[0] or 0) for item in ranges if item), default=0)

    def _end_segment(self, events: List[Dict]) -> int:
        ranges = [
            (event.get("evidence") or {}).get("segmentRange") or [0, 0]
            for event in events
        ]
        return max((int(item[-1] or 0) for item in ranges if item), default=0)


class QualityGuard:
    def validate(self, *, patch: Dict, context: Dict) -> Dict:
        reasons: List[str] = []
        patch_type = patch.get("patchType")
        if patch_type not in SUPPORTED_PATCH_TYPES:
            reasons.append("unsupported_patch_type")

        events = {
            str(event.get("eventId")): event
            for event in context.get("recentEvents") or []
            if event.get("eventId")
        }
        if patch_type != "noop":
            self._check_technical_terms(patch, reasons)
            self._check_evidence(patch, events, reasons)
            self._check_role_mismatch(patch, events, reasons)

        severity = "pass" if not reasons else "reject"
        return {
            "accepted": not reasons,
            "severity": severity,
            "reasons": reasons,
            "repairedPatch": patch if not reasons else None,
        }

    def _check_technical_terms(self, patch: Dict, reasons: List[str]) -> None:
        text = str(patch)
        if any(term in text for term in TECHNICAL_TERMS):
            reasons.append("technical_terms")

    def _check_evidence(self, patch: Dict, events: Dict[str, Dict], reasons: List[str]) -> None:
        evidence_items = self._evidence_items(patch)
        if not evidence_items and patch.get("patchType") not in {"noop"}:
            reasons.append("missing_evidence")
            return
        for event_id in evidence_items:
            if event_id not in events:
                reasons.append("invalid_evidence_reference")
                return

    def _check_role_mismatch(self, patch: Dict, events: Dict[str, Dict], reasons: List[str]) -> None:
        for question in patch.get("studentQuestions") or []:
            for event_id in evidence_event_ids(question):
                event = events.get(event_id) or {}
                evidence = event.get("evidence") or {}
                if event.get("type") != "student_question" or evidence.get("speaker") == "teacher":
                    reasons.append("speaker_role_mismatch")
                    return

    def _evidence_items(self, patch: Dict) -> List[str]:
        result: List[str] = []
        section = patch.get("section") if isinstance(patch.get("section"), dict) else {}
        for item in section.get("items") or []:
            result.extend(evidence_event_ids(item))
        for item in patch.get("appendItems") or []:
            result.extend(evidence_event_ids(item))
        for field in ["studentQuestions", "homeworkCandidates", "keyPoints"]:
            for item in patch.get(field) or []:
                result.extend(evidence_event_ids(item))
        return result


class PatchApplier:
    def __init__(self):
        self.settings = get_settings()

    def apply(self, *, state: Dict, patch: Dict) -> Dict:
        projection = {
            "minutesOutline": [dict(item) for item in state.get("minutesOutline") or []],
            "studentQuestions": list(state.get("studentQuestions") or []),
            "homeworkCandidates": list(state.get("homeworkCandidates") or []),
            "keyPoints": list(state.get("keyPoints") or []),
            "latestStageSummary": state.get("latestStageSummary"),
            "activeSectionTitle": state.get("activeSectionTitle"),
            "currentTopic": patch.get("currentTopic") or state.get("currentTopic"),
        }
        patch_type = patch.get("patchType")
        if patch_type == "noop":
            return projection
        if patch_type == "append_section":
            self._append_section(projection, patch)
        elif patch_type == "update_section":
            self._update_section(projection, patch)
        elif patch_type == "append_item":
            self._append_item(projection, patch)
        elif patch_type == "mark_question":
            pass
        elif patch_type == "mark_homework":
            pass
        self._merge_lists(projection, patch)
        return projection

    def _append_section(self, projection: Dict, patch: Dict) -> None:
        section = dict(patch.get("section") or {})
        outline = projection["minutesOutline"]
        section["id"] = section.get("id") or f"section-{len(outline) + 1}"
        section["title"] = clean_text(section.get("title") or "课堂阶段总结", 40)
        section["summary"] = clean_text(section.get("summary") or patch.get("summary"), 260)
        section["items"] = self._dedupe_items(section.get("items") or [])
        section["updatedAt"] = int(time.time())
        outline.append(section)
        projection["minutesOutline"] = outline[-12:]
        projection["latestStageSummary"] = section["summary"]
        projection["activeSectionTitle"] = section["title"]

    def _update_section(self, projection: Dict, patch: Dict) -> None:
        outline = projection["minutesOutline"]
        target_id = patch.get("targetSectionId") or (outline[-1].get("id") if outline else None)
        target = next((item for item in outline if item.get("id") == target_id), None)
        if not target:
            patch = {**patch, "section": {"title": "课堂阶段总结", "summary": patch.get("summary"), "items": patch.get("appendItems") or []}}
            self._append_section(projection, patch)
            return
        if patch.get("summary"):
            target["summary"] = clean_text(patch.get("summary"), 260)
            projection["latestStageSummary"] = target["summary"]
        target["items"] = self._dedupe_items([*(target.get("items") or []), *(patch.get("appendItems") or [])])
        target["endSegment"] = patch.get("endSegment") or target.get("endSegment") or 0
        target["updatedAt"] = int(time.time())
        projection["activeSectionTitle"] = target.get("title")

    def _append_item(self, projection: Dict, patch: Dict) -> None:
        patch = {**patch, "summary": None}
        self._update_section(projection, patch)

    def _merge_lists(self, projection: Dict, patch: Dict) -> None:
        projection["studentQuestions"] = unique_texts([
            *projection.get("studentQuestions", []),
            *(item.get("text") for item in patch.get("studentQuestions") or []),
        ])
        projection["homeworkCandidates"] = unique_texts([
            *projection.get("homeworkCandidates", []),
            *(item.get("text") for item in patch.get("homeworkCandidates") or []),
        ])
        projection["keyPoints"] = unique_texts([
            *projection.get("keyPoints", []),
            *(item.get("text") for item in patch.get("keyPoints") or []),
        ])

    def _dedupe_items(self, items: List[Dict]) -> List[Dict]:
        result: List[Dict] = []
        for item in items:
            normalized = {
                "title": clean_text(item.get("title") or "本段重点", 40),
                "detail": clean_text(item.get("detail") or item.get("content"), 240),
            }
            if not normalized["detail"]:
                continue
            if any(self._similar(normalized["detail"], existing.get("detail", "")) > 0.78 for existing in result):
                continue
            result.append(normalized)
        return result[-self.settings.realtime_agent_max_section_items:]

    def _similar(self, left: str, right: str) -> float:
        return SequenceMatcher(None, left, right).ratio()
