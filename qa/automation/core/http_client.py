from __future__ import annotations

from dataclasses import dataclass
from typing import Any

import requests


@dataclass(frozen=True)
class BaseResponse:
    code: int
    message: str | None
    data: Any


class ApiError(RuntimeError):
    def __init__(self, code: int, message: str | None):
        super().__init__(f"api_error code={code} message={message}")
        self.code = code
        self.message = message


class ApiClient:
    def __init__(self, base_url: str, timeout_s: float = 10.0):
        self.base_url = base_url.rstrip("/")
        self.timeout_s = timeout_s
        self.session = requests.Session()

    def set_bearer_token(self, token: str | None):
        if not token:
            self.session.headers.pop("Authorization", None)
            return
        self.session.headers["Authorization"] = f"Bearer {token}"

    def request_json(self, method: str, path: str, *, params: dict[str, Any] | None = None, json: Any = None) -> dict[str, Any]:
        url = self.base_url + path
        resp = self.session.request(method, url, params=params, json=json, timeout=self.timeout_s)
        resp.raise_for_status()
        body = resp.json()
        if not isinstance(body, dict):
            raise RuntimeError("invalid_json_body")
        return body

    def request_base_response(self, method: str, path: str, *, params: dict[str, Any] | None = None, json: Any = None) -> BaseResponse:
        body = self.request_json(method, path, params=params, json=json)
        code = int(body.get("code", -1))
        message = body.get("message")
        data = body.get("data")
        return BaseResponse(code=code, message=message, data=data)

    def request_data(self, method: str, path: str, *, params: dict[str, Any] | None = None, json: Any = None) -> Any:
        r = self.request_base_response(method, path, params=params, json=json)
        if r.code != 0:
            raise ApiError(r.code, r.message)
        return r.data

    def get_data(self, path: str, *, params: dict[str, Any] | None = None) -> Any:
        return self.request_data("GET", path, params=params)

    def post_data(self, path: str, *, json: Any = None) -> Any:
        return self.request_data("POST", path, json=json)
