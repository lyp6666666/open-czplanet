from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from core.http_client import ApiClient, ApiError


@dataclass(frozen=True)
class UploadResult:
    objectKey: str
    url: str
    contentType: str
    size: int


class AssetsClient:
    def __init__(self, client: ApiClient):
        self.client = client

    def upload_image(self, *, file_name: str, content: bytes, content_type: str, biz: str = "AVATAR") -> UploadResult:
        url = self.client.base_url + "/api/v1/assets/upload"
        files = {"file": (file_name, content, content_type)}
        data = {"biz": biz}
        resp = self.client.session.post(url, files=files, data=data, timeout=self.client.timeout_s)
        resp.raise_for_status()
        body = resp.json()
        if not isinstance(body, dict):
            raise RuntimeError("upload_invalid")
        code = int(body.get("code", -1))
        if code != 0:
            raise ApiError(code, body.get("message"))
        d = body.get("data")
        if not isinstance(d, dict):
            raise RuntimeError("upload_data_invalid")
        return UploadResult(
            objectKey=str(d.get("objectKey")),
            url=str(d.get("url")),
            contentType=str(d.get("contentType")),
            size=int(d.get("size")),
        )
