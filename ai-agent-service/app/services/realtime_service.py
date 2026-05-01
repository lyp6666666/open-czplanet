from __future__ import annotations

import base64
import time
from dataclasses import dataclass
from typing import Dict

from app.asr.provider import get_realtime_asr_provider
from app.core.id_generator import new_task_id
from app.core.config import get_settings
from app.realtime.graph import REALTIME_GRAPH
from app.realtime.state_store import RealtimeStateStore
from app.repositories.realtime_repository import (
    LessonEpisodeRepository,
    LessonStageSummaryRepository,
    LessonSummaryPatchRepository,
    LessonTeachingEventRepository,
    LessonTurnRepository,
    LiveLessonSessionRepository,
    TranscriptRepository,
)
from app.schemas.realtime import AudioChunkInput, LiveLessonSessionCreateRequest, TranscriptSegmentInput
from app.storage.database import session_scope


@dataclass
class _AsrRuntime:
    recognizer: object
    provider: object
    started: bool = False
    last_voice_at: float = 0.0


_ASR_RUNTIMES: Dict[int, _AsrRuntime] = {}


def _discard_asr_runtime(lesson_id: int) -> None:
    runtime = _ASR_RUNTIMES.pop(lesson_id, None)
    if not runtime:
        return
    try:
        runtime.provider.stop_session(runtime.recognizer)
    except Exception:
        pass


class RealtimeLessonService:
    def create_session(self, lesson_id: int, request: LiveLessonSessionCreateRequest) -> dict:
        asr_provider = get_realtime_asr_provider()
        asr_enabled = bool(request.audioEnabled and asr_provider.available())
        llm_enabled = request.realtimeAiMode.upper() not in {"ASR_ONLY", "OFF"}
        session_id = new_task_id("lesson_ai")
        with session_scope() as session:
            LiveLessonSessionRepository(session).upsert(
                lesson_id=lesson_id,
                session_id=session_id,
                teacher_id=request.teacherId,
                student_id=request.studentId,
                subject=request.subject,
                grade=request.grade,
                course_type=request.courseType,
                mode=request.realtimeAiMode,
                asr_enabled=asr_enabled,
                llm_enabled=llm_enabled,
                status="ACTIVE",
            )
        RealtimeStateStore().create_session_state(
            lesson_id,
            {
                "lessonId": lesson_id,
                "sessionId": session_id,
                "createdAtTs": int(time.time()),
                "mode": request.realtimeAiMode,
                "asrEnabled": asr_enabled,
                "llmEnabled": llm_enabled,
                "subject": request.subject,
                "grade": request.grade,
                "status": "ACTIVE",
            },
        )
        return {
            "lessonId": lesson_id,
            "sessionId": session_id,
            "asrEnabled": asr_enabled,
            "llmEnabled": llm_enabled,
            "mode": request.realtimeAiMode,
            "status": "ACTIVE",
        }

    def accept_segment(self, lesson_id: int, segment: TranscriptSegmentInput) -> dict:
        payload = segment.model_dump(mode="json")
        with session_scope() as session:
            TranscriptRepository(session).save_segment(
                lesson_id=lesson_id,
                seq=segment.seq,
                speaker=segment.speaker,
                start_ms=segment.startMs,
                end_ms=segment.endMs,
                text=segment.text,
                is_final=segment.isFinal,
            )
        graph_result = REALTIME_GRAPH.invoke({"lesson_id": lesson_id, "segment": payload})
        self._persist_agent_memory(lesson_id, graph_result)
        if graph_result.get("summary"):
            with session_scope() as session:
                LessonStageSummaryRepository(session).save(
                    lesson_id=lesson_id,
                    stage_index=int((graph_result.get("state") or {}).get("lastLlmSegmentCount") or 0),
                    summary_json=graph_result["summary"],
                )
        return RealtimeStateStore().get_state(lesson_id)

    def accept_audio_chunk(self, lesson_id: int, chunk: AudioChunkInput) -> dict:
        settings = get_settings()
        store = RealtimeStateStore()
        state = store.get_state(lesson_id)
        if not state:
            state = {
                "lessonId": lesson_id,
                "mode": "LIGHT",
                "asrEnabled": False,
                "llmEnabled": True,
                "status": "ACTIVE",
            }
            store.create_session_state(lesson_id, state)

        now = time.time()
        rms = float(chunk.rms or 0.0)
        patch = {
            "lastAudioAt": int(now),
            "lastAudioRms": rms,
            "audioSampleRate": chunk.sampleRate,
            "audioFormat": chunk.format,
        }
        if rms < settings.realtime_min_audio_rms:
            last_voice_at = int(state.get("lastVoiceAt") or 0)
            patch["asrListening"] = bool(
                last_voice_at
                and int(now) - last_voice_at < settings.realtime_silence_pause_seconds
            )
            return store.update_state(lesson_id, patch)

        patch["lastVoiceAt"] = int(now)
        patch["asrListening"] = True
        state = store.update_state(lesson_id, patch)

        provider = get_realtime_asr_provider()
        if not provider.available():
            return store.update_state(lesson_id, {"asrEnabled": False, "status": "ASR_DEGRADED"})

        runtime = _ASR_RUNTIMES.get(lesson_id)
        is_closed = getattr(provider, "session_closed", None)
        if runtime is not None and callable(is_closed) and is_closed(runtime.recognizer):
            _discard_asr_runtime(lesson_id)
            runtime = None
        if runtime is None:
            runtime = _AsrRuntime(
                recognizer=provider.create_session(
                    lesson_id=lesson_id,
                    callback=lambda event: self._handle_asr_event(lesson_id, chunk.speaker, event),
                ),
                provider=provider,
            )
            _ASR_RUNTIMES[lesson_id] = runtime
        if not runtime.started:
            runtime.provider.start_session(runtime.recognizer)
            runtime.started = True
        runtime.last_voice_at = now

        audio = base64.b64decode(chunk.audioBase64)
        if not runtime.provider.write_audio(runtime.recognizer, audio):
            _discard_asr_runtime(lesson_id)
            return store.update_state(lesson_id, {"asrEnabled": False, "status": "ASR_DEGRADED"})
        return store.update_state(lesson_id, {"asrEnabled": True, "status": "ACTIVE"})

    def _handle_asr_event(self, lesson_id: int, speaker: str, event: dict) -> None:
        if event.get("type") == "asr.error":
            payload = event.get("payload") or {}
            message = str(payload.get("message") or "")
            if "超过15秒未发送音频数据" in message:
                _discard_asr_runtime(lesson_id)
                RealtimeStateStore().update_state(
                    lesson_id,
                    {
                        "status": "ACTIVE",
                        "asrListening": False,
                        "lastAsrIdleAt": int(time.time()),
                        "lastAsrNotice": message,
                    },
                )
                return
            RealtimeStateStore().update_state(
                lesson_id, {"status": "ASR_DEGRADED", "lastAsrError": payload}
            )
            return
        text = str(event.get("text") or "").strip()
        if not text:
            return
        segment = TranscriptSegmentInput(
            seq=int(event.get("seq") or int(time.time() * 1000)),
            speaker=event.get("speaker") or speaker or "unknown",
            startMs=int(event.get("startMs") or 0),
            endMs=int(event.get("endMs") or 0),
            text=text,
            isFinal=bool(event.get("isFinal", True)),
        )
        self.accept_segment(lesson_id, segment)

    def get_state(self, lesson_id: int) -> dict:
        return RealtimeStateStore().get_state(lesson_id)

    def finalize(self, lesson_id: int) -> dict:
        state = RealtimeStateStore().finalize(lesson_id)
        with session_scope() as session:
            LiveLessonSessionRepository(session).update_status(lesson_id, "FINALIZED")
        return state

    def _persist_agent_memory(self, lesson_id: int, graph_result: dict) -> None:
        try:
            with session_scope() as session:
                turn = graph_result.get("turn")
                if turn:
                    LessonTurnRepository(session).save(lesson_id=lesson_id, turn=turn)
                events = graph_result.get("events") or []
                if events:
                    LessonTeachingEventRepository(session).save_many(
                        lesson_id=lesson_id,
                        events=events,
                    )
                patch = graph_result.get("patch")
                guard = graph_result.get("guard") or {}
                if patch:
                    trigger_reasons = (graph_result.get("decision") or {}).get("triggerReasons") or []
                    status = "ACCEPTED" if guard.get("accepted") else "REJECTED"
                    LessonSummaryPatchRepository(session).save(
                        lesson_id=lesson_id,
                        patch=patch,
                        status=status,
                        trigger_reasons=trigger_reasons,
                        guard=guard,
                    )
                    projection = graph_result.get("projection") or {}
                    outline = projection.get("minutesOutline") or []
                    if guard.get("accepted") and outline:
                        LessonEpisodeRepository(session).save_from_section(
                            lesson_id=lesson_id,
                            section=outline[-1],
                            patch=patch,
                        )
        except Exception:
            # Realtime memory persistence must not break classroom transcript ingestion.
            return
