from __future__ import annotations

from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


class ChatMessageInput(BaseModel):
    messageId: Optional[int] = None
    senderId: Optional[int] = None
    senderRole: Optional[str] = None
    senderName: Optional[str] = None
    content: str = Field(min_length=1, max_length=4000)
    sentAt: Optional[str] = None


class ChatSummaryTaskRequest(BaseModel):
    triggeredBy: Optional[int] = None
    messages: List[ChatMessageInput] = Field(default_factory=list, max_length=300)
    messageStartId: Optional[int] = None
    messageEndId: Optional[int] = None
    extraContext: Optional[Dict[str, Any]] = None
    forceRegenerate: bool = False


class ChatSummaryTaskResponse(BaseModel):
    taskId: str
    status: str


class ChatSummaryView(BaseModel):
    roomId: int
    taskId: str
    summary: Optional[Dict[str, Any]] = None
