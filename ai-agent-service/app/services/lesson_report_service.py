from __future__ import annotations

from typing import Tuple

from app.core.id_generator import new_task_id
from app.repositories.report_repository import LessonReportRepository
from app.repositories.task_repository import TaskRepository
from app.schemas.lesson_report import LessonReportTaskRequest
from app.schemas.task import TaskStatus, TaskType
from app.services.task_dispatcher import dispatch_task
from app.storage.database import session_scope


class LessonReportService:
    def create_task(self, lesson_id: int, request: LessonReportTaskRequest) -> Tuple[str, TaskStatus]:
        task_id = new_task_id("lesson_report")
        payload = request.model_dump(mode="json")
        payload["lessonId"] = lesson_id
        with session_scope() as session:
            task_repo = TaskRepository(session)
            report_repo = LessonReportRepository(session)
            task_repo.create(
                task_id=task_id,
                task_type=TaskType.LESSON_REPORT,
                biz_id=str(lesson_id),
                input_json=payload,
                status=TaskStatus.QUEUED,
            )
            report_repo.upsert_pending(
                lesson_id=lesson_id,
                task_id=task_id,
                teacher_id=request.teacherId,
                student_id=request.studentId,
                status=TaskStatus.QUEUED.value,
            )
        dispatch_task(task_id, TaskType.LESSON_REPORT)
        return task_id, TaskStatus.QUEUED
