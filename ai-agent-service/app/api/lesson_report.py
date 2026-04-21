from __future__ import annotations

from fastapi import APIRouter, Depends

from app.core.errors import not_found
from app.core.security import verify_internal_token
from app.repositories.report_repository import LessonReportRepository
from app.schemas.common import ApiResponse, ok
from app.schemas.lesson_report import (
    LessonReportTaskRequest,
    LessonReportTaskResponse,
    LessonReportView,
)
from app.services.lesson_report_service import LessonReportService
from app.storage.database import session_scope

router = APIRouter(
    prefix="/internal/ai/lessons",
    tags=["lesson-report"],
    dependencies=[Depends(verify_internal_token)],
)


@router.post("/{lesson_id}/report-tasks", response_model=ApiResponse[LessonReportTaskResponse])
def create_report_task(
    lesson_id: int, request: LessonReportTaskRequest
) -> ApiResponse[LessonReportTaskResponse]:
    task_id, status = LessonReportService().create_task(lesson_id, request)
    return ok(LessonReportTaskResponse(taskId=task_id, status=status.value))


@router.get("/{lesson_id}/report", response_model=ApiResponse[LessonReportView])
def get_report(lesson_id: int) -> ApiResponse[LessonReportView]:
    with session_scope() as session:
        report = LessonReportRepository(session).get_by_lesson_id(lesson_id)
        if report is None:
            raise not_found("lesson_report_not_found")
        return ok(
            LessonReportView(
                lessonId=report.lesson_id,
                taskId=report.task_id,
                status=report.status,
                report=report.report_json,
            )
        )
