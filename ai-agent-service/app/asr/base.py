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
