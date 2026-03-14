from __future__ import annotations

import pytest

from core.config import QAConfig
from core.http_client import ApiClient


@pytest.mark.api
@pytest.mark.regression
def test_me_requires_auth(qa_config: QAConfig):
    c = ApiClient(qa_config.api_base_url, timeout_s=10)
    r = c.request_base_response("GET", "/user/me")
    assert r.code != 0
