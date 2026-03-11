from __future__ import annotations

import os

import pytest

from core.config import QAConfig
from core.http_client import ApiClient


@pytest.mark.api
@pytest.mark.regression
def test_notify_invalid_sign_returns_fail(qa_config: QAConfig):
    if os.getenv("PAYMENT_ENABLED", "").lower() != "true":
        pytest.skip("PAYMENT_ENABLED is not true")
    c = ApiClient(qa_config.api_base_url, timeout_s=10)
    url = c.base_url + "/payment/notify/yungouos"
    resp = c.session.post(url, data={"out_trade_no": "QA_FAKE_ORDER", "sign": "BAD"}, timeout=c.timeout_s)
    resp.raise_for_status()
    assert resp.text.strip().upper() == "FAIL"
