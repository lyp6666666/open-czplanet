from __future__ import annotations

from typing import Dict

from app.realtime.memory import MemoryRetriever


class ContextBuilder:
    def __init__(self):
        self.retriever = MemoryRetriever()

    def build(self, *, lesson_id: int, decision: Dict) -> Dict:
        plan = decision.get("contextPlan") or {}
        return self.retriever.retrieve(
            lesson_id=lesson_id,
            recent_turns=int(plan.get("recentTurns") or 12),
            recent_events=int(plan.get("recentEvents") or 20),
        )
