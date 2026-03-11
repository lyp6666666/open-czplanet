from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from typing import Any

from core.http_client import ApiClient


@dataclass(frozen=True)
class YungouosNotifyParams:
    out_trade_no: str
    total_fee: str
    pay_no: str
    order_no: str
    pay_time: str
    sign: str


def yungouos_sign(client: ApiClient, params: dict[str, Any]) -> str:
    data = client.post_data("/internal/debug/payment/yungouos-sign", json=params)
    if not isinstance(data, str) or not data.strip():
        raise RuntimeError("sign_failed")
    return data.strip()


def build_success_notify(client: ApiClient, *, out_trade_no: str, amount_fen: int) -> YungouosNotifyParams:
    total_fee = f"{amount_fen / 100:.2f}"
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    payload: dict[str, Any] = {
        "out_trade_no": out_trade_no,
        "total_fee": total_fee,
        "pay_no": f"QA_PAY_{out_trade_no}",
        "order_no": f"QA_ORDER_{out_trade_no}",
        "pay_time": now,
    }
    sign = yungouos_sign(client, payload)
    return YungouosNotifyParams(
        out_trade_no=out_trade_no,
        total_fee=total_fee,
        pay_no=str(payload["pay_no"]),
        order_no=str(payload["order_no"]),
        pay_time=now,
        sign=sign,
    )
