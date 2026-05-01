from __future__ import annotations

from typing import Dict

from app.realtime.agent_models import (
    CLASSROOM_MANAGEMENT_MARKERS,
    HOMEWORK_MARKERS,
    QUESTION_MARKERS,
    clean_text,
    contains_any,
)


class TurnAggregator:
    def normalize(self, *, segment: Dict, state: Dict) -> Dict:
        text = clean_text(segment.get("text"), 4000)
        speaker = clean_text(segment.get("speaker") or "unknown", 32)
        seq = int(segment.get("seq") or segment.get("sequence") or 0)
        role_hint = self._role_hint(speaker, text)
        open_turn = state.get("openTurn") if isinstance(state.get("openTurn"), dict) else None

        if self._should_append(open_turn, speaker, role_hint):
            merged = dict(open_turn)
            merged["endSegment"] = seq or int(merged.get("endSegment") or 0)
            merged["text"] = clean_text(f"{merged.get('text', '')}{text}", 4000)
            merged["roleHint"] = self._merge_role_hint(str(merged.get("roleHint") or ""), role_hint)
            merged["updatedBySegment"] = seq
            return {"action": "append_to_open_turn", "turn": merged}

        turn = {
            "turnId": f"turn-{max(seq, int(state.get('turnCount') or 0) + 1)}",
            "speaker": speaker,
            "startSegment": seq,
            "endSegment": seq,
            "text": text,
            "roleHint": role_hint,
        }
        return {"action": "create_turn", "turn": turn, "closedTurn": open_turn}

    def _should_append(self, open_turn: Dict | None, speaker: str, role_hint: str) -> bool:
        if not open_turn:
            return False
        if str(open_turn.get("speaker") or "") != speaker:
            return False
        if role_hint == "question":
            return False
        return str(open_turn.get("roleHint") or "") not in {"question", "homework"}

    def _role_hint(self, speaker: str, text: str) -> str:
        if speaker == "student" and contains_any(text, QUESTION_MARKERS):
            return "question"
        if contains_any(text, HOMEWORK_MARKERS):
            return "homework"
        if contains_any(text, CLASSROOM_MANAGEMENT_MARKERS):
            return "classroom_management"
        if speaker == "teacher":
            return "explanation"
        return "unknown"

    def _merge_role_hint(self, existing: str, incoming: str) -> str:
        if incoming in {"homework", "question", "classroom_management"}:
            return incoming
        return existing or incoming or "unknown"
