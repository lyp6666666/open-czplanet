from __future__ import annotations

import os
from pathlib import Path
from typing import Generator

import pytest
from playwright.sync_api import Browser, BrowserContext, Page, sync_playwright

from core.config import QAConfig


@pytest.fixture(scope="session")
def browser(qa_config: QAConfig) -> Generator[Browser, None, None]:
    with sync_playwright() as p:
        b = p.chromium.launch(channel="chrome", headless=qa_config.headless)
        yield b
        b.close()


@pytest.fixture
def context(browser: Browser, qa_config: QAConfig) -> Generator[BrowserContext, None, None]:
    ctx = browser.new_context(base_url=qa_config.web_base_url)
    ctx.set_default_timeout(qa_config.playwright_timeout_ms)
    yield ctx
    ctx.close()


@pytest.fixture
def page(context: BrowserContext, request: pytest.FixtureRequest) -> Generator[Page, None, None]:
    p = context.new_page()
    setattr(request.node, "_page", p)
    yield p
    p.close()


@pytest.hookimpl(hookwrapper=True)
def pytest_runtest_makereport(item: pytest.Item, call: pytest.CallInfo):
    outcome = yield
    rep = outcome.get_result()
    if rep.when != "call" or rep.passed:
        return
    p = getattr(item, "_page", None)
    if p is None:
        return
    out_dir = Path(item.config.rootpath) / "artifacts" / "ui"
    out_dir.mkdir(parents=True, exist_ok=True)
    safe = "".join(c if c.isalnum() or c in {"-", "_"} else "_" for c in item.nodeid)
    png = out_dir / f"{safe}.png"
    try:
        p.screenshot(path=str(png), full_page=True)
    except Exception:
        pass
