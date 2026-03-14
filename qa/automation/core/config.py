from __future__ import annotations

import os
from dataclasses import dataclass

from dotenv import load_dotenv


@dataclass(frozen=True)
class QAConfig:
    api_base_url: str
    web_base_url: str
    jwt_secret: str | None
    jwt_issuer: str
    headless: bool
    playwright_timeout_ms: int


def _bool(v: str | None, default: bool) -> bool:
    if v is None:
        return default
    s = v.strip().lower()
    if s in {"1", "true", "yes", "y", "on"}:
        return True
    if s in {"0", "false", "no", "n", "off"}:
        return False
    return default


def load_config() -> QAConfig:
    load_dotenv()
    api_base_url = os.getenv("QA_API_BASE_URL", "http://localhost:8080").rstrip("/")
    web_base_url = os.getenv("QA_WEB_BASE_URL", "http://localhost:5173").rstrip("/")
    jwt_secret = os.getenv("QA_JWT_SECRET") or os.getenv("JWT_SECRET_PRIMARY")
    jwt_issuer = os.getenv("QA_JWT_ISSUER", "ai-tutor")
    headless = _bool(os.getenv("QA_HEADLESS"), True)
    playwright_timeout_ms = int(os.getenv("QA_PLAYWRIGHT_TIMEOUT_MS", "15000"))
    return QAConfig(
        api_base_url=api_base_url,
        web_base_url=web_base_url,
        jwt_secret=jwt_secret,
        jwt_issuer=jwt_issuer,
        headless=headless,
        playwright_timeout_ms=playwright_timeout_ms,
    )
