from __future__ import annotations

import pytest

from core.config import QAConfig
from core.http_client import ApiClient
from ui.app_auth import add_auth_storage_to_page
from ui.pages.me_page import MePage


@pytest.mark.ui
@pytest.mark.smoke
def test_me_upload_avatar(page, context, qa_config: QAConfig, teacher_token: str):
    api = ApiClient(qa_config.api_base_url, timeout_s=10)
    api.set_bearer_token(teacher_token)
    me = api.get_data("/user/me")

    add_auth_storage_to_page(page, me, teacher_token)

    me_page = MePage(page)
    me_page.goto()

    png = bytes.fromhex(
        "89504e470d0a1a0a0000000d4948445200000001000000010802000000907753de"
        "0000000c4944415408d763f8ffff3f0005fe02fea7b1c4b80000000049454e44ae426082"
    )
    me_page.upload_avatar(name="avatar.png", content_type="image/png", content=png)
    me_page.click_save()
    me_page.wait_saved()
