from __future__ import annotations

from collections import defaultdict
from threading import Lock

from redis import Redis
from rq import Queue

from app.core.config import get_settings


class InMemoryRedis:
    def __init__(self):
        self._kv: dict[str, str] = {}
        self._lists: dict[str, list[str]] = defaultdict(list)
        self._lock = Lock()

    def set(self, key: str, value: str) -> bool:
        with self._lock:
            self._kv[key] = value
        return True

    def get(self, key: str):
        with self._lock:
            return self._kv.get(key)

    def rpush(self, key: str, value: str) -> int:
        with self._lock:
            self._lists[key].append(value)
            return len(self._lists[key])

    def ltrim(self, key: str, start: int, end: int) -> bool:
        with self._lock:
            values = self._lists.get(key, [])
            if not values:
                return True
            normalized_start = max(0, len(values) + start) if start < 0 else start
            normalized_end = len(values) + end if end < 0 else end
            normalized_end = min(len(values) - 1, normalized_end)
            if normalized_start > normalized_end:
                self._lists[key] = []
            else:
                self._lists[key] = values[normalized_start:normalized_end + 1]
        return True

    def lrange(self, key: str, start: int, end: int):
        with self._lock:
            values = list(self._lists.get(key, []))
        if not values:
            return []
        normalized_start = max(0, len(values) + start) if start < 0 else start
        normalized_end = len(values) + end if end < 0 else end
        normalized_end = min(len(values) - 1, normalized_end)
        if normalized_start > normalized_end:
            return []
        return values[normalized_start:normalized_end + 1]

    def publish(self, channel: str, message: str) -> int:
        return 0


_memory_redis = InMemoryRedis()


def get_redis() -> Redis | InMemoryRedis:
    settings = get_settings()
    if settings.redis_url.startswith("memory://"):
        return _memory_redis
    return Redis.from_url(settings.redis_url)


def get_queue() -> Queue:
    settings = get_settings()
    return Queue(settings.queue_name, connection=get_redis())
