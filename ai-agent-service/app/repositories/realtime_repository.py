from __future__ import annotations

from typing import Optional

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.storage.database import AiLessonStageSummary, AiLessonTranscript, AiLiveLessonSession


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
