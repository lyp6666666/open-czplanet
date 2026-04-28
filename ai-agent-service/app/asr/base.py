from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Callable, Dict


TranscriptCallback = Callable[[Dict], None]


class RealtimeASRProvider(ABC):
    @abstractmethod
    def available(self) -> bool:
        raise NotImplementedError

    @abstractmethod
    def create_session(self, *, lesson_id: int, callback: TranscriptCallback):
        raise NotImplementedError

    def start_session(self, session) -> None:
        for method_name in ("start", "open", "begin"):
            method = getattr(session, method_name, None)
            if callable(method):
                method()
                return

    def write_audio(self, session, audio: bytes) -> bool:
        for method_name in ("write", "write_data", "send", "send_audio", "write_audio"):
            method = getattr(session, method_name, None)
            if callable(method):
                method(audio)
                return True
        return False

    def stop_session(self, session) -> None:
        for method_name in ("stop", "close", "end"):
            method = getattr(session, method_name, None)
            if callable(method):
                method()
                return
