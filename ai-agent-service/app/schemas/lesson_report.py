from __future__ import annotations

from typing import Any, Dict, Optional

from pydantic import BaseModel, Field


class LessonReportTaskRequest(BaseModel):
    teacherId: Optional[int] = None
    studentId: Optional[int] = None
    subject: Optional[str] = None
    grade: Optional[str] = None
    lessonTopic: str = Field(min_length=1, max_length=200)
    teacherNotes: str = Field(min_length=1, max_length=12000)
    studentPerformance: Optional[str] = Field(default=None, max_length=4000)
    homework: Optional[str] = Field(default=None, max_length=4000)
    nextPlan: Optional[str] = Field(default=None, max_length=4000)
    extraContext: Optional[Dict[str, Any]] = None
    forceRegenerate: bool = False


class LessonReportTaskResponse(BaseModel):
    taskId: str
    status: str


class LessonReportView(BaseModel):
    lessonId: int
    taskId: str
    status: str
    report: Optional[Dict[str, Any]] = None
