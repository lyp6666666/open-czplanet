from __future__ import annotations

import uuid

import pytest

from api.chat_client import ChatClient
from core.config import QAConfig
from core.http_client import ApiClient
from ui.app_auth import add_auth_storage_to_page
from ui.pages.chat_room_page import ChatRoomPage


@pytest.mark.e2e
@pytest.mark.regression
def test_chat_send_and_ack_e2e(page, qa_config: QAConfig, teacher_token: str, student_token: str):
    teacher = ApiClient(qa_config.api_base_url, timeout_s=10)
    student = ApiClient(qa_config.api_base_url, timeout_s=10)
    teacher.set_bearer_token(teacher_token)
    student.set_bearer_token(student_token)

    room_id = ChatClient(teacher).start_chat(target_uid=113, greeting="hello")

    student_me = student.get_data("/user/me")
    add_auth_storage_to_page(page, student_me, student_token)

    room = ChatRoomPage(page)
    room.goto(room_id, other_uid=206)

    msg = f"e2e-{uuid.uuid4().hex[:6]}"
    ChatClient(teacher).send_text(room_id, msg)
    room.wait_message_visible(msg)

    page_data = ChatClient(student).get_msg_page(room_id, page_size=10)
    items = page_data.get("list") or []
    assert isinstance(items, list) and items
    last = items[0]
    message = last.get("message") if isinstance(last, dict) else None
    msg_id = message.get("id") if isinstance(message, dict) else None
    assert isinstance(msg_id, int)
    ChatClient(student).ack_read(room_id, msg_id)
