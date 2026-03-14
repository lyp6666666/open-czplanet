from __future__ import annotations

from typing import Any

from core.http_client import ApiClient


class ChatClient:
    def __init__(self, client: ApiClient):
        self.client = client

    def get_or_create_room(self, target_uid: int) -> int:
        data = self.client.post_data("/chat/room", json={"targetUid": int(target_uid)})
        return int(data)

    def start_chat(self, target_uid: int, greeting: str | None = None) -> int:
        payload: dict[str, Any] = {"targetUid": int(target_uid)}
        if greeting is not None:
            payload["greeting"] = greeting
        data = self.client.post_data("/chat/room/start", json=payload)
        return int(data)

    def send_text(self, room_id: int, content: str) -> dict[str, Any]:
        data = self.client.post_data(
            "/chat/msg",
            json={"roomId": int(room_id), "msgType": 1, "body": {"content": content}},
        )
        if not isinstance(data, dict):
            raise RuntimeError("send_msg_invalid")
        return data

    def get_msg_page(self, room_id: int, page_size: int = 10, cursor: str | None = None) -> dict[str, Any]:
        params: dict[str, Any] = {"roomId": int(room_id), "pageSize": int(page_size)}
        if cursor:
            params["cursor"] = cursor
        data = self.client.get_data("/chat/public/msg/page", params=params)
        if not isinstance(data, dict):
            raise RuntimeError("msg_page_invalid")
        return data

    def ack_read(self, room_id: int, last_read_msg_id: int) -> bool:
        data = self.client.post_data("/chat/read/ack", json={"roomId": int(room_id), "lastReadMsgId": int(last_read_msg_id)})
        return bool(data)
