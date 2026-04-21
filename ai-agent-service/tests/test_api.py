from __future__ import annotations

import importlib

from fastapi.testclient import TestClient


def _client(tmp_path, monkeypatch) -> TestClient:
    monkeypatch.setenv("AI_AGENT_DATABASE_URL", f"sqlite:///{tmp_path}/test.db")
    monkeypatch.setenv("AI_AGENT_USE_ASYNC_WORKER", "false")
    monkeypatch.setenv("AI_AGENT_LLM_PROVIDER", "template")

    import app.core.config as config
    import app.storage.database as database
    import app.main as main

    config.get_settings.cache_clear()
    importlib.reload(database)
    importlib.reload(main)
    return TestClient(main.app)


def test_lesson_report_task_generates_report(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    response = client.post(
        "/internal/ai/lessons/101/report-tasks",
        json={
            "teacherId": 1,
            "studentId": 2,
            "subject": "数学",
            "grade": "初二",
            "lessonTopic": "一次函数图像与性质",
            "teacherNotes": "讲解了一次函数的斜率、截距以及图像变化。",
            "studentPerformance": "学生听课认真，但应用题建模偏慢。",
            "homework": "完成讲义 P12-P14",
            "nextPlan": "下节课训练一次函数应用题。",
        },
    )
    assert response.status_code == 200
    task_id = response.json()["data"]["taskId"]

    task = client.get(f"/internal/ai/tasks/{task_id}").json()["data"]
    assert task["status"] == "SUCCESS"
    assert task["progress"] == 100

    report = client.get("/internal/ai/lessons/101/report").json()["data"]
    assert report["status"] == "WAITING_TEACHER_REVIEW"
    assert report["report"]["reportTitle"] == "初二数学课后反馈"
    assert report["report"]["needTeacherReview"] is True


def test_chat_summary_task_generates_summary(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    response = client.post(
        "/internal/ai/chat-rooms/202/summary-tasks",
        json={
            "triggeredBy": 1,
            "messages": [
                {
                    "messageId": 1,
                    "senderRole": "parent",
                    "senderName": "家长",
                    "content": "孩子初二，数学几何比较弱，想周末线下补课。",
                },
                {
                    "messageId": 2,
                    "senderRole": "teacher",
                    "senderName": "老师",
                    "content": "可以，建议先安排一次试听，我会看一下基础情况。",
                },
            ],
            "messageStartId": 1,
            "messageEndId": 2,
        },
    )
    assert response.status_code == 200
    task_id = response.json()["data"]["taskId"]

    task = client.get(f"/internal/ai/tasks/{task_id}").json()["data"]
    assert task["status"] == "SUCCESS"

    summary = client.get("/internal/ai/chat-rooms/202/summary").json()["data"]
    assert summary["summary"]["studentProfile"]["grade"] == "初二"
    assert summary["summary"]["studentProfile"]["subject"] == "数学"


def test_realtime_session_and_segment_flow(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)

    created = client.post(
        "/internal/ai/live-lessons/303/sessions",
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
    assert created.status_code == 200
    assert created.json()["data"]["asrEnabled"] is False

    state_after_segment = client.post(
        "/internal/ai/live-lessons/303/transcript-segments",
        json={
            "seq": 1,
            "speaker": "teacher",
            "startMs": 0,
            "endMs": 3000,
            "text": "这节课的重点是一次函数，作业是完成讲义P12-P14。",
            "isFinal": True,
        },
    )
    assert state_after_segment.status_code == 200
    state = state_after_segment.json()["data"]
    assert state["lessonId"] == 303
    assert state["segmentCount"] >= 1
    assert state["status"] == "ACTIVE"

    finalized = client.post("/internal/ai/live-lessons/303/finalize")
    assert finalized.status_code == 200
    assert finalized.json()["data"]["status"] == "FINALIZED"
