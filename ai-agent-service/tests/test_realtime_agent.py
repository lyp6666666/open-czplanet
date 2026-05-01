from __future__ import annotations

import importlib
from pathlib import Path

from fastapi.testclient import TestClient


def _client(tmp_path, monkeypatch, *, agent_enabled: bool = True) -> TestClient:
    monkeypatch.setenv("AI_AGENT_DATABASE_URL", f"sqlite:///{tmp_path}/test.db")
    monkeypatch.setenv("AI_AGENT_USE_ASYNC_WORKER", "false")
    monkeypatch.setenv("AI_AGENT_LLM_PROVIDER", "template")
    monkeypatch.setenv("AI_AGENT_REDIS_URL", "memory://local")
    monkeypatch.setenv("AI_AGENT_REALTIME_AGENT_ENABLED", "true" if agent_enabled else "false")
    monkeypatch.setenv("AI_AGENT_REALTIME_AGENT_FIRST_SUMMARY_MIN_TURNS", "3")
    monkeypatch.setenv("AI_AGENT_REALTIME_STAGE_SUMMARY_INTERVAL_SECONDS", "300")
    monkeypatch.setenv("AI_AGENT_REALTIME_STAGE_MIN_SEGMENTS", "8")

    import app.core.config as config
    import app.queue.redis_queue as redis_queue
    import app.storage.database as database
    import app.realtime.graph as graph
    import app.main as main

    config.get_settings.cache_clear()
    importlib.reload(redis_queue)
    importlib.reload(database)
    importlib.reload(graph)
    importlib.reload(main)
    return TestClient(main.app)


def _create_session(client: TestClient, lesson_id: int = 901):
    response = client.post(
        f"/internal/ai/live-lessons/{lesson_id}/sessions",
        json={
            "teacherId": 1,
            "studentId": 2,
            "subject": "数学",
            "grade": "初二",
            "courseType": "ONLINE_FORMAL",
            "audioEnabled": False,
            "realtimeAiMode": "LIGHT",
        },
    )
    assert response.status_code == 200


def _post_segment(client: TestClient, lesson_id: int, seq: int, speaker: str, text: str):
    response = client.post(
        f"/internal/ai/live-lessons/{lesson_id}/transcript-segments",
        json={
            "seq": seq,
            "speaker": speaker,
            "startMs": seq * 1000,
            "endMs": seq * 1000 + 900,
            "text": text,
            "isFinal": True,
        },
    )
    assert response.status_code == 200
    return response.json()["data"]


def test_realtime_agent_first_summary_generates_patch_and_keeps_state_contract(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    lesson_id = 901
    _create_session(client, lesson_id)

    _post_segment(client, lesson_id, 1, "teacher", "我们先回顾上节课的一次函数。")
    _post_segment(client, lesson_id, 2, "teacher", "k 会影响一次函数图像的倾斜程度。")
    state = _post_segment(client, lesson_id, 3, "student", "为什么 k 越大图像越陡？")

    assert state["lessonId"] == lesson_id
    assert state["segmentCount"] == 3
    assert state["minutesOutline"]
    assert state["activeSectionTitle"]
    assert "为什么 k 越大图像越陡" in "".join(state["studentQuestions"])
    raw = state["rawState"]
    assert raw["turnCount"] >= 3
    assert raw["eventCount"] >= 2
    assert raw["summaryVersion"] >= 1
    assert raw["lastPatchId"]
    assert "first_summary" in raw["lastTriggerReasons"]


def test_realtime_agent_classroom_management_does_not_call_summary(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    lesson_id = 902
    _create_session(client, lesson_id)

    _post_segment(client, lesson_id, 1, "teacher", "能听到吗？")
    _post_segment(client, lesson_id, 2, "student", "可以。")
    state = _post_segment(client, lesson_id, 3, "teacher", "你把摄像头打开一下。")

    assert state["segmentCount"] == 3
    assert state["minutesOutline"] == []
    raw = state["rawState"]
    assert raw["eventCount"] >= 1
    assert raw["lastOrchestratorDecision"] in {"extract_only", "skip"}
    assert not raw.get("lastPatchId")


def test_realtime_agent_guard_rejects_patch_without_polluting_projection(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    lesson_id = 903
    _create_session(client, lesson_id)

    import app.realtime.agent_llm as agent_llm

    original_write = agent_llm.RealtimeAgentLLMClient.generate_summary_patch

    def bad_write(self, *, context, decision):
        return {
            "patchId": "patch-bad",
            "patchType": "append_item",
            "targetSectionId": "section-1",
            "appendItems": [{"title": "斜率重点", "detail": "k 决定图像倾斜。"}],
        }

    monkeypatch.setattr(agent_llm.RealtimeAgentLLMClient, "generate_summary_patch", bad_write)
    try:
        _post_segment(client, lesson_id, 1, "teacher", "我们先回顾一次函数。")
        _post_segment(client, lesson_id, 2, "teacher", "这里的重点是 k 决定图像的倾斜程度。")
        state = _post_segment(client, lesson_id, 3, "student", "为什么 k 越大图像越陡？")
    finally:
        monkeypatch.setattr(agent_llm.RealtimeAgentLLMClient, "generate_summary_patch", original_write)

    assert state["minutesOutline"] == []
    raw = state["rawState"]
    assert raw["guardRejectedCount"] >= 1
    assert "missing_evidence" in raw["lastGuardReasons"]
    assert raw["lastRejectedPatchId"] == "patch-bad"


def test_realtime_agent_feature_flag_off_uses_legacy_flow(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch, agent_enabled=False)
    lesson_id = 904
    _create_session(client, lesson_id)

    state = None
    for seq in range(1, 9):
        state = _post_segment(
            client,
            lesson_id,
            seq,
            "teacher",
            f"这节课的重点是一次函数第{seq}段，作业是完成讲义P12-P14。",
        )

    assert state is not None
    assert state["segmentCount"] == 8
    assert state["minutesOutline"]
    raw = state["rawState"]
    assert raw.get("turnCount", 0) == 0
    assert raw.get("lastPatchId") is None


def test_realtime_agent_metrics_expose_trigger_patch_and_llm_counts(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    lesson_id = 905
    _create_session(client, lesson_id)

    _post_segment(client, lesson_id, 1, "teacher", "我们先回顾上节课的一次函数。")
    _post_segment(client, lesson_id, 2, "teacher", "k 会影响一次函数图像的倾斜程度。")
    _post_segment(client, lesson_id, 3, "student", "为什么 k 越大图像越陡？")

    metrics = client.get("/metrics")
    assert metrics.status_code == 200
    body = metrics.text
    assert 'realtime_agent_trigger_total{reason="first_summary"}' in body
    assert 'realtime_agent_patch_total{type="append_section",status="accepted"}' in body
    assert 'realtime_agent_llm_calls_total{agent="summary_patch_writer",model_tier="strong"}' in body
    assert 'realtime_agent_first_summary_latency_seconds_count{bucket="all"}' in body


def test_lesson_report_uses_realtime_memory_when_available(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    lesson_id = 906
    _create_session(client, lesson_id)

    _post_segment(client, lesson_id, 1, "teacher", "我们先回顾一次函数。")
    _post_segment(client, lesson_id, 2, "teacher", "这里的重点是 k 决定图像的倾斜程度。")
    _post_segment(client, lesson_id, 3, "student", "为什么 k 越大图像越陡？")

    response = client.post(
        f"/internal/ai/lessons/{lesson_id}/report-tasks",
        json={
            "teacherId": 1,
            "studentId": 2,
            "subject": "数学",
            "grade": "初二",
            "lessonTopic": "一次函数",
            "teacherNotes": "老师课堂记录待补充。",
            "studentPerformance": None,
            "homework": None,
            "nextPlan": "下节课继续训练一次函数应用题。",
        },
    )
    assert response.status_code == 200

    report = client.get(f"/internal/ai/lessons/{lesson_id}/report").json()["data"]["report"]
    assert "实时课堂阶段记忆" in report["parentSummary"]
    assert "一次函数" in report["parentSummary"]


def test_offline_realtime_agent_evaluation_quantifies_expected_effects(monkeypatch):
    from scripts.evaluate_realtime_agent import DEFAULT_FIXTURE, evaluate

    result = evaluate(Path(DEFAULT_FIXTURE))

    assert result["newAgent"]["firstSummarySegment"] is not None
    assert result["improvements"]["firstSummaryEarlier"] is True
    assert result["improvements"]["historyPreserved"] is True
    assert result["newAgent"]["duplicateItemRate"] <= 0.25
    assert result["newAgent"]["studentQuestionCount"] >= 1
