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


class AudioChunkInput(BaseModel):
    participantId: Optional[int] = None
    speaker: str = "unknown"
    sequence: int = 0
    sampleRate: int = 16000
    channelCount: int = 1
    durationMs: int = 0
    rms: float = 0.0
    format: str = "PCM16"
    audioBase64: str = Field(min_length=1, max_length=160000)


class RealtimeMinuteItem(BaseModel):
    title: str = Field(min_length=1, max_length=40)
    detail: str = Field(min_length=1, max_length=240)


class RealtimeMinuteSection(BaseModel):
    id: str = Field(min_length=1, max_length=64)
    title: str = Field(min_length=1, max_length=40)
    summary: str = Field(min_length=1, max_length=260)
    startSegment: int = 0
    endSegment: int = 0
    updatedAt: int = 0
    items: List[RealtimeMinuteItem] = Field(default_factory=list)


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
    minutesOutline: List[RealtimeMinuteSection] = Field(default_factory=list)
    activeSectionTitle: Optional[str] = None
    segmentCount: int = 0
    status: str
    rawState: Optional[Dict] = None
