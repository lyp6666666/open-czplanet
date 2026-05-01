from __future__ import annotations

from typing import Any

from core.http_client import ApiClient


class AdminClient:
    def __init__(self, client: ApiClient):
        self.client = client

    def login(self, username: str, password: str) -> dict[str, Any]:
        data = self.client.post_data("/api/admin/auth/login", json={"username": username, "password": password})
        if not isinstance(data, dict):
            raise RuntimeError("admin_login_invalid")
        token = data.get("token")
        if isinstance(token, str) and token.strip():
            self.client.set_bearer_token(token.strip())
        return data

    def refund_requests(
        self,
        *,
        status: str | None = None,
        type_: str | None = None,
        page: int = 1,
        size: int = 20,
    ) -> dict[str, Any]:
        params: dict[str, Any] = {"page": int(page), "size": int(size)}
        if status:
            params["status"] = status
        if type_:
            params["type"] = type_
        data = self.client.get_data("/api/admin/refund/requests", params=params)
        if not isinstance(data, dict):
            raise RuntimeError("refund_requests_invalid")
        return data

    def refund_request_detail(self, request_id: int) -> dict[str, Any]:
        data = self.client.get_data(f"/api/admin/refund/requests/{int(request_id)}")
        if not isinstance(data, dict):
            raise RuntimeError("refund_request_detail_invalid")
        return data

    def approve_refund_request(self, request_id: int, note: str | None = None) -> bool:
        data = self.client.post_data(f"/api/admin/refund/requests/{int(request_id)}/approve", json={"note": note})
        return bool(data)
