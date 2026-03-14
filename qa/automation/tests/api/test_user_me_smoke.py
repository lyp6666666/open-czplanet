from __future__ import annotations

import uuid

import pytest

from api.assets_client import AssetsClient
from api.user_client import UserClient
from core.assertions import assert_has_keys, assert_non_empty_str
from core.http_client import ApiClient


@pytest.mark.api
@pytest.mark.smoke
def test_me_and_update_profile_and_upload_avatar(authed_client: ApiClient):
    user = UserClient(authed_client)
    me = user.me()
    assert_has_keys(me, ["id", "name", "phone", "userType"])
    assert_non_empty_str(me["phone"])

    new_name = f"qa-{uuid.uuid4().hex[:8]}"
    user.update_user_info({"baseUserInfo": {"name": new_name}})
    me2 = user.me()
    assert me2["name"] == new_name

    assets = AssetsClient(authed_client)
    png = bytes.fromhex(
        "89504e470d0a1a0a0000000d4948445200000001000000010802000000907753de"
        "0000000c4944415408d763f8ffff3f0005fe02fea7b1c4b80000000049454e44ae426082"
    )
    r = assets.upload_image(file_name="avatar.png", content=png, content_type="image/png", biz="avatar")
    assert_non_empty_str(r.url)
