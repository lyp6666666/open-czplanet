from __future__ import annotations

from fastapi import APIRouter, Depends

from app.core.errors import not_found
from app.core.security import verify_internal_token
from app.repositories.chat_summary_repository import ChatSummaryRepository
from app.schemas.chat_summary import ChatSummaryTaskRequest, ChatSummaryTaskResponse, ChatSummaryView
from app.schemas.common import ApiResponse, ok
from app.services.chat_summary_service import ChatSummaryService
from app.storage.database import session_scope

router = APIRouter(
    prefix="/internal/ai/chat-rooms",
    tags=["chat-summary"],
    dependencies=[Depends(verify_internal_token)],
)


@router.post("/{room_id}/summary-tasks", response_model=ApiResponse[ChatSummaryTaskResponse])
def create_summary_task(
    room_id: int, request: ChatSummaryTaskRequest
) -> ApiResponse[ChatSummaryTaskResponse]:
    task_id, status = ChatSummaryService().create_task(room_id, request)
    return ok(ChatSummaryTaskResponse(taskId=task_id, status=status.value))


@router.get("/{room_id}/summary", response_model=ApiResponse[ChatSummaryView])
def get_summary(room_id: int) -> ApiResponse[ChatSummaryView]:
    with session_scope() as session:
        summary = ChatSummaryRepository(session).get_by_room_id(room_id)
        if summary is None:
            raise not_found("chat_summary_not_found")
        return ok(
            ChatSummaryView(
                roomId=summary.room_id,
                taskId=summary.task_id,
                summary=summary.summary_json,
            )
        )
