from __future__ import annotations

from typing import Optional

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.storage.database import AiLessonReport


class LessonReportRepository:
    def __init__(self, session: Session):
        self.session = session

    def get_by_lesson_id(self, lesson_id: int) -> Optional[AiLessonReport]:
        return self.session.scalar(
            select(AiLessonReport).where(AiLessonReport.lesson_id == lesson_id)
        )

    def upsert_pending(
        self,
        *,
        lesson_id: int,
        task_id: str,
        teacher_id: Optional[int],
        student_id: Optional[int],
        status: str,
    ) -> AiLessonReport:
        report = self.get_by_lesson_id(lesson_id)
        if report is None:
            report = AiLessonReport(
                lesson_id=lesson_id,
                task_id=task_id,
                teacher_id=teacher_id,
                student_id=student_id,
                status=status,
            )
            self.session.add(report)
        else:
            report.task_id = task_id
            report.teacher_id = teacher_id
            report.student_id = student_id
            report.status = status
        self.session.flush()
        return report

    def save_report(self, *, lesson_id: int, task_id: str, report_json: dict, status: str) -> None:
        report = self.get_by_lesson_id(lesson_id)
        if report is None:
            report = AiLessonReport(lesson_id=lesson_id, task_id=task_id, status=status)
            self.session.add(report)
        report.task_id = task_id
        report.status = status
        report.report_json = report_json
        self.session.flush()
