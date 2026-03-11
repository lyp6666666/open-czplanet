from __future__ import annotations

import os

import pytest

from api.payment_client import PaymentClient
from core.config import QAConfig
from core.http_client import ApiClient
from core.payment_notify import build_success_notify
from ui.app_auth import add_auth_storage_to_page
from ui.pages.cashier_pay_page import CashierPayPage


@pytest.mark.e2e
@pytest.mark.regression
def test_cashier_paid_after_notify(page, qa_config: QAConfig, teacher_token: str):
    if os.getenv("PAYMENT_ENABLED", "").lower() != "true":
        pytest.skip("PAYMENT_ENABLED is not true")
    if os.getenv("SPRING_PROFILES_ACTIVE", "").lower() not in {"qa", "test"}:
        pytest.skip("need qa/test profile for internal debug endpoints")

    brokerage_order_id = int(os.getenv("QA_BROKERAGE_ORDER_ID", "98001"))

    api = ApiClient(qa_config.api_base_url, timeout_s=10)
    api.set_bearer_token(teacher_token)
    me = api.get_data("/user/me")
    add_auth_storage_to_page(page, me, teacher_token)

    cashier = CashierPayPage(page)
    cashier.goto(context_type="BROKERAGE_ORDER", context_id=brokerage_order_id)
    cashier.wait_ready()

    order_no = cashier.get_order_no_text()
    assert order_no and order_no != "-"

    pay = PaymentClient(api)
    status = pay.get_order_status(order_no)
    notify = build_success_notify(api, out_trade_no=order_no, amount_fen=status.amountFen)
    resp = pay.notify_yungouos(
        {
            "out_trade_no": notify.out_trade_no,
            "total_fee": notify.total_fee,
            "pay_no": notify.pay_no,
            "order_no": notify.order_no,
            "pay_time": notify.pay_time,
            "sign": notify.sign,
        }
    )
    assert resp.strip().upper() == "SUCCESS"

    page.locator(".g-ok", has_text="支付成功").wait_for(timeout=30000)
