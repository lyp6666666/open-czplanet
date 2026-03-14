from __future__ import annotations

import pytest

from api.chat_client import ChatClient
from core.config import QAConfig
from core.http_client import ApiClient


@pytest.mark.api
@pytest.mark.regression
def test_send_msg_requires_body(qa_config: QAConfig, teacher_token: str):
    c = ApiClient(qa_config.api_base_url, timeout_s=10)
    c.set_bearer_token(teacher_token)
    chat = ChatClient(c)
    room_id = chat.start_chat(target_uid=113, greeting=None)
    r = c.request_base_response("POST", "/chat/msg", json={"roomId": room_id, "msgType": 1})
    assert r.code != 0
