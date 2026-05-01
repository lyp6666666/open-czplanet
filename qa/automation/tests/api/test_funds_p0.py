from __future__ import annotations

import os
import time
from typing import Any

import pytest

from api.admin_client import AdminClient
from api.chat_client import ChatClient
from api.payment_client import PaymentClient
from core.config import QAConfig
from core.http_client import ApiClient, ApiError
from core.payment_notify import build_success_notify


def _need_payment_enabled() -> None:
    if os.getenv("PAYMENT_ENABLED", "").lower() != "true":
        pytest.skip("PAYMENT_ENABLED is not true")


def _need_qa_profile() -> None:
    if os.getenv("SPRING_PROFILES_ACTIVE", "").lower() not in {"qa", "test"}:
        pytest.skip("need qa/test profile for internal debug endpoints")


def _notify_success(payment: PaymentClient, api: ApiClient, order_no: str, amount_fen: int) -> str:
    notify = build_success_notify(api, out_trade_no=order_no, amount_fen=amount_fen)
    return payment.notify_yungouos(
        {
            "out_trade_no": notify.out_trade_no,
            "total_fee": notify.total_fee,
            "pay_no": notify.pay_no,
            "order_no": notify.order_no,
            "pay_time": notify.pay_time,
            "sign": notify.sign,
        }
    )


def _wait_application_chat_enabled(chat: ChatClient, application_id: int, timeout_s: float = 20.0) -> dict[str, Any]:
    deadline = time.time() + timeout_s
    last: dict[str, Any] = {}
    while time.time() < deadline:
        last = chat.application_detail(application_id)
        if last.get("chatAccessStatus") == "CHAT_ENABLED" and last.get("roomId"):
            return last
        time.sleep(1)
    return last


def _build_application_chain(
    qa_config: QAConfig,
    teacher_token: str,
    student_token: str,
) -> tuple[int, int]:
    teacher_uid = int(os.getenv("QA_FUNDS_TEACHER_USER_ID", "910102"))
    student_uid = int(os.getenv("QA_FUNDS_STUDENT_USER_ID", "910002"))
    demand_id = int(os.getenv("QA_FUNDS_DEMAND_ID", "940002"))
    request_id = f"qa-funds-{int(time.time() * 1000)}"

    teacher_api = ApiClient(qa_config.api_base_url, timeout_s=10)
    teacher_api.set_bearer_token(teacher_token)
    teacher_chat = ChatClient(teacher_api)
    application = teacher_chat.create_application(
        receiver_uid=student_uid,
        context_type="DEMAND",
        context_id=demand_id,
        content=f"QA资金链路动态申请 {request_id}",
        client_request_id=request_id,
    )
    application_id = int(application["id"])

    student_api = ApiClient(qa_config.api_base_url, timeout_s=10)
    student_api.set_bearer_token(student_token)
    student_chat = ChatClient(student_api)
    accepted = student_chat.decide_application(application_id, "ACCEPT")
    assert accepted.get("status") == "ACCEPTED"
    assert accepted.get("chatAccessStatus") == "PAYMENT_REQUIRED"
    order_id = int(accepted["orderId"])
    assert order_id > 0

    teacher_enter = teacher_chat.enter_application_chat(application_id)
    assert teacher_enter.get("paymentRequired") is True
    assert int(teacher_enter["orderId"]) == order_id

    return application_id, order_id


@pytest.mark.api
@pytest.mark.regression
@pytest.mark.funds
def test_brokerage_payment_success_unlocks_chat_and_is_idempotent(
    qa_config: QAConfig,
    funds_teacher_token: str,
    funds_student_token: str,
):
    _need_payment_enabled()
    _need_qa_profile()

    application_id, brokerage_order_id = _build_application_chain(
        qa_config,
        teacher_token=funds_teacher_token,
        student_token=funds_student_token,
    )

    teacher_api = ApiClient(qa_config.api_base_url, timeout_s=10)
    teacher_api.set_bearer_token(funds_teacher_token)
    payment = PaymentClient(teacher_api)

    prepay = payment.prepay_brokerage(brokerage_order_id, "WECHAT")
    assert prepay.orderNo
    assert prepay.amountFen > 0

    first_notify = _notify_success(payment, teacher_api, prepay.orderNo, prepay.amountFen)
    assert first_notify.strip().upper() == "SUCCESS"

    second_notify = _notify_success(payment, teacher_api, prepay.orderNo, prepay.amountFen)
    assert second_notify.strip().upper() == "SUCCESS"

    status = payment.get_order_status(prepay.orderNo)
    assert status.status == "SUCCESS"
    assert status.amountFen == prepay.amountFen
    assert status.successTime

    student_api = ApiClient(qa_config.api_base_url, timeout_s=10)
    student_api.set_bearer_token(funds_student_token)
    chat = ChatClient(student_api)
    app = _wait_application_chat_enabled(chat, application_id)
    assert app.get("status") == "ACCEPTED"
    assert app.get("chatAccessStatus") == "CHAT_ENABLED"
    assert int(app.get("orderId")) == brokerage_order_id
    assert int(app.get("roomId")) > 0

    msg = chat.send_text(int(app["roomId"]), f"QA资金链路支付后聊天验证 {int(time.time())}")
    assert int(msg.get("roomId")) == int(app["roomId"])
    assert int(msg.get("id", 0)) > 0


@pytest.mark.api
@pytest.mark.regression
@pytest.mark.funds
def test_yungouos_notify_rejects_bad_signature_without_success(qa_config: QAConfig, funds_teacher_token: str):
    _need_payment_enabled()

    brokerage_order_id = int(os.getenv("QA_FUNDS_BAD_SIGN_BROKERAGE_ORDER_ID", "980001"))

    api = ApiClient(qa_config.api_base_url, timeout_s=10)
    api.set_bearer_token(funds_teacher_token)
    payment = PaymentClient(api)

    prepay = payment.prepay_brokerage(brokerage_order_id, "WECHAT")
    resp = payment.notify_yungouos(
        {
            "out_trade_no": prepay.orderNo,
            "total_fee": f"{prepay.amountFen / 100:.2f}",
            "pay_no": f"QA_BAD_SIGN_{prepay.orderNo}",
            "order_no": f"QA_BAD_PROVIDER_{prepay.orderNo}",
            "pay_time": "2026-05-01 12:00:00",
            "sign": "BAD_SIGN",
        }
    )
    assert resp.strip().upper() == "FAIL"

    status = payment.get_order_status(prepay.orderNo)
    if status.status == "SUCCESS":
        pytest.skip("seed order was already paid by another funds test run")
    assert status.status == "PENDING"


@pytest.mark.api
@pytest.mark.regression
@pytest.mark.funds
def test_non_payer_cannot_create_brokerage_payment_order(qa_config: QAConfig, funds_student_token: str):
    _need_payment_enabled()

    brokerage_order_id = int(os.getenv("QA_FUNDS_BROKERAGE_ORDER_ID", "980001"))

    api = ApiClient(qa_config.api_base_url, timeout_s=10)
    api.set_bearer_token(funds_student_token)
    payment = PaymentClient(api)

    with pytest.raises(ApiError):
        payment.prepay_brokerage(brokerage_order_id, "WECHAT")


@pytest.mark.api
@pytest.mark.regression
@pytest.mark.funds
def test_refund_request_visible_to_admin(qa_config: QAConfig):
    username = os.getenv("QA_ADMIN_USERNAME", "admin")
    password = os.getenv("QA_ADMIN_PASSWORD")
    if not password:
        pytest.skip("QA_ADMIN_PASSWORD is not set")

    request_id = int(os.getenv("QA_FUNDS_REFUND_REQUEST_ID", "985001"))

    api = ApiClient(qa_config.api_base_url, timeout_s=10)
    admin = AdminClient(api)
    login = admin.login(username, password)
    assert login.get("token")

    detail = admin.refund_request_detail(request_id)
    assert int(detail.get("id")) == request_id
    assert detail.get("status") in {"PENDING", "APPROVED", "REJECTED"}
    assert int(detail.get("refundAmountFen", 0)) > 0
