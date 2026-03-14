from __future__ import annotations

import uuid

import pytest

from api.chat_client import ChatClient
from core.config import QAConfig
from core.http_client import ApiClient
from ui.app_auth import add_auth_storage_to_page
from ui.pages.chat_room_page import ChatRoomPage


@pytest.mark.ui
@pytest.mark.smoke
def test_chat_send_message(page, qa_config: QAConfig, teacher_token: str):
    api = ApiClient(qa_config.api_base_url, timeout_s=10)
    api.set_bearer_token(teacher_token)
    me = api.get_data("/user/me")

    add_auth_storage_to_page(page, me, teacher_token)

    chat_api = ChatClient(api)
    room_id = chat_api.start_chat(target_uid=113, greeting="hello")

    room = ChatRoomPage(page)
    room.goto(room_id, other_uid=113)

    msg = f"ui-{uuid.uuid4().hex[:6]}"
    room.send_text(msg)
    room.wait_message_visible(msg)
