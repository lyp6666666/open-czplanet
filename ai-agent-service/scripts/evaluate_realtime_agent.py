from __future__ import annotations

import argparse
import importlib
import json
import os
import tempfile
from pathlib import Path
from typing import Dict, List


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_FIXTURE = ROOT / "tests" / "fixtures" / "realtime_math_function_lesson.json"


def _bootstrap(*, agent_enabled: bool, database_path: str):
    os.environ["AI_AGENT_DATABASE_URL"] = f"sqlite:///{database_path}"
    os.environ["AI_AGENT_USE_ASYNC_WORKER"] = "false"
    os.environ["AI_AGENT_LLM_PROVIDER"] = "template"
    os.environ["AI_AGENT_REDIS_URL"] = "memory://local"
    os.environ["AI_AGENT_REALTIME_AGENT_ENABLED"] = "true" if agent_enabled else "false"
    os.environ["AI_AGENT_REALTIME_AGENT_FIRST_SUMMARY_MIN_TURNS"] = "3"
    os.environ["AI_AGENT_REALTIME_STAGE_SUMMARY_INTERVAL_SECONDS"] = "300"
    os.environ["AI_AGENT_REALTIME_STAGE_MIN_SEGMENTS"] = "8"

    import app.core.config as config
    import app.queue.redis_queue as redis_queue
    import app.storage.database as database
    import app.realtime.graph as graph

    config.get_settings.cache_clear()
    importlib.reload(redis_queue)
    importlib.reload(database)
    importlib.reload(graph)
    database.init_database()

    from app.schemas.realtime import LiveLessonSessionCreateRequest, TranscriptSegmentInput
    from app.services.realtime_service import RealtimeLessonService

    return RealtimeLessonService(), LiveLessonSessionCreateRequest, TranscriptSegmentInput


def _run_fixture(*, fixture: List[Dict], agent_enabled: bool, lesson_id: int) -> Dict:
    with tempfile.TemporaryDirectory() as tmpdir:
        service, SessionRequest, SegmentInput = _bootstrap(
            agent_enabled=agent_enabled,
            database_path=str(Path(tmpdir) / "eval.db"),
        )
        service.create_session(
            lesson_id,
            SessionRequest(
                teacherId=1,
                studentId=2,
                subject="数学",
                grade="初二",
                courseType="ONLINE_FORMAL",
                audioEnabled=False,
                realtimeAiMode="LIGHT",
            ),
        )
        first_summary_segment = None
        previous_sections: List[Dict] = []
        history_preserved = True
        for item in fixture:
            state = service.accept_segment(
                lesson_id,
                SegmentInput(
                    seq=int(item["seq"]),
                    speaker=str(item["speaker"]),
                    startMs=int(item["seq"]) * 1000,
                    endMs=int(item["seq"]) * 1000 + 900,
                    text=str(item["text"]),
                    isFinal=True,
                ),
            )
            outline = state.get("minutesOutline") or []
            if outline and first_summary_segment is None:
                first_summary_segment = int(item["seq"])
            if previous_sections and outline:
                previous_ids = {section.get("id") for section in previous_sections}
                current_ids = {section.get("id") for section in outline}
                if not previous_ids.issubset(current_ids):
                    history_preserved = False
            if outline:
                previous_sections = outline

        final_state = service.get_state(lesson_id)
        outline = final_state.get("minutesOutline") or []
        return {
            "agentEnabled": agent_enabled,
            "firstSummarySegment": first_summary_segment,
            "sectionCount": len(outline),
            "duplicateItemRate": _duplicate_item_rate(outline),
            "historyPreserved": history_preserved,
            "studentQuestionCount": len(final_state.get("studentQuestions") or []),
            "homeworkCandidateCount": len(final_state.get("homeworkCandidates") or []),
            "summaryVersion": final_state.get("summaryVersion") or 0,
            "lastTriggerReasons": final_state.get("lastTriggerReasons") or [],
        }


def _duplicate_item_rate(outline: List[Dict]) -> float:
    details: List[str] = []
    for section in outline:
        for item in section.get("items") or []:
            detail = str(item.get("detail") or "").strip()
            if detail:
                details.append(detail)
    if not details:
        return 0.0
    duplicate_count = len(details) - len(set(details))
    return round(duplicate_count / len(details), 4)


def evaluate(fixture_path: Path = DEFAULT_FIXTURE) -> Dict:
    fixture = json.loads(fixture_path.read_text(encoding="utf-8"))
    new_result = _run_fixture(fixture=fixture, agent_enabled=True, lesson_id=5001)
    legacy_result = _run_fixture(fixture=fixture, agent_enabled=False, lesson_id=5002)
    return {
        "fixture": str(fixture_path),
        "newAgent": new_result,
        "legacy": legacy_result,
        "improvements": {
            "firstSummaryEarlier": (
                new_result["firstSummarySegment"] is not None
                and (
                    legacy_result["firstSummarySegment"] is None
                    or new_result["firstSummarySegment"] < legacy_result["firstSummarySegment"]
                )
            ),
            "historyPreserved": new_result["historyPreserved"],
            "duplicateItemRate": new_result["duplicateItemRate"],
        },
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--fixture", default=str(DEFAULT_FIXTURE))
    args = parser.parse_args()
    print(json.dumps(evaluate(Path(args.fixture)), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
