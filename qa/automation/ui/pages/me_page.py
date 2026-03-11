from __future__ import annotations

from playwright.sync_api import Page


class MePage:
    def __init__(self, page: Page):
        self.page = page

    def goto(self):
        self.page.goto("/me")

    def upload_avatar(self, *, name: str, content_type: str, content: bytes):
        self.page.locator("input.avatar-file").set_input_files(
            {"name": name, "mimeType": content_type, "buffer": content}
        )

    def click_save(self):
        self.page.locator("button.btn-primary").click()

    def wait_saved(self):
        self.page.locator(".hint.ok", has_text="已保存").wait_for()
