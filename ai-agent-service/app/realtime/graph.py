from __future__ import annotations

from typing import Dict, List, TypedDict

from langgraph.graph import END, StateGraph

from app.realtime.llm import RealtimeLessonLLM
from app.realtime.rule_engine import extract_realtime_signals
from app.realtime.state_store import RealtimeStateStore


class RealtimeGraphState(TypedDict, total=False):
    lesson_id: int
    segment: Dict
    signals: Dict
    state: Dict
    recent_segments: List[Dict]
    summary: Dict
    should_llm: bool


def _extract_signals(state: RealtimeGraphState) -> RealtimeGraphState:
    text = state["segment"].get("text", "")
    state["signals"] = extract_realtime_signals(text)
    return state


def _store_segment(state: RealtimeGraphState) -> RealtimeGraphState:
    store = RealtimeStateStore()
    lesson_id = state["lesson_id"]
    store.append_segment(lesson_id, state["segment"])
    merged = store.merge_signals(lesson_id, state.get("signals") or {})
    state["state"] = merged
    state["should_llm"] = store.should_run_llm(lesson_id)
    return state


def _load_recent_segments(state: RealtimeGraphState) -> RealtimeGraphState:
    store = RealtimeStateStore()
    state["recent_segments"] = store.recent_segments(state["lesson_id"], limit=80)
    return state


def _summarize(state: RealtimeGraphState) -> RealtimeGraphState:
    summary = RealtimeLessonLLM().summarize_stage(
        state["lesson_id"], state.get("state") or {}, state.get("recent_segments") or []
    )
    state["summary"] = summary
    state["state"] = RealtimeStateStore().mark_llm_ran(state["lesson_id"], summary)
    return state


def _publish(state: RealtimeGraphState) -> RealtimeGraphState:
    store = RealtimeStateStore()
    lesson_id = state["lesson_id"]
    store.publish_event(
        lesson_id,
        {"type": "transcript.segment", "payload": state["segment"]},
    )
    if state.get("summary"):
        store.publish_event(
            lesson_id,
            {"type": "lesson.insight", "payload": state["summary"]},
        )
    return state


def _route_after_store(state: RealtimeGraphState) -> str:
    return "load_recent_segments" if state.get("should_llm") else "publish"


def build_realtime_graph():
    graph = StateGraph(RealtimeGraphState)
    graph.add_node("extract_signals", _extract_signals)
    graph.add_node("store_segment", _store_segment)
    graph.add_node("load_recent_segments", _load_recent_segments)
    graph.add_node("summarize", _summarize)
    graph.add_node("publish", _publish)
    graph.set_entry_point("extract_signals")
    graph.add_edge("extract_signals", "store_segment")
    graph.add_conditional_edges(
        "store_segment",
        _route_after_store,
        {"load_recent_segments": "load_recent_segments", "publish": "publish"},
    )
    graph.add_edge("load_recent_segments", "summarize")
    graph.add_edge("summarize", "publish")
    graph.add_edge("publish", END)
    return graph.compile()


REALTIME_GRAPH = build_realtime_graph()
