from __future__ import annotations

from typing import Any

from core.http_client import ApiClient


class UserClient:
    def __init__(self, client: ApiClient):
        self.client = client

    def me(self) -> dict[str, Any]:
        data = self.client.get_data("/user/me")
        if not isinstance(data, dict):
            raise RuntimeError("user_me_invalid")
        return data

    def update_user_info(self, payload: dict[str, Any]) -> str:
        data = self.client.post_data("/user/updateUserInfo", json=payload)
        if not isinstance(data, str):
            raise RuntimeError("update_user_info_invalid")
        return data

    def send_code(self, phone: str) -> str:
        data = self.client.post_data("/user/sendcode", json={"phone": phone})
        if not isinstance(data, str):
            raise RuntimeError("send_code_invalid")
        return data

    def login_or_register(self, phone: str, code: str, role_enum: str) -> dict[str, Any]:
        data = self.client.post_data(
            "/user/loginOrRegister",
            json={"phone": phone, "code": code, "userRoleEnum": role_enum},
        )
        if not isinstance(data, dict):
            raise RuntimeError("login_invalid")
        return data
