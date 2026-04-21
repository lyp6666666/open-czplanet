from __future__ import annotations

from typing import Any, Dict, Optional

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.schemas.task import TaskStatus, TaskType, TaskView
from app.storage.database import AiTask


class TaskRepository:
    def __init__(self, session: Session):
        self.session = session

    def create(
        self,
        *,
        task_id: str,
        task_type: TaskType,
        biz_id: str,
        input_json: Dict[str, Any],
        status: TaskStatus,
    ) -> AiTask:
        task = AiTask(
            task_id=task_id,
            task_type=task_type.value,
            biz_id=biz_id,
            status=status.value,
            progress=0,
            input_json=input_json,
        )
        self.session.add(task)
        self.session.flush()
        return task

    def get(self, task_id: str) -> Optional[AiTask]:
        return self.session.scalar(select(AiTask).where(AiTask.task_id == task_id))

    def update_status(
        self,
        task_id: str,
        *,
        status: TaskStatus,
        progress: int,
        message: Optional[str] = None,
        output: Optional[Dict[str, Any]] = None,
        error_message: Optional[str] = None,
    ) -> AiTask:
        task = self.get(task_id)
        if task is None:
            raise ValueError(f"task_not_found:{task_id}")
        task.status = status.value
        task.progress = progress
        task.message = message
        if output is not None:
            task.output_json = output
        if error_message is not None:
            task.error_message = error_message
        self.session.flush()
        return task


def to_task_view(task: AiTask) -> TaskView:
    return TaskView(
        taskId=task.task_id,
        taskType=TaskType(task.task_type),
        bizId=task.biz_id,
        status=TaskStatus(task.status),
        progress=task.progress,
        message=task.message,
        output=task.output_json,
        errorMessage=task.error_message,
    )
