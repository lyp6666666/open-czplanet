from __future__ import annotations

from typing import Tuple

from app.core.id_generator import new_task_id
from app.repositories.chat_summary_repository import ChatSummaryRepository
from app.repositories.task_repository import TaskRepository
from app.schemas.chat_summary import ChatSummaryTaskRequest
from app.schemas.task import TaskStatus, TaskType
from app.services.task_dispatcher import dispatch_task
from app.storage.database import session_scope


class ChatSummaryService:
    def create_task(self, room_id: int, request: ChatSummaryTaskRequest) -> Tuple[str, TaskStatus]:
        task_id = new_task_id("chat_summary")
        payload = request.model_dump(mode="json")
        payload["roomId"] = room_id
        with session_scope() as session:
            task_repo = TaskRepository(session)
            summary_repo = ChatSummaryRepository(session)
            task_repo.create(
                task_id=task_id,
                task_type=TaskType.CHAT_SUMMARY,
                biz_id=str(room_id),
                input_json=payload,
                status=TaskStatus.QUEUED,
            )
            summary_repo.upsert_pending(
                room_id=room_id,
                task_id=task_id,
                message_start_id=request.messageStartId,
                message_end_id=request.messageEndId,
            )
        dispatch_task(task_id, TaskType.CHAT_SUMMARY)
        return task_id, TaskStatus.QUEUED
