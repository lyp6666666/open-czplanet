from __future__ import annotations

from app.asr.provider import get_realtime_asr_provider
from app.core.id_generator import new_task_id
from app.realtime.graph import REALTIME_GRAPH
from app.realtime.state_store import RealtimeStateStore
from app.repositories.realtime_repository import (
    LessonStageSummaryRepository,
    LiveLessonSessionRepository,
    TranscriptRepository,
)
from app.schemas.realtime import LiveLessonSessionCreateRequest, TranscriptSegmentInput
from app.storage.database import session_scope


class RealtimeLessonService:
    def create_session(self, lesson_id: int, request: LiveLessonSessionCreateRequest) -> dict:
        asr_provider = get_realtime_asr_provider()
        asr_enabled = bool(request.audioEnabled and asr_provider.available())
        llm_enabled = request.realtimeAiMode.upper() not in {"ASR_ONLY", "OFF"}
        session_id = new_task_id("lesson_ai")
        with session_scope() as session:
            LiveLessonSessionRepository(session).upsert(
                lesson_id=lesson_id,
                session_id=session_id,
                teacher_id=request.teacherId,
                student_id=request.studentId,
                subject=request.subject,
                grade=request.grade,
                course_type=request.courseType,
                mode=request.realtimeAiMode,
                asr_enabled=asr_enabled,
                llm_enabled=llm_enabled,
                status="ACTIVE",
            )
        RealtimeStateStore().create_session_state(
            lesson_id,
            {
                "lessonId": lesson_id,
                "sessionId": session_id,
                "mode": request.realtimeAiMode,
                "asrEnabled": asr_enabled,
                "llmEnabled": llm_enabled,
                "subject": request.subject,
                "grade": request.grade,
                "status": "ACTIVE",
            },
        )
        return {
            "lessonId": lesson_id,
            "sessionId": session_id,
            "asrEnabled": asr_enabled,
            "llmEnabled": llm_enabled,
            "mode": request.realtimeAiMode,
            "status": "ACTIVE",
        }

    def accept_segment(self, lesson_id: int, segment: TranscriptSegmentInput) -> dict:
        payload = segment.model_dump(mode="json")
        with session_scope() as session:
            TranscriptRepository(session).save_segment(
                lesson_id=lesson_id,
                seq=segment.seq,
                speaker=segment.speaker,
                start_ms=segment.startMs,
                end_ms=segment.endMs,
                text=segment.text,
                is_final=segment.isFinal,
            )
        graph_result = REALTIME_GRAPH.invoke({"lesson_id": lesson_id, "segment": payload})
        if graph_result.get("summary"):
            with session_scope() as session:
                LessonStageSummaryRepository(session).save(
                    lesson_id=lesson_id,
                    stage_index=int((graph_result.get("state") or {}).get("lastLlmSegmentCount") or 0),
                    summary_json=graph_result["summary"],
                )
        return RealtimeStateStore().get_state(lesson_id)

    def get_state(self, lesson_id: int) -> dict:
        return RealtimeStateStore().get_state(lesson_id)

    def finalize(self, lesson_id: int) -> dict:
        state = RealtimeStateStore().finalize(lesson_id)
        with session_scope() as session:
            LiveLessonSessionRepository(session).update_status(lesson_id, "FINALIZED")
        return state
