from __future__ import annotations

from typing import Dict, List

from app.realtime.agent_models import clean_text, unique_texts
from app.realtime.state_store import RealtimeStateStore


class MemoryRetriever:
    def __init__(self):
        self.store = RealtimeStateStore()

    def retrieve(self, *, lesson_id: int, recent_turns: int = 12, recent_events: int = 20) -> Dict:
        state = self.store.get_state(lesson_id)
        return {
            "state": state,
            "recentTurns": self.store.recent_turns(lesson_id, recent_turns),
            "recentEvents": self.store.recent_events(lesson_id, recent_events),
            "currentEpisode": state.get("currentEpisode"),
            "studentLearningState": state.get("studentLearningState") or self.empty_learning_state(),
            "projection": {
                "minutesOutline": state.get("minutesOutline") or [],
                "latestStageSummary": state.get("latestStageSummary"),
                "studentQuestions": state.get("studentQuestions") or [],
                "homeworkCandidates": state.get("homeworkCandidates") or [],
                "keyPoints": state.get("keyPoints") or [],
                "activeSectionTitle": state.get("activeSectionTitle"),
                "currentTopic": state.get("currentTopic"),
            },
        }

    @staticmethod
    def empty_learning_state() -> Dict:
        return {
            "masteredPoints": [],
            "uncertainPoints": [],
            "misconceptions": [],
            "questionPatterns": [],
            "teacherInterventions": [],
            "nextLessonHints": [],
        }


class StudentLearningStateUpdater:
    def update(self, *, state: Dict, events: List[Dict], projection: Dict) -> Dict:
        learning = dict(state.get("studentLearningState") or MemoryRetriever.empty_learning_state())
        questions = [
            clean_text(event.get("text"), 220)
            for event in events
            if event.get("type") == "student_question"
        ]
        key_points = projection.get("keyPoints") or []
        homework = projection.get("homeworkCandidates") or []
        learning["uncertainPoints"] = unique_texts([*learning.get("uncertainPoints", []), *questions])
        learning["questionPatterns"] = unique_texts([*learning.get("questionPatterns", []), *questions])
        learning["masteredPoints"] = unique_texts([*learning.get("masteredPoints", []), *key_points])
        learning["nextLessonHints"] = unique_texts([*learning.get("nextLessonHints", []), *homework])
        return learning


def build_lesson_report_memory_context(lesson_id: int) -> Dict:
    memory = MemoryRetriever().retrieve(lesson_id=lesson_id, recent_turns=40, recent_events=80)
    outline = memory.get("projection", {}).get("minutesOutline") or []
    learning = memory.get("studentLearningState") or MemoryRetriever.empty_learning_state()
    events = memory.get("recentEvents") or []
    return {
        "episodes": [
            {
                "title": section.get("title"),
                "summary": section.get("summary"),
                "items": section.get("items") or [],
            }
            for section in outline
        ],
        "studentLearningState": learning,
        "studentQuestions": [
            event.get("text") for event in events if event.get("type") == "student_question"
        ],
        "homeworkEvents": [
            event.get("text") for event in events if event.get("type") == "homework_assigned"
        ],
    }
