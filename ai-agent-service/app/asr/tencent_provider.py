from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Callable, Dict, Optional

from app.asr.base import RealtimeASRProvider, TranscriptCallback
from app.core.config import get_settings


class TencentRealtimeASRProvider(RealtimeASRProvider):
    """Tencent Cloud realtime ASR adapter based on tencentcloud-speech-sdk-python.

    The official SDK is not published as a normal package in this repository. Production deployments
    should mount or install TencentCloud/tencentcloud-speech-sdk-python and configure
    AI_AGENT_TENCENT_SPEECH_SDK_PATH when needed.
    """

    def __init__(self):
        self.settings = get_settings()

    def available(self) -> bool:
        return bool(
            self.settings.tencent_asr_enabled
            and self.settings.tencent_asr_app_id
            and self.settings.tencent_asr_secret_id
            and self.settings.tencent_asr_secret_key
        )

    def create_session(self, *, lesson_id: int, callback: TranscriptCallback):
        if not self.available():
            raise ValueError("tencent_asr_not_configured")
        self._ensure_sdk_path()
        from asr import speech_recognizer
        from common import credential

        listener = _TencentListener(lesson_id=lesson_id, callback=callback)
        credential_var = credential.Credential(
            self.settings.tencent_asr_secret_id, self.settings.tencent_asr_secret_key
        )
        recognizer = speech_recognizer.SpeechRecognizer(
            self.settings.tencent_asr_app_id,
            credential_var,
            self.settings.tencent_asr_engine_model_type,
            listener,
        )
        recognizer.set_filter_modal(1)
        recognizer.set_filter_punc(0)
        recognizer.set_filter_dirty(1)
        recognizer.set_need_vad(self.settings.tencent_asr_need_vad)
        recognizer.set_voice_format(self.settings.tencent_asr_voice_format)
        recognizer.set_word_info(1)
        recognizer.set_convert_num_mode(1)
        return recognizer

    def session_closed(self, session) -> bool:
        self._ensure_sdk_path()
        from asr import speech_recognizer

        return getattr(session, "status", None) in {
            speech_recognizer.FINAL,
            speech_recognizer.ERROR,
            speech_recognizer.CLOSED,
        }

    def write_audio(self, session, audio: bytes) -> bool:
        self._ensure_sdk_path()
        from asr import speech_recognizer

        if getattr(session, "status", None) in {
            speech_recognizer.FINAL,
            speech_recognizer.ERROR,
            speech_recognizer.CLOSED,
        }:
            return False
        session.write(audio)
        return True

    def _ensure_sdk_path(self) -> None:
        sdk_path = self.settings.tencent_speech_sdk_path
        if not sdk_path:
            return
        path = str(Path(sdk_path).expanduser().resolve())
        if path not in sys.path:
            sys.path.insert(0, path)


class _TencentListener:
    def __init__(self, *, lesson_id: int, callback: TranscriptCallback):
        self.lesson_id = lesson_id
        self.callback = callback
        self.seq = 0

    def on_recognition_start(self, response):
        pass

    def on_sentence_begin(self, response):
        pass

    def on_recognition_result_change(self, response):
        self._emit(response, is_final=False)

    def on_sentence_end(self, response):
        self._emit(response, is_final=True)

    def on_recognition_complete(self, response):
        pass

    def on_fail(self, response):
        self.callback({"type": "asr.error", "payload": response})

    def _emit(self, response: Dict, *, is_final: bool) -> None:
        text = _extract_text(response)
        if not text:
            return
        self.seq += 1
        self.callback(
            {
                "lessonId": self.lesson_id,
                "seq": self.seq,
                "speaker": "unknown",
                "startMs": int(response.get("start_time") or 0),
                "endMs": int(response.get("end_time") or 0),
                "text": text,
                "isFinal": is_final,
                "raw": response,
            }
        )


def _extract_text(response: Dict) -> Optional[str]:
    for key in ["result", "voice_text_str", "text", "final_sentence"]:
        value = response.get(key)
        if isinstance(value, str) and value.strip():
            return value.strip()
    result = response.get("result")
    if isinstance(result, dict):
        for key in ["voice_text_str", "text"]:
            value = result.get(key)
            if isinstance(value, str) and value.strip():
                return value.strip()
    try:
        return json.dumps(response, ensure_ascii=False)
    except Exception:
        return None
