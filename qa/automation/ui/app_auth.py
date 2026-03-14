from __future__ import annotations

from typing import Any

from playwright.sync_api import BrowserContext, Page


def add_auth_storage(context: BrowserContext, user: dict[str, Any], token: str) -> None:
    payload = {**user, "token": token}
    context.add_init_script(
        """(payload) => {
  localStorage.setItem('ai_tutor_token', payload.token);
  localStorage.setItem('ai_tutor_user', JSON.stringify(payload));
}""",
        payload,
    )


def add_auth_storage_to_page(page: Page, user: dict[str, Any], token: str) -> None:
    payload = {**user, "token": token}
    page.add_init_script(
        """(payload) => {
  localStorage.setItem('ai_tutor_token', payload.token);
  localStorage.setItem('ai_tutor_user', JSON.stringify(payload));
}""",
        payload,
    )
