from __future__ import annotations

from typing import Any

from core.http_client import ApiClient


class JobClient:
    def __init__(self, client: ApiClient):
        self.client = client

    def feed(self, *, page_size: int = 10, cursor: str | None = None, q: str | None = None) -> dict[str, Any]:
        params: dict[str, Any] = {"pageSize": int(page_size)}
        if cursor:
            params["cursor"] = cursor
        if q:
            params["q"] = q
        data = self.client.get_data("/api/v1/parent/jobs/feed", params=params)
        if not isinstance(data, dict):
            raise RuntimeError("feed_invalid")
        return data

    def view(self, job_id: int) -> dict[str, Any]:
        data = self.client.get_data(f"/api/v1/parent/jobs/{int(job_id)}/view")
        if not isinstance(data, dict):
            raise RuntimeError("view_invalid")
        return data
