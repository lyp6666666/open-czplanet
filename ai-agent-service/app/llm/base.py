from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any, Dict


class LLMProvider(ABC):
    @abstractmethod
    def generate_lesson_report(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        raise NotImplementedError

    @abstractmethod
    def generate_chat_summary(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        raise NotImplementedError
