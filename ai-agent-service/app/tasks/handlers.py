from __future__ import annotations

from app.pipelines.chat_summary_pipeline import ChatSummaryPipeline
from app.pipelines.lesson_report_pipeline import LessonReportPipeline
from app.repositories.chat_summary_repository import ChatSummaryRepository
from app.repositories.report_repository import LessonReportRepository
from app.repositories.task_repository import TaskRepository
from app.schemas.task import TaskStatus
from app.storage.database import session_scope


def run_lesson_report_task(task_id: str) -> None:
    with session_scope() as session:
        task_repo = TaskRepository(session)
        task = task_repo.get(task_id)
        if task is None:
            raise ValueError(f"task_not_found:{task_id}")
        task_repo.update_status(
            task_id, status=TaskStatus.RUNNING, progress=20, message="generating lesson report"
        )
        try:
            payload = task.input_json or {}
            lesson_id = int(payload["lessonId"])
            report = LessonReportPipeline().generate(payload)
            LessonReportRepository(session).save_report(
                lesson_id=lesson_id,
                task_id=task_id,
                report_json=report,
                status="WAITING_TEACHER_REVIEW",
            )
            task_repo.update_status(
                task_id,
                status=TaskStatus.SUCCESS,
                progress=100,
                message="lesson report generated",
                output={"lessonId": lesson_id, "report": report},
            )
        except Exception as exc:
            task_repo.update_status(
                task_id,
                status=TaskStatus.FAILED,
                progress=100,
                message="lesson report failed",
                error_message=str(exc),
            )
            raise


def run_chat_summary_task(task_id: str) -> None:
    with session_scope() as session:
        task_repo = TaskRepository(session)
        task = task_repo.get(task_id)
        if task is None:
            raise ValueError(f"task_not_found:{task_id}")
        task_repo.update_status(
            task_id, status=TaskStatus.RUNNING, progress=20, message="generating chat summary"
        )
        try:
            payload = task.input_json or {}
            room_id = int(payload["roomId"])
            summary = ChatSummaryPipeline().generate(payload)
            ChatSummaryRepository(session).save_summary(
                room_id=room_id,
                task_id=task_id,
                summary_json=summary,
                message_start_id=payload.get("messageStartId"),
                message_end_id=payload.get("messageEndId"),
            )
            task_repo.update_status(
                task_id,
                status=TaskStatus.SUCCESS,
                progress=100,
                message="chat summary generated",
                output={"roomId": room_id, "summary": summary},
            )
        except Exception as exc:
            task_repo.update_status(
                task_id,
                status=TaskStatus.FAILED,
                progress=100,
                message="chat summary failed",
                error_message=str(exc),
            )
            raise
