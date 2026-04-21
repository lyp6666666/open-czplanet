from __future__ import annotations

from enum import Enum
from typing import Any, Dict, Optional

from pydantic import BaseModel, Field


class TaskType(str, Enum):
    LESSON_REPORT = "LESSON_REPORT"
    CHAT_SUMMARY = "CHAT_SUMMARY"


class TaskStatus(str, Enum):
    CREATED = "CREATED"
    QUEUED = "QUEUED"
    RUNNING = "RUNNING"
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"
    CANCELED = "CANCELED"


class TaskView(BaseModel):
    taskId: str
    taskType: TaskType
    bizId: str
    status: TaskStatus
    progress: int = Field(ge=0, le=100)
    message: Optional[str] = None
    output: Optional[Dict[str, Any]] = None
    errorMessage: Optional[str] = None
