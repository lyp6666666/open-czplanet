from __future__ import annotations

from typing import Dict, List

from app.realtime.agent_models import (
    CLASSROOM_MANAGEMENT_MARKERS,
    HOMEWORK_MARKERS,
    KEY_POINT_MARKERS,
    PRACTICE_MARKERS,
    QUESTION_MARKERS,
    TRANSITION_MARKERS,
    clean_text,
    contains_any,
    detect_topic,
)


class TeachingEventExtractor:
    def extract(self, *, turn: Dict, state: Dict) -> List[Dict]:
        text = clean_text(turn.get("text"), 4000)
        if not text:
            return []
        speaker = clean_text(turn.get("speaker") or "unknown", 32)
        topic = detect_topic(text, state.get("currentTopic"))
        evidence = {
            "turnIds": [turn.get("turnId")],
            "segmentRange": [int(turn.get("startSegment") or 0), int(turn.get("endSegment") or 0)],
            "quotes": [text[:120]],
            "speaker": speaker,
        }
        base_id = f"evt-{turn.get('endSegment') or turn.get('turnId')}"
        events: List[Dict] = []

        if speaker == "student" and contains_any(text, QUESTION_MARKERS):
            events.append(self._event(base_id, "student_question", text, topic, evidence, 0.9))
        if speaker == "teacher" and contains_any(text, KEY_POINT_MARKERS):
            events.append(self._event(base_id, "teacher_emphasis", text, topic, evidence, 0.86))
        if speaker == "teacher" and contains_any(text, HOMEWORK_MARKERS):
            events.append(self._event(base_id, "homework_assigned", text, topic, evidence, 0.9))
        if contains_any(text, TRANSITION_MARKERS):
            events.append(self._event(base_id, "stage_transition", text, topic, evidence, 0.78))
        if contains_any(text, PRACTICE_MARKERS):
            event_type = "example_started" if "例题" in text else "practice_or_drill"
            events.append(self._event(base_id, event_type, text, topic, evidence, 0.72))
        if contains_any(text, CLASSROOM_MANAGEMENT_MARKERS):
            events.append(self._event(base_id, "classroom_management", text, topic, evidence, 0.82))
        if speaker == "teacher" and not events:
            events.append(self._event(base_id, "concept_explained", text, topic, evidence, 0.62))
        return self._dedupe(events)

    def _event(
        self,
        base_id: str,
        event_type: str,
        text: str,
        topic: str | None,
        evidence: Dict,
        confidence: float,
    ) -> Dict:
        return {
            "eventId": f"{base_id}-{event_type}",
            "type": event_type,
            "topic": topic,
            "text": clean_text(text, 500),
            "confidence": confidence,
            "evidence": evidence,
        }

    def _dedupe(self, events: List[Dict]) -> List[Dict]:
        seen = set()
        result = []
        for event in events:
            key = (event.get("type"), event.get("text"))
            if key in seen:
                continue
            seen.add(key)
            result.append(event)
        return result
