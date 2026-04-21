from __future__ import annotations

from typing import Optional

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.storage.database import AiChatSummary


class ChatSummaryRepository:
    def __init__(self, session: Session):
        self.session = session

    def get_by_room_id(self, room_id: int) -> Optional[AiChatSummary]:
        return self.session.scalar(select(AiChatSummary).where(AiChatSummary.room_id == room_id))

    def upsert_pending(
        self,
        *,
        room_id: int,
        task_id: str,
        message_start_id: Optional[int],
        message_end_id: Optional[int],
    ) -> AiChatSummary:
        summary = self.get_by_room_id(room_id)
        if summary is None:
            summary = AiChatSummary(
                room_id=room_id,
                task_id=task_id,
                message_start_id=message_start_id,
                message_end_id=message_end_id,
            )
            self.session.add(summary)
        else:
            summary.task_id = task_id
            summary.message_start_id = message_start_id
            summary.message_end_id = message_end_id
        self.session.flush()
        return summary

    def save_summary(
        self,
        *,
        room_id: int,
        task_id: str,
        summary_json: dict,
        message_start_id: Optional[int],
        message_end_id: Optional[int],
    ) -> None:
        summary = self.get_by_room_id(room_id)
        if summary is None:
            summary = AiChatSummary(room_id=room_id, task_id=task_id)
            self.session.add(summary)
        summary.task_id = task_id
        summary.summary_json = summary_json
        summary.message_start_id = message_start_id
        summary.message_end_id = message_end_id
        self.session.flush()
