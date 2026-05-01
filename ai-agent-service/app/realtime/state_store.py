from __future__ import annotations

import json
import time
from typing import Dict, List

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
        state.setdefault("turnCount", 0)
        state.setdefault("eventCount", 0)
        state.setdefault("memoryVersion", 0)
        state.setdefault("summaryVersion", 0)
        state.setdefault("studentQuestions", [])
        state.setdefault("homeworkCandidates", [])
        state.setdefault("keyPoints", [])
        state.setdefault("minutesOutline", [])
        state.setdefault("activeSectionTitle", None)
        state.setdefault("guardRejectedCount", 0)
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

    def append_turn(self, lesson_id: int, turn: Dict) -> int:
        key = _key(lesson_id, "turns")
        self.redis.rpush(key, json.dumps(turn, ensure_ascii=False))
        self.redis.ltrim(key, -max(self.settings.realtime_transcript_buffer_size, 100), -1)
        state = self.get_state(lesson_id)
        count = int(state.get("turnCount") or 0) + 1
        state["turnCount"] = count
        state["memoryVersion"] = int(state.get("memoryVersion") or 0) + 1
        state["openTurn"] = turn
        self.redis.set(_key(lesson_id, "state"), json.dumps(state, ensure_ascii=False))
        return count

    def append_events(self, lesson_id: int, events: List[Dict]) -> int:
        if not events:
            return int((self.get_state(lesson_id) or {}).get("eventCount") or 0)
        key = _key(lesson_id, "teaching_events")
        for event in events:
            self.redis.rpush(key, json.dumps(event, ensure_ascii=False))
        self.redis.ltrim(key, -max(self.settings.realtime_transcript_buffer_size, 100), -1)
        state = self.get_state(lesson_id)
        count = int(state.get("eventCount") or 0) + len(events)
        state["eventCount"] = count
        state["memoryVersion"] = int(state.get("memoryVersion") or 0) + 1
        self.redis.set(_key(lesson_id, "state"), json.dumps(state, ensure_ascii=False))
        return count

    def recent_segments(self, lesson_id: int, limit: int = 80) -> List[Dict]:
        return self._recent_json_list(_key(lesson_id, "transcript"), limit)

    def recent_turns(self, lesson_id: int, limit: int = 12) -> List[Dict]:
        return self._recent_json_list(_key(lesson_id, "turns"), limit)

    def recent_events(self, lesson_id: int, limit: int = 20) -> List[Dict]:
        return self._recent_json_list(_key(lesson_id, "teaching_events"), limit)

    def _recent_json_list(self, key: str, limit: int) -> List[Dict]:
        raw_items = self.redis.lrange(key, -limit, -1)
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
        minutes_outline = _normalize_minutes_outline(
            summary.get("minutesOutline") or summary.get("outline") or [],
            segment_count=int(state.get("segmentCount") or 0),
            fallback_title=state.get("currentTopic") or summary.get("currentTopic"),
        )
        if minutes_outline:
            state["minutesOutline"] = minutes_outline[-12:]
            state["activeSectionTitle"] = minutes_outline[-1].get("title")
        for field in ["studentQuestions", "homeworkCandidates", "keyPoints"]:
            values = summary.get(field) or []
            if values:
                state[field] = values
        self.redis.set(_key(lesson_id, "state"), json.dumps(state, ensure_ascii=False))
        return state

    def apply_summary_projection(
        self,
        lesson_id: int,
        projection: Dict,
        *,
        patch: Dict,
        trigger_reasons: List[str],
        guard: Dict,
    ) -> Dict:
        state = self.get_state(lesson_id)
        state["minutesOutline"] = projection.get("minutesOutline") or []
        state["studentQuestions"] = projection.get("studentQuestions") or []
        state["homeworkCandidates"] = projection.get("homeworkCandidates") or []
        state["keyPoints"] = projection.get("keyPoints") or []
        state["latestStageSummary"] = projection.get("latestStageSummary")
        state["activeSectionTitle"] = projection.get("activeSectionTitle")
        state["currentTopic"] = projection.get("currentTopic") or state.get("currentTopic")
        state["summaryVersion"] = int(state.get("summaryVersion") or 0) + 1
        state["lastLlmSummaryTs"] = int(time.time())
        state["lastLlmSegmentCount"] = int(state.get("segmentCount") or 0)
        state["lastStrongPatchTs"] = int(time.time())
        if not state.get("firstSummaryAt"):
            state["firstSummaryAt"] = int(time.time())
        state["lastPatchId"] = patch.get("patchId")
        state["lastPatchType"] = patch.get("patchType")
        state["lastTriggerReasons"] = trigger_reasons
        state["lastGuardStatus"] = guard.get("severity") or "pass"
        state["guardAccepted"] = guard.get("accepted")
        if projection.get("studentLearningState"):
            state["studentLearningState"] = projection.get("studentLearningState")
        self.redis.set(_key(lesson_id, "state"), json.dumps(state, ensure_ascii=False))
        return state

    def update_learning_state(self, lesson_id: int, learning_state: Dict) -> Dict:
        state = self.get_state(lesson_id)
        state["studentLearningState"] = learning_state
        state["memoryVersion"] = int(state.get("memoryVersion") or 0) + 1
        self.redis.set(_key(lesson_id, "state"), json.dumps(state, ensure_ascii=False))
        return state

    def record_guard_rejection(
        self,
        lesson_id: int,
        *,
        patch: Dict,
        trigger_reasons: List[str],
        guard: Dict,
    ) -> Dict:
        state = self.get_state(lesson_id)
        state["guardRejectedCount"] = int(state.get("guardRejectedCount") or 0) + 1
        state["lastRejectedPatchId"] = patch.get("patchId")
        state["lastTriggerReasons"] = trigger_reasons
        state["lastGuardReasons"] = guard.get("reasons") or []
        state["lastGuardStatus"] = guard.get("severity") or "reject"
        self.redis.set(_key(lesson_id, "state"), json.dumps(state, ensure_ascii=False))
        return state

    def record_orchestrator_decision(self, lesson_id: int, decision: Dict) -> Dict:
        state = self.get_state(lesson_id)
        state["lastOrchestratorDecision"] = decision.get("decision")
        state["lastTriggerReasons"] = decision.get("triggerReasons") or []
        if decision.get("suppressedTriggerReasons"):
            state["suppressedTriggerReasons"] = decision.get("suppressedTriggerReasons")
        self.redis.set(_key(lesson_id, "state"), json.dumps(state, ensure_ascii=False))
        return state

    def finalize(self, lesson_id: int) -> Dict:
        return self.update_state(lesson_id, {"status": "FINALIZED", "finalizedAt": int(time.time())})

    def publish_event(self, lesson_id: int, event: Dict) -> None:
        self.redis.publish(_key(lesson_id, "events"), json.dumps(event, ensure_ascii=False))

    def event_channel(self, lesson_id: int) -> str:
        return _key(lesson_id, "events")


def _clean_text(value: object, limit: int) -> str:
    text = str(value or "").strip()
    return text[:limit]


def _normalize_minutes_outline(items: object, *, segment_count: int, fallback_title: object = None) -> List[Dict]:
    if not isinstance(items, list):
        return []
    normalized: List[Dict] = []
    now = int(time.time())
    for index, raw in enumerate(items[-12:]):
        if not isinstance(raw, dict):
            continue
        title = _clean_text(raw.get("title") or fallback_title or f"课堂阶段 {index + 1}", 40)
        summary = _clean_text(raw.get("summary") or raw.get("stageSummary") or title, 260)
        raw_items = raw.get("items") if isinstance(raw.get("items"), list) else []
        section_items: List[Dict] = []
        for raw_item in raw_items[:8]:
            if not isinstance(raw_item, dict):
                continue
            item_title = _clean_text(raw_item.get("title") or raw_item.get("heading"), 40)
            item_detail = _clean_text(raw_item.get("detail") or raw_item.get("content"), 240)
            if item_title and item_detail:
                section_items.append({"title": item_title, "detail": item_detail})
        section_id = _clean_text(raw.get("id") or f"section-{index + 1}", 64)
        normalized.append(
            {
                "id": section_id,
                "title": title,
                "summary": summary,
                "startSegment": int(raw.get("startSegment") or raw.get("start_segment") or 0),
                "endSegment": int(raw.get("endSegment") or raw.get("end_segment") or segment_count),
                "updatedAt": int(raw.get("updatedAt") or raw.get("updated_at") or now),
                "items": section_items,
            }
        )
    return normalized
