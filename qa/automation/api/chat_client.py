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

    def application_detail(self, application_id: int) -> dict[str, Any]:
        data = self.client.get_data(f"/chat/application/{int(application_id)}")
        if not isinstance(data, dict):
            raise RuntimeError("application_detail_invalid")
        return data

    def brokerage_order(self, order_id: int) -> dict[str, Any]:
        data = self.client.get_data(f"/chat/brokerage/order/{int(order_id)}")
        if not isinstance(data, dict):
            raise RuntimeError("brokerage_order_invalid")
        return data

    def refund_state(self, room_id: int) -> dict[str, Any]:
        data = self.client.get_data("/chat/refund/state", params={"roomId": int(room_id)})
        if not isinstance(data, dict):
            raise RuntimeError("refund_state_invalid")
        return data

    def apply_chat_refund(self, room_id: int, reason: str) -> dict[str, Any]:
        data = self.client.post_data("/chat/refund/apply", json={"roomId": int(room_id), "reason": reason})
        if not isinstance(data, dict):
            raise RuntimeError("chat_refund_apply_invalid")
        return data

    def create_application(
        self,
        *,
        receiver_uid: int,
        context_type: str,
        context_id: int,
        content: str,
        teaching_mode: str | None = None,
        client_request_id: str | None = None,
    ) -> dict[str, Any]:
        payload: dict[str, Any] = {
            "receiverUid": int(receiver_uid),
            "contextType": context_type,
            "contextId": int(context_id),
            "content": content,
        }
        if teaching_mode:
            payload["teachingMode"] = teaching_mode
        if client_request_id:
            payload["clientRequestId"] = client_request_id
        data = self.client.post_data("/chat/application", json=payload)
        if not isinstance(data, dict):
            raise RuntimeError("create_application_invalid")
        return data

    def decide_application(self, application_id: int, action: str) -> dict[str, Any]:
        data = self.client.post_data(f"/chat/application/{int(application_id)}/decision", json={"action": action})
        if not isinstance(data, dict):
            raise RuntimeError("decide_application_invalid")
        return data

    def enter_application_chat(self, application_id: int) -> dict[str, Any]:
        data = self.client.post_data(f"/chat/application/{int(application_id)}/enter-chat")
        if not isinstance(data, dict):
            raise RuntimeError("enter_application_chat_invalid")
        return data
