from __future__ import annotations

from fastapi import APIRouter, Depends

from app.core.errors import not_found
from app.core.security import verify_internal_token
from app.repositories.task_repository import TaskRepository, to_task_view
from app.schemas.common import ApiResponse, ok
from app.schemas.task import TaskView
from app.storage.database import session_scope

router = APIRouter(prefix="/internal/ai/tasks", dependencies=[Depends(verify_internal_token)])


@router.get("/{task_id}", response_model=ApiResponse[TaskView])
def get_task(task_id: str) -> ApiResponse[TaskView]:
    with session_scope() as session:
        task = TaskRepository(session).get(task_id)
        if task is None:
            raise not_found("task_not_found")
        return ok(to_task_view(task))
