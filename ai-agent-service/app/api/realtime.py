from __future__ import annotations

from fastapi import APIRouter, Depends, WebSocket, WebSocketDisconnect

from app.core.security import verify_internal_token
from app.queue.redis_queue import get_redis
from app.realtime.state_store import RealtimeStateStore
from app.schemas.common import ApiResponse, ok
from app.schemas.realtime import (
    AudioChunkInput,
    LiveLessonSessionCreateRequest,
    LiveLessonSessionView,
    RealtimeLessonStateView,
    TranscriptSegmentInput,
)
from app.services.realtime_service import RealtimeLessonService

router = APIRouter(
    prefix="/internal/ai/live-lessons",
    tags=["realtime-class-ai"],
    dependencies=[Depends(verify_internal_token)],
)


@router.post("/{lesson_id}/sessions", response_model=ApiResponse[LiveLessonSessionView])
def create_session(
    lesson_id: int, request: LiveLessonSessionCreateRequest
) -> ApiResponse[LiveLessonSessionView]:
    data = RealtimeLessonService().create_session(lesson_id, request)
    return ok(LiveLessonSessionView(**data))


@router.post("/{lesson_id}/transcript-segments", response_model=ApiResponse[RealtimeLessonStateView])
def accept_segment(
    lesson_id: int, request: TranscriptSegmentInput
) -> ApiResponse[RealtimeLessonStateView]:
    state = RealtimeLessonService().accept_segment(lesson_id, request)
    return ok(_to_state_view(lesson_id, state))


@router.post("/{lesson_id}/audio-chunks", response_model=ApiResponse[RealtimeLessonStateView])
def accept_audio_chunk(
    lesson_id: int, request: AudioChunkInput
) -> ApiResponse[RealtimeLessonStateView]:
    state = RealtimeLessonService().accept_audio_chunk(lesson_id, request)
    return ok(_to_state_view(lesson_id, state))


@router.get("/{lesson_id}/state", response_model=ApiResponse[RealtimeLessonStateView])
def get_state(lesson_id: int) -> ApiResponse[RealtimeLessonStateView]:
    state = RealtimeLessonService().get_state(lesson_id)
    return ok(_to_state_view(lesson_id, state))


@router.post("/{lesson_id}/finalize", response_model=ApiResponse[RealtimeLessonStateView])
def finalize(lesson_id: int) -> ApiResponse[RealtimeLessonStateView]:
    state = RealtimeLessonService().finalize(lesson_id)
    return ok(_to_state_view(lesson_id, state))


@router.websocket("/{lesson_id}/stream")
async def lesson_stream(websocket: WebSocket, lesson_id: int):
    await websocket.accept()
    redis = get_redis()
    pubsub = redis.pubsub()
    channel = RealtimeStateStore().event_channel(lesson_id)
    pubsub.subscribe(channel)
    try:
        while True:
            message = pubsub.get_message(ignore_subscribe_messages=True, timeout=1.0)
            if message:
                data = message.get("data")
                if isinstance(data, bytes):
                    data = data.decode("utf-8")
                await websocket.send_text(data)
            try:
                await websocket.receive_text()
            except Exception:
                pass
    except WebSocketDisconnect:
        return
    finally:
        pubsub.unsubscribe(channel)
        pubsub.close()


def _to_state_view(lesson_id: int, state: dict) -> RealtimeLessonStateView:
    return RealtimeLessonStateView(
        lessonId=lesson_id,
        mode=state.get("mode") or "LIGHT",
        asrEnabled=bool(state.get("asrEnabled")),
        llmEnabled=bool(state.get("llmEnabled", True)),
        currentTopic=state.get("currentTopic"),
        latestStageSummary=state.get("latestStageSummary"),
        studentQuestions=state.get("studentQuestions") or [],
        homeworkCandidates=state.get("homeworkCandidates") or [],
        keyPoints=state.get("keyPoints") or [],
        minutesOutline=state.get("minutesOutline") or [],
        activeSectionTitle=state.get("activeSectionTitle"),
        segmentCount=int(state.get("segmentCount") or 0),
        status=state.get("status") or "UNKNOWN",
        rawState=state,
    )
