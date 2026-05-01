from __future__ import annotations

from typing import Any

from core.http_client import ApiClient


class LiveClient:
    def __init__(self, client: ApiClient):
        self.client = client

    def by_course(self, course_id: int) -> dict[str, Any]:
        data = self.client.get_data(f"/live/sessions/by-course/{int(course_id)}")
        if not isinstance(data, dict):
            raise RuntimeError("live_by_course_invalid")
        return data

    def prepare(self, course_id: int, *, client_type: str, source_page: str | None = None) -> dict[str, Any]:
        payload: dict[str, Any] = {"clientType": client_type}
        if source_page:
            payload["sourcePage"] = source_page
        data = self.client.post_data(f"/live/sessions/by-course/{int(course_id)}/prepare", json=payload)
        if not isinstance(data, dict):
            raise RuntimeError("live_prepare_invalid")
        return data

    def ai_result(self, session_id: int) -> dict[str, Any]:
        data = self.client.get_data(f"/live/sessions/{int(session_id)}/ai/result")
        if not isinstance(data, dict):
            raise RuntimeError("live_ai_result_invalid")
        return data
