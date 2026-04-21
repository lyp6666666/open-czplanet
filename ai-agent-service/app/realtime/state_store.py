from __future__ import annotations

import json
import time
from typing import Dict, List, Optional

from app.core.config import get_settings
from app.queue.redis_queue import get_redis


def _key(lesson_id: int, suffix: str) -> str:
    return f"ai:lesson:{lesson_id}:{suffix}"


class RealtimeStateStore:
    def __init__(self):
        self.redis = get_redis()
        self.settings = get_settings()

    def create_session_state(self, lesson_id: int, state: Dict) -> None:
        state = dict(state)
        state.setdefault("segmentCount", 0)
        state.setdefault("studentQuestions", [])
        state.setdefault("homeworkCandidates", [])
        state.setdefault("keyPoints", [])
        state.setdefault("status", "ACTIVE")
        self.redis.set(_key(lesson_id, "state"), json.dumps(state, ensure_ascii=False))

    def get_state(self, lesson_id: int) -> Dict:
        raw = self.redis.get(_key(lesson_id, "state"))
        if not raw:
            return {}
        if isinstance(raw, bytes):
            raw = raw.decode("utf-8")
        return json.loads(raw)

    def update_state(self, lesson_id: int, patch: Dict) -> Dict:
        state = self.get_state(lesson_id)
        state.update(patch)
        self.redis.set(_key(lesson_id, "state"), json.dumps(state, ensure_ascii=False))
        return state

    def append_segment(self, lesson_id: int, segment: Dict) -> int:
        key = _key(lesson_id, "transcript")
        self.redis.rpush(key, json.dumps(segment, ensure_ascii=False))
        self.redis.ltrim(key, -self.settings.realtime_transcript_buffer_size, -1)
        state = self.get_state(lesson_id)
        count = int(state.get("segmentCount") or 0) + 1
        state["segmentCount"] = count
        self.redis.set(_key(lesson_id, "state"), json.dumps(state, ensure_ascii=False))
        return count

    def recent_segments(self, lesson_id: int, limit: int = 80) -> List[Dict]:
        raw_items = self.redis.lrange(_key(lesson_id, "transcript"), -limit, -1)
        result = []
        for raw in raw_items:
            if isinstance(raw, bytes):
                raw = raw.decode("utf-8")
            result.append(json.loads(raw))
        return result

    def merge_signals(self, lesson_id: int, signals: Dict[str, List[str]]) -> Dict:
        state = self.get_state(lesson_id)
        for field in ["studentQuestions", "homeworkCandidates", "keyPoints"]:
            existing = list(state.get(field) or [])
            for item in signals.get(field) or []:
                if item not in existing:
                    existing.append(item)
            state[field] = existing[-20:]
        topics = signals.get("topics") or []
        if topics:
            state["currentTopic"] = topics[-1]
        self.redis.set(_key(lesson_id, "state"), json.dumps(state, ensure_ascii=False))
        return state

    def should_run_llm(self, lesson_id: int) -> bool:
        state = self.get_state(lesson_id)
        if not state.get("llmEnabled", True):
            return False
        last_ts = int(state.get("lastLlmSummaryTs") or 0)
        if int(time.time()) - last_ts < self.settings.realtime_stage_summary_interval_seconds:
            return False
        last_count = int(state.get("lastLlmSegmentCount") or 0)
        count = int(state.get("segmentCount") or 0)
        return count - last_count >= self.settings.realtime_stage_min_segments

    def mark_llm_ran(self, lesson_id: int, summary: Dict) -> Dict:
        state = self.get_state(lesson_id)
        state["lastLlmSummaryTs"] = int(time.time())
        state["lastLlmSegmentCount"] = int(state.get("segmentCount") or 0)
        state["latestStageSummary"] = summary.get("stageSummary")
        state["currentTopic"] = summary.get("currentTopic") or state.get("currentTopic")
        for field in ["studentQuestions", "homeworkCandidates", "keyPoints"]:
            values = summary.get(field) or []
            if values:
                state[field] = values
        self.redis.set(_key(lesson_id, "state"), json.dumps(state, ensure_ascii=False))
        return state

    def finalize(self, lesson_id: int) -> Dict:
        return self.update_state(lesson_id, {"status": "FINALIZED", "finalizedAt": int(time.time())})

    def publish_event(self, lesson_id: int, event: Dict) -> None:
        self.redis.publish(_key(lesson_id, "events"), json.dumps(event, ensure_ascii=False))

    def event_channel(self, lesson_id: int) -> str:
        return _key(lesson_id, "events")
