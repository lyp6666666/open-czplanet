from __future__ import annotations

from typing import Dict, List, TypedDict

from langgraph.graph import END, StateGraph

from app.core.config import get_settings
from app.observability import (
    record_realtime_agent_guard_reject,
    record_realtime_agent_first_summary_latency,
    record_realtime_agent_patch,
    record_realtime_agent_trigger,
)
from app.realtime.agent_llm import RealtimeAgentLLMClient
from app.realtime.context import ContextBuilder
from app.realtime.events import TeachingEventExtractor
from app.realtime.llm import RealtimeLessonLLM
from app.realtime.memory import StudentLearningStateUpdater
from app.realtime.orchestrator import RealtimeOrchestrator
from app.realtime.patches import PatchApplier, QualityGuard
from app.realtime.rule_engine import extract_realtime_signals
from app.realtime.stage_tracker import TopicStageTracker
from app.realtime.state_store import RealtimeStateStore
from app.realtime.trace import trace_realtime_agent
from app.realtime.turns import TurnAggregator


class RealtimeGraphState(TypedDict, total=False):
    lesson_id: int
    segment: Dict
    signals: Dict
    state: Dict
    turn_result: Dict
    turn: Dict
    events: List[Dict]
    decision: Dict
    stage_decision: Dict
    context: Dict
    patch: Dict
    guard: Dict
    projection: Dict
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
    if not get_settings().realtime_agent_enabled:
        state["should_llm"] = store.should_run_llm(lesson_id)
    return state


def _normalize_turn(state: RealtimeGraphState) -> RealtimeGraphState:
    store = RealtimeStateStore()
    lesson_id = state["lesson_id"]
    current = store.get_state(lesson_id)
    result = TurnAggregator().normalize(segment=state["segment"], state=current)
    state["turn_result"] = result
    turn = result.get("turn") or {}
    state["turn"] = turn
    store.append_turn(lesson_id, turn)
    state["state"] = store.get_state(lesson_id)
    return state


def _extract_events(state: RealtimeGraphState) -> RealtimeGraphState:
    events = TeachingEventExtractor().extract(
        turn=state.get("turn") or {},
        state=state.get("state") or {},
    )
    state["events"] = events
    store = RealtimeStateStore()
    store.append_events(state["lesson_id"], events)
    state["state"] = store.get_state(state["lesson_id"])
    return state


def _orchestrate(state: RealtimeGraphState) -> RealtimeGraphState:
    stage_decision = TopicStageTracker().track(
        state=state.get("state") or {},
        events=state.get("events") or [],
    )
    state["stage_decision"] = stage_decision
    decision = RealtimeOrchestrator().decide(
        state=state.get("state") or {},
        events=state.get("events") or [],
        stage_decision=stage_decision,
    )
    state["decision"] = decision
    record_realtime_agent_trigger(decision.get("triggerReasons") or [])
    segment = state.get("segment") or {}
    trace_realtime_agent(
        "orchestrator_decision",
        lessonId=state["lesson_id"],
        segmentRange=[segment.get("seq"), segment.get("seq")],
        decision=decision.get("decision"),
        triggerReasons=decision.get("triggerReasons") or [],
        modelTier=decision.get("modelTier"),
        stageDecision=stage_decision.get("stageDecision"),
        stageType=stage_decision.get("stageType"),
    )
    state["state"] = RealtimeStateStore().record_orchestrator_decision(
        state["lesson_id"], decision
    )
    return state


def _build_context(state: RealtimeGraphState) -> RealtimeGraphState:
    state["context"] = ContextBuilder().build(
        lesson_id=state["lesson_id"],
        decision=state.get("decision") or {},
    )
    return state


def _generate_patch(state: RealtimeGraphState) -> RealtimeGraphState:
    state["patch"] = RealtimeAgentLLMClient().generate_summary_patch(
        context=state.get("context") or {},
        decision=state.get("decision") or {},
    )
    patch = state.get("patch") or {}
    trace_realtime_agent(
        "summary_patch_generated",
        lessonId=state["lesson_id"],
        patchId=patch.get("patchId"),
        patchType=patch.get("patchType"),
        triggerReasons=(state.get("decision") or {}).get("triggerReasons") or [],
    )
    return state


def _quality_guard(state: RealtimeGraphState) -> RealtimeGraphState:
    state["guard"] = QualityGuard().validate(
        patch=state.get("patch") or {},
        context=state.get("context") or {},
    )
    return state


def _apply_patch(state: RealtimeGraphState) -> RealtimeGraphState:
    store = RealtimeStateStore()
    lesson_id = state["lesson_id"]
    patch = state.get("patch") or {}
    guard = state.get("guard") or {}
    trigger_reasons = (state.get("decision") or {}).get("triggerReasons") or []
    if not guard.get("accepted"):
        record_realtime_agent_patch(str(patch.get("patchType") or "unknown"), "rejected")
        record_realtime_agent_guard_reject(guard.get("reasons") or [])
        state["state"] = store.record_guard_rejection(
            lesson_id,
            patch=patch,
            trigger_reasons=trigger_reasons,
            guard=guard,
        )
        trace_realtime_agent(
            "summary_patch_rejected",
            lessonId=lesson_id,
            patchId=patch.get("patchId"),
            patchType=patch.get("patchType"),
            triggerReasons=trigger_reasons,
            guardStatus=guard.get("severity"),
            guardReasons=guard.get("reasons") or [],
        )
        return state
    projection = PatchApplier().apply(state=store.get_state(lesson_id), patch=patch)
    record_realtime_agent_patch(str(patch.get("patchType") or "unknown"), "accepted")
    current_state = store.get_state(lesson_id)
    if not current_state.get("firstSummaryAt"):
        created_at = int(current_state.get("createdAtTs") or current_state.get("lastAudioAt") or 0)
        if created_at:
            import time

            record_realtime_agent_first_summary_latency(max(0, int(time.time()) - created_at))
    learning_state = StudentLearningStateUpdater().update(
        state=store.get_state(lesson_id),
        events=(state.get("context") or {}).get("recentEvents") or [],
        projection=projection,
    )
    projection["studentLearningState"] = learning_state
    state["projection"] = projection
    state["state"] = store.apply_summary_projection(
        lesson_id,
        projection,
        patch=patch,
        trigger_reasons=trigger_reasons,
        guard=guard,
    )
    trace_realtime_agent(
        "summary_patch_applied",
        lessonId=lesson_id,
        patchId=patch.get("patchId"),
        patchType=patch.get("patchType"),
        triggerReasons=trigger_reasons,
        guardStatus=guard.get("severity"),
        sectionCount=len(projection.get("minutesOutline") or []),
    )
    state["summary"] = {
        "patch": patch,
        "projection": projection,
        "stageSummary": projection.get("latestStageSummary"),
        "minutesOutline": projection.get("minutesOutline"),
        "studentQuestions": projection.get("studentQuestions"),
        "homeworkCandidates": projection.get("homeworkCandidates"),
        "keyPoints": projection.get("keyPoints"),
        "provider": "agent_patch",
    }
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
    if state.get("events"):
        store.publish_event(
            lesson_id,
            {"type": "lesson.events", "payload": state["events"]},
        )
    return state


def _route_after_store(state: RealtimeGraphState) -> str:
    if get_settings().realtime_agent_enabled:
        return "normalize_turn"
    return "load_recent_segments" if state.get("should_llm") else "publish"


def _route_after_orchestrate(state: RealtimeGraphState) -> str:
    decision = (state.get("decision") or {}).get("decision")
    return "build_context" if decision == "generate_patch" else "publish"


def build_realtime_graph():
    graph = StateGraph(RealtimeGraphState)
    graph.add_node("extract_signals", _extract_signals)
    graph.add_node("store_segment", _store_segment)
    graph.add_node("normalize_turn", _normalize_turn)
    graph.add_node("extract_events", _extract_events)
    graph.add_node("orchestrate", _orchestrate)
    graph.add_node("build_context", _build_context)
    graph.add_node("generate_patch", _generate_patch)
    graph.add_node("quality_guard", _quality_guard)
    graph.add_node("apply_patch", _apply_patch)
    graph.add_node("load_recent_segments", _load_recent_segments)
    graph.add_node("summarize", _summarize)
    graph.add_node("publish", _publish)
    graph.set_entry_point("extract_signals")
    graph.add_edge("extract_signals", "store_segment")
    graph.add_conditional_edges(
        "store_segment",
        _route_after_store,
        {
            "normalize_turn": "normalize_turn",
            "load_recent_segments": "load_recent_segments",
            "publish": "publish",
        },
    )
    graph.add_edge("normalize_turn", "extract_events")
    graph.add_edge("extract_events", "orchestrate")
    graph.add_conditional_edges(
        "orchestrate",
        _route_after_orchestrate,
        {"build_context": "build_context", "publish": "publish"},
    )
    graph.add_edge("build_context", "generate_patch")
    graph.add_edge("generate_patch", "quality_guard")
    graph.add_edge("quality_guard", "apply_patch")
    graph.add_edge("apply_patch", "publish")
    graph.add_edge("load_recent_segments", "summarize")
    graph.add_edge("summarize", "publish")
    graph.add_edge("publish", END)
    return graph.compile()


REALTIME_GRAPH = build_realtime_graph()
