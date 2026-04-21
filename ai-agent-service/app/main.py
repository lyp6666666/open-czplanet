from __future__ import annotations

from fastapi import FastAPI

from app.api.chat_summary import router as chat_summary_router
from app.api.health import router as health_router
from app.api.lesson_report import router as lesson_report_router
from app.api.realtime import router as realtime_router
from app.api.task import router as task_router
from app.core.config import get_settings
from app.storage.database import init_database


def create_app() -> FastAPI:
    settings = get_settings()
    init_database()
    app = FastAPI(
        title="ai-agent-service",
        version="0.1.0",
        description="AI agent microservice for lesson reports and IM summaries.",
    )
    app.state.settings = settings
    app.include_router(health_router)
    app.include_router(task_router)
    app.include_router(lesson_report_router)
    app.include_router(chat_summary_router)
    app.include_router(realtime_router)
    return app


app = create_app()
