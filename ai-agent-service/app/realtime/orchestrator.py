from __future__ import annotations

import time
from typing import Dict, List

from app.core.config import get_settings


SUMMARY_EVENT_TYPES = {
    "student_question",
    "teacher_emphasis",
    "homework_assigned",
    "stage_transition",
    "example_started",
    "practice_or_drill",
}


class RealtimeOrchestrator:
    def __init__(self):
        self.settings = get_settings()

    def decide(self, *, state: Dict, events: List[Dict], stage_decision: Dict | None = None) -> Dict:
        if not state.get("llmEnabled", True):
            return self._decision("skip", [], "none")

        reasons = self._semantic_reasons(state, events, stage_decision or {})
        fixed = self._fixed_reason(state)
        if fixed:
            reasons.append(fixed)

        if self._is_first_summary(state):
            reasons.insert(0, "first_summary")

        reasons = list(dict.fromkeys(reasons))
        if not reasons:
            return self._decision("extract_only", [], "none")

        if self._only_low_value(events) and "fixed_interval" not in reasons:
            return self._decision("extract_only", [], "none")

        if self._cooldown_active(state) and "first_summary" not in reasons:
            return {
                **self._decision("extract_only", [], "none"),
                "suppressedTriggerReasons": reasons,
            }

        model_tier = "small" if "first_summary" in reasons and len(reasons) == 1 else "strong"
        return self._decision("generate_patch", reasons, model_tier)

    def _semantic_reasons(self, state: Dict, events: List[Dict], stage_decision: Dict) -> List[str]:
        reasons: List[str] = []
        event_types = [str(event.get("type") or "") for event in events]
        if "stage_transition" in event_types or stage_decision.get("stageDecision") in {"transition", "close_current"}:
            reasons.append("topic_transition")
        if "student_question" in event_types:
            recent_questions = [
                event for event in events if event.get("type") == "student_question"
            ]
            if len(recent_questions) >= 1:
                reasons.append("student_questions")
        if "teacher_emphasis" in event_types:
            reasons.append("teacher_emphasis")
        if "homework_assigned" in event_types:
            reasons.append("homework_assigned")
        dense_events = [event for event in events if event.get("type") in SUMMARY_EVENT_TYPES]
        if len(dense_events) >= self.settings.realtime_agent_event_density_threshold:
            reasons.append("event_density")
        return reasons

    def _fixed_reason(self, state: Dict) -> str | None:
        last_ts = int(state.get("lastLlmSummaryTs") or 0)
        if int(time.time()) - last_ts < self.settings.realtime_stage_summary_interval_seconds:
            return None
        last_count = int(state.get("lastLlmSegmentCount") or 0)
        count = int(state.get("segmentCount") or 0)
        if count - last_count >= self.settings.realtime_stage_min_segments:
            return "fixed_interval"
        return None

    def _is_first_summary(self, state: Dict) -> bool:
        if state.get("minutesOutline"):
            return False
        return int(state.get("turnCount") or 0) >= self.settings.realtime_agent_first_summary_min_turns

    def _cooldown_active(self, state: Dict) -> bool:
        last_ts = int(state.get("lastStrongPatchTs") or 0)
        return bool(last_ts and int(time.time()) - last_ts < self.settings.realtime_agent_strong_cooldown_seconds)

    def _only_low_value(self, events: List[Dict]) -> bool:
        if not events:
            return False
        return all(event.get("type") == "classroom_management" for event in events)

    def _decision(self, decision: str, reasons: List[str], model_tier: str) -> Dict:
        return {
            "decision": decision,
            "triggerReasons": reasons,
            "modelTier": model_tier,
            "contextPlan": {
                "recentTurns": self.settings.realtime_agent_recent_turn_limit,
                "recentEvents": self.settings.realtime_agent_recent_event_limit,
                "includeCurrentEpisode": True,
                "includeHistoricalEpisodes": False,
            },
        }
