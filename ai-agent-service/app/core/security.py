from __future__ import annotations

from typing import Optional

from fastapi import Header, HTTPException, status

from app.core.config import get_settings


def verify_internal_token(x_ai_agent_token: Optional[str] = Header(default=None)) -> None:
    settings = get_settings()
    if not settings.internal_token:
        return
    if x_ai_agent_token != settings.internal_token:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="invalid_internal_token")
