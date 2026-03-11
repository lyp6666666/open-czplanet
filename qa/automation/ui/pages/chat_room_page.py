from __future__ import annotations

from playwright.sync_api import Page


class ChatRoomPage:
    def __init__(self, page: Page):
        self.page = page

    def goto(self, room_id: int, other_uid: int | None = None):
        url = f"/chat/{room_id}"
        if other_uid is not None:
            url += f"?otherUid={other_uid}"
        self.page.goto(url)

    def send_text(self, content: str):
        self.page.locator(".composer .send input.input").fill(content)
        self.page.locator(".composer .send button.btn-primary").click()

    def wait_message_visible(self, content: str):
        self.page.locator(".bubble", has_text=content).first.wait_for()
