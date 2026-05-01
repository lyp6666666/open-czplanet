from __future__ import annotations

from typing import Optional

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.storage.database import (
    AiLessonEpisode,
    AiLessonStageSummary,
    AiLessonSummaryPatch,
    AiLessonTeachingEvent,
    AiLessonTranscript,
    AiLessonTurn,
    AiLiveLessonSession,
)


class LiveLessonSessionRepository:
    def __init__(self, session: Session):
        self.session = session

    def get_by_lesson_id(self, lesson_id: int) -> Optional[AiLiveLessonSession]:
        return self.session.scalar(
            select(AiLiveLessonSession).where(AiLiveLessonSession.lesson_id == lesson_id)
        )

    def upsert(
        self,
        *,
        lesson_id: int,
        session_id: str,
        teacher_id: Optional[int],
        student_id: Optional[int],
        subject: Optional[str],
        grade: Optional[str],
        course_type: str,
        mode: str,
        asr_enabled: bool,
        llm_enabled: bool,
        status: str,
    ) -> AiLiveLessonSession:
        row = self.get_by_lesson_id(lesson_id)
        if row is None:
            row = AiLiveLessonSession(
                lesson_id=lesson_id,
                session_id=session_id,
                teacher_id=teacher_id,
                student_id=student_id,
                subject=subject,
                grade=grade,
                course_type=course_type,
                mode=mode,
                asr_enabled=asr_enabled,
                llm_enabled=llm_enabled,
                status=status,
            )
            self.session.add(row)
        else:
            row.session_id = session_id
            row.teacher_id = teacher_id
            row.student_id = student_id
            row.subject = subject
            row.grade = grade
            row.course_type = course_type
            row.mode = mode
            row.asr_enabled = asr_enabled
            row.llm_enabled = llm_enabled
            row.status = status
        self.session.flush()
        return row

    def update_status(self, lesson_id: int, status: str) -> None:
        row = self.get_by_lesson_id(lesson_id)
        if row is None:
            return
        row.status = status
        self.session.flush()


class TranscriptRepository:
    def __init__(self, session: Session):
        self.session = session

    def save_segment(
        self,
        *,
        lesson_id: int,
        seq: int,
        speaker: str,
        start_ms: int,
        end_ms: int,
        text: str,
        is_final: bool,
    ) -> AiLessonTranscript:
        row = AiLessonTranscript(
            lesson_id=lesson_id,
            segment_index=seq,
            speaker=speaker,
            start_ms=start_ms,
            end_ms=end_ms,
            text=text,
            is_final=is_final,
        )
        self.session.add(row)
        self.session.flush()
        return row


class LessonStageSummaryRepository:
    def __init__(self, session: Session):
        self.session = session

    def save(
        self,
        *,
        lesson_id: int,
        stage_index: int,
        summary_json: dict,
    ) -> AiLessonStageSummary:
        row = AiLessonStageSummary(
            lesson_id=lesson_id,
            stage_index=stage_index,
            summary_json=summary_json,
        )
        self.session.add(row)
        self.session.flush()
        return row


class LessonTurnRepository:
    def __init__(self, session: Session):
        self.session = session

    def save(self, *, lesson_id: int, turn: dict) -> AiLessonTurn:
        row = AiLessonTurn(
            lesson_id=lesson_id,
            turn_id=str(turn.get("turnId") or ""),
            speaker=str(turn.get("speaker") or "unknown"),
            start_segment=int(turn.get("startSegment") or 0),
            end_segment=int(turn.get("endSegment") or 0),
            text=str(turn.get("text") or ""),
            role_hint=turn.get("roleHint"),
            metadata_json=turn.get("metadata") or {},
        )
        self.session.add(row)
        self.session.flush()
        return row


class LessonTeachingEventRepository:
    def __init__(self, session: Session):
        self.session = session

    def save_many(self, *, lesson_id: int, events: list[dict]) -> list[AiLessonTeachingEvent]:
        rows: list[AiLessonTeachingEvent] = []
        for event in events:
            row = AiLessonTeachingEvent(
                lesson_id=lesson_id,
                event_id=str(event.get("eventId") or ""),
                event_type=str(event.get("type") or "unknown"),
                topic=event.get("topic"),
                text=str(event.get("text") or ""),
                confidence=int(float(event.get("confidence") or 0) * 100),
                evidence_json=event.get("evidence") or {},
                status=str(event.get("status") or "ACTIVE"),
            )
            self.session.add(row)
            rows.append(row)
        self.session.flush()
        return rows


class LessonEpisodeRepository:
    def __init__(self, session: Session):
        self.session = session

    def save_from_section(self, *, lesson_id: int, section: dict, patch: dict) -> AiLessonEpisode:
        row = AiLessonEpisode(
            lesson_id=lesson_id,
            episode_id=str(section.get("id") or patch.get("targetSectionId") or ""),
            title=str(section.get("title") or "课堂阶段"),
            stage_type=patch.get("stageType"),
            topic=patch.get("currentTopic"),
            start_segment=int(section.get("startSegment") or 0),
            end_segment=int(section.get("endSegment") or 0),
            summary=section.get("summary"),
            key_points_json={"items": patch.get("keyPoints") or []},
            student_questions_json={"items": patch.get("studentQuestions") or []},
            homework_json={"items": patch.get("homeworkCandidates") or []},
            evidence_json={"patchId": patch.get("patchId")},
            status="ACTIVE",
        )
        self.session.add(row)
        self.session.flush()
        return row


class LessonSummaryPatchRepository:
    def __init__(self, session: Session):
        self.session = session

    def save(
        self,
        *,
        lesson_id: int,
        patch: dict,
        status: str,
        trigger_reasons: list[str],
        guard: dict,
    ) -> AiLessonSummaryPatch:
        row = AiLessonSummaryPatch(
            lesson_id=lesson_id,
            patch_id=str(patch.get("patchId") or ""),
            patch_type=str(patch.get("patchType") or "unknown"),
            status=status,
            trigger_reasons_json={"items": trigger_reasons},
            patch_json=patch,
            guard_json=guard,
        )
        self.session.add(row)
        self.session.flush()
        return row
