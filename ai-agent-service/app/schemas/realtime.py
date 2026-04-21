from __future__ import annotations

from typing import Dict, List, Optional

from pydantic import BaseModel, Field


class LiveLessonSessionCreateRequest(BaseModel):
    teacherId: Optional[int] = None
    studentId: Optional[int] = None
    subject: Optional[str] = None
    grade: Optional[str] = None
    courseType: str = "ONLINE_FORMAL"
    audioEnabled: bool = True
    realtimeAiMode: str = "LIGHT"


class LiveLessonSessionView(BaseModel):
    lessonId: int
    sessionId: str
    asrEnabled: bool
    llmEnabled: bool
    mode: str
    status: str


class TranscriptSegmentInput(BaseModel):
    seq: int
    speaker: str = "unknown"
    startMs: int = 0
    endMs: int = 0
    text: str = Field(min_length=1, max_length=4000)
    isFinal: bool = True


class RealtimeLessonStateView(BaseModel):
    lessonId: int
    mode: str
    asrEnabled: bool
    llmEnabled: bool
    currentTopic: Optional[str] = None
    latestStageSummary: Optional[str] = None
    studentQuestions: List[str] = Field(default_factory=list)
    homeworkCandidates: List[str] = Field(default_factory=list)
    keyPoints: List[str] = Field(default_factory=list)
    segmentCount: int = 0
    status: str
    rawState: Optional[Dict] = None
