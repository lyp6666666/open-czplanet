from __future__ import annotations

from typing import Dict, List

from app.realtime.agent_models import clean_text, detect_topic


class TopicStageTracker:
    def track(self, *, state: Dict, events: List[Dict]) -> Dict:
        event_types = [str(event.get("type") or "") for event in events]
        topic = self._topic(events, state)
        stage_type = self._stage_type(event_types, events, state)

        if "stage_transition" in event_types:
            return {
                "stageDecision": "transition",
                "currentTopic": topic,
                "stageType": stage_type,
                "reason": self._reason(events, "课堂出现阶段切换信号"),
                "confidence": 0.82,
            }
        if topic and topic != state.get("currentTopic") and state.get("currentTopic"):
            return {
                "stageDecision": "transition",
                "currentTopic": topic,
                "stageType": stage_type,
                "reason": f"课堂主题从{state.get('currentTopic')}切换到{topic}",
                "confidence": 0.76,
            }
        if "homework_assigned" in event_types:
            return {
                "stageDecision": "close_current",
                "currentTopic": topic,
                "stageType": "homework",
                "reason": self._reason(events, "课堂进入作业布置"),
                "confidence": 0.84,
            }
        return {
            "stageDecision": "continue",
            "currentTopic": topic,
            "stageType": stage_type,
            "reason": "课堂仍在当前主题内继续推进",
            "confidence": 0.62,
        }

    def _topic(self, events: List[Dict], state: Dict) -> str | None:
        for event in reversed(events):
            topic = event.get("topic") or detect_topic(str(event.get("text") or ""))
            if topic:
                return str(topic)
        return state.get("currentTopic")

    def _stage_type(self, event_types: List[str], events: List[Dict], state: Dict) -> str:
        if "homework_assigned" in event_types:
            return "homework"
        if "student_question" in event_types:
            return "qa"
        if "example_started" in event_types or "practice_or_drill" in event_types:
            return "practice"
        if "teacher_emphasis" in event_types or "concept_explained" in event_types:
            return "teach"
        if all(item == "classroom_management" for item in event_types) and event_types:
            return "classroom_management"
        return str(state.get("currentStageType") or "unknown")

    def _reason(self, events: List[Dict], fallback: str) -> str:
        event = events[-1] if events else {}
        text = clean_text(event.get("text"), 80)
        return f"{fallback}：{text}" if text else fallback
