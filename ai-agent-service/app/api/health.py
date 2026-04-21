from __future__ import annotations

from fastapi import APIRouter

from app.core.config import get_settings
from app.schemas.common import ApiResponse, ok

router = APIRouter()


@router.get("/health", response_model=ApiResponse[dict])
def health() -> ApiResponse[dict]:
    settings = get_settings()
    return ok(
        {
            "status": "ok",
            "service": "ai-agent-service",
            "env": settings.env,
            "llmProvider": settings.llm_provider,
            "asyncWorker": settings.use_async_worker,
        }
    )
