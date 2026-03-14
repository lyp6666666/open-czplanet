from __future__ import annotations

from playwright.sync_api import Page


class CashierPayPage:
    def __init__(self, page: Page):
        self.page = page

    def goto(self, *, context_type: str, context_id: int):
        self.page.goto(f"/pay/cashier?contextType={context_type}&contextId={context_id}")

    def wait_ready(self):
        self.page.locator(".title", has_text="订单支付").wait_for()

    def get_order_no_text(self) -> str:
        return self.page.locator(".row .v.mono").first.inner_text().strip()

    def wait_qr_or_codeurl(self):
        self.page.locator(".card.pay").wait_for()
