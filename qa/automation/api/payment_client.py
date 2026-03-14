from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from core.http_client import ApiClient


@dataclass(frozen=True)
class PrepayResult:
    orderNo: str
    amountFen: int
    channel: str
    expireTime: Any
    codeUrl: str | None
    qrCodeUrl: str | None


@dataclass(frozen=True)
class PaymentStatus:
    orderNo: str
    status: str
    amountFen: int
    channel: str
    expireTime: Any
    successTime: Any


class PaymentClient:
    def __init__(self, client: ApiClient):
        self.client = client

    def prepay_brokerage(self, context_id: int, channel: str) -> PrepayResult:
        data = self.client.post_data(
            "/payment/prepay",
            json={"contextType": "BROKERAGE_ORDER", "contextId": int(context_id), "channel": channel},
        )
        if not isinstance(data, dict):
            raise RuntimeError("prepay_invalid")
        return PrepayResult(
            orderNo=str(data.get("orderNo")),
            amountFen=int(data.get("amountFen")),
            channel=str(data.get("channel")),
            expireTime=data.get("expireTime"),
            codeUrl=(data.get("codeUrl") if isinstance(data.get("codeUrl"), str) else None),
            qrCodeUrl=(data.get("qrCodeUrl") if isinstance(data.get("qrCodeUrl"), str) else None),
        )

    def get_order_status(self, order_no: str) -> PaymentStatus:
        data = self.client.get_data(f"/payment/orders/{order_no}")
        if not isinstance(data, dict):
            raise RuntimeError("status_invalid")
        return PaymentStatus(
            orderNo=str(data.get("orderNo")),
            status=str(data.get("status")),
            amountFen=int(data.get("amountFen")),
            channel=str(data.get("channel")),
            expireTime=data.get("expireTime"),
            successTime=data.get("successTime"),
        )

    def notify_yungouos(self, params: dict[str, str]) -> str:
        url = self.client.base_url + "/payment/notify/yungouos"
        resp = self.client.session.post(url, data=params, timeout=self.client.timeout_s)
        resp.raise_for_status()
        return resp.text
