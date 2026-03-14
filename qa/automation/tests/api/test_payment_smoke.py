from __future__ import annotations

import os

import pytest

from api.payment_client import PaymentClient
from core.config import QAConfig
from core.http_client import ApiClient


@pytest.mark.api
@pytest.mark.smoke
def test_prepay_and_poll_status(qa_config: QAConfig, teacher_token: str):
    brokerage_order_id = os.getenv("QA_BROKERAGE_ORDER_ID", "98001")
    if os.getenv("PAYMENT_ENABLED", "").lower() != "true":
        pytest.skip("PAYMENT_ENABLED is not true")

    c = ApiClient(qa_config.api_base_url, timeout_s=10)
    c.set_bearer_token(teacher_token)
    payment = PaymentClient(c)

    prepay = payment.prepay_brokerage(int(brokerage_order_id), "WECHAT")
    assert prepay.orderNo
    s = payment.get_order_status(prepay.orderNo)
    assert s.orderNo == prepay.orderNo
