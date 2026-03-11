from __future__ import annotations

import pytest

from api.chat_client import ChatClient
from core.config import QAConfig
from core.http_client import ApiClient


@pytest.mark.api
@pytest.mark.smoke
def test_start_chat_send_and_ack(qa_config: QAConfig, teacher_token: str, student_token: str):
    teacher = ApiClient(qa_config.api_base_url, timeout_s=10)
    student = ApiClient(qa_config.api_base_url, timeout_s=10)
    teacher.set_bearer_token(teacher_token)
    student.set_bearer_token(student_token)

    chat_teacher = ChatClient(teacher)
    chat_student = ChatClient(student)

    room_id = chat_teacher.start_chat(target_uid=113, greeting="hello")
    chat_teacher.send_text(room_id, "ping")

    page = chat_student.get_msg_page(room_id, page_size=10)
    items = page.get("list") or []
    assert isinstance(items, list) and items

    last = items[0]
    msg = last.get("message") if isinstance(last, dict) else None
    msg_id = msg.get("id") if isinstance(msg, dict) else None
    assert isinstance(msg_id, int)

    chat_student.ack_read(room_id, msg_id)
