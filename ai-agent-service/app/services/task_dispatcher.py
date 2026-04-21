from __future__ import annotations

from app.core.config import get_settings
from app.queue.redis_queue import get_queue
from app.schemas.task import TaskType
from app.tasks.handlers import run_chat_summary_task, run_lesson_report_task


def dispatch_task(task_id: str, task_type: TaskType) -> None:
    settings = get_settings()
    if settings.use_async_worker:
        queue = get_queue()
        if task_type == TaskType.LESSON_REPORT:
            queue.enqueue(run_lesson_report_task, task_id, job_timeout=settings.task_default_timeout_seconds)
            return
        if task_type == TaskType.CHAT_SUMMARY:
            queue.enqueue(run_chat_summary_task, task_id, job_timeout=settings.task_default_timeout_seconds)
            return
        raise ValueError(f"unsupported_task_type:{task_type}")

    if task_type == TaskType.LESSON_REPORT:
        run_lesson_report_task(task_id)
        return
    if task_type == TaskType.CHAT_SUMMARY:
        run_chat_summary_task(task_id)
        return
    raise ValueError(f"unsupported_task_type:{task_type}")
