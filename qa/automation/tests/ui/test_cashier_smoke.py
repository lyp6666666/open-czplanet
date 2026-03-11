from __future__ import annotations

import os

import pytest

from core.config import QAConfig
from core.http_client import ApiClient
from ui.app_auth import add_auth_storage_to_page
from ui.pages.cashier_pay_page import CashierPayPage


@pytest.mark.ui
@pytest.mark.smoke
def test_cashier_prepay_page(page, qa_config: QAConfig, teacher_token: str):
    brokerage_order_id = os.getenv("QA_BROKERAGE_ORDER_ID", "98001")
    if os.getenv("PAYMENT_ENABLED", "").lower() != "true":
        pytest.skip("PAYMENT_ENABLED is not true")

    api = ApiClient(qa_config.api_base_url, timeout_s=10)
    api.set_bearer_token(teacher_token)
    me = api.get_data("/user/me")
    add_auth_storage_to_page(page, me, teacher_token)

    cashier = CashierPayPage(page)
    cashier.goto(context_type="BROKERAGE_ORDER", context_id=int(brokerage_order_id))
    cashier.wait_ready()
    assert cashier.get_order_no_text() != "-"
