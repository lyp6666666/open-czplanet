from __future__ import annotations

from contextlib import contextmanager
from pathlib import Path
from typing import Iterator, Optional

from sqlalchemy import JSON, DateTime, Integer, String, Text, create_engine, func
from sqlalchemy.orm import DeclarativeBase, Mapped, Session, mapped_column, sessionmaker

from app.core.config import ROOT_DIR, get_settings


def _normalize_database_url(database_url: str) -> str:
    if database_url.startswith("sqlite:///./"):
        db_path = ROOT_DIR / database_url.removeprefix("sqlite:///./")
        db_path.parent.mkdir(parents=True, exist_ok=True)
        return f"sqlite:///{db_path}"
    if database_url.startswith("sqlite:///"):
        path = Path(database_url.removeprefix("sqlite:///"))
        path.parent.mkdir(parents=True, exist_ok=True)
    return database_url


class Base(DeclarativeBase):
    pass


class AiTask(Base):
    __tablename__ = "ai_task"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    task_id: Mapped[str] = mapped_column(String(64), unique=True, index=True, nullable=False)
    task_type: Mapped[str] = mapped_column(String(64), nullable=False)
    biz_id: Mapped[str] = mapped_column(String(64), index=True, nullable=False)
    status: Mapped[str] = mapped_column(String(32), nullable=False)
    progress: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    message: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    input_json: Mapped[Optional[dict]] = mapped_column(JSON, nullable=True)
    output_json: Mapped[Optional[dict]] = mapped_column(JSON, nullable=True)
    error_message: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    created_at: Mapped[str] = mapped_column(DateTime, server_default=func.now(), nullable=False)
    updated_at: Mapped[str] = mapped_column(
        DateTime, server_default=func.now(), onupdate=func.now(), nullable=False
    )


class AiLessonReport(Base):
    __tablename__ = "ai_lesson_report"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    lesson_id: Mapped[int] = mapped_column(Integer, unique=True, index=True, nullable=False)
    task_id: Mapped[str] = mapped_column(String(64), unique=True, index=True, nullable=False)
    teacher_id: Mapped[Optional[int]] = mapped_column(Integer, nullable=True)
    student_id: Mapped[Optional[int]] = mapped_column(Integer, nullable=True)
    status: Mapped[str] = mapped_column(String(32), nullable=False)
    report_json: Mapped[Optional[dict]] = mapped_column(JSON, nullable=True)
    teacher_edited_json: Mapped[Optional[dict]] = mapped_column(JSON, nullable=True)
    created_at: Mapped[str] = mapped_column(DateTime, server_default=func.now(), nullable=False)
    updated_at: Mapped[str] = mapped_column(
        DateTime, server_default=func.now(), onupdate=func.now(), nullable=False
    )


class AiChatSummary(Base):
    __tablename__ = "ai_chat_summary"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    room_id: Mapped[int] = mapped_column(Integer, unique=True, index=True, nullable=False)
    task_id: Mapped[str] = mapped_column(String(64), unique=True, index=True, nullable=False)
    summary_json: Mapped[Optional[dict]] = mapped_column(JSON, nullable=True)
    message_start_id: Mapped[Optional[int]] = mapped_column(Integer, nullable=True)
    message_end_id: Mapped[Optional[int]] = mapped_column(Integer, nullable=True)
    created_at: Mapped[str] = mapped_column(DateTime, server_default=func.now(), nullable=False)
    updated_at: Mapped[str] = mapped_column(
        DateTime, server_default=func.now(), onupdate=func.now(), nullable=False
    )


class AiLiveLessonSession(Base):
    __tablename__ = "ai_live_lesson_session"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    lesson_id: Mapped[int] = mapped_column(Integer, unique=True, index=True, nullable=False)
    session_id: Mapped[str] = mapped_column(String(64), unique=True, index=True, nullable=False)
    teacher_id: Mapped[Optional[int]] = mapped_column(Integer, nullable=True)
    student_id: Mapped[Optional[int]] = mapped_column(Integer, nullable=True)
    subject: Mapped[Optional[str]] = mapped_column(String(64), nullable=True)
    grade: Mapped[Optional[str]] = mapped_column(String(64), nullable=True)
    course_type: Mapped[str] = mapped_column(String(32), nullable=False)
    mode: Mapped[str] = mapped_column(String(32), nullable=False)
    asr_enabled: Mapped[bool] = mapped_column(nullable=False, default=False)
    llm_enabled: Mapped[bool] = mapped_column(nullable=False, default=True)
    status: Mapped[str] = mapped_column(String(32), nullable=False)
    created_at: Mapped[str] = mapped_column(DateTime, server_default=func.now(), nullable=False)
    updated_at: Mapped[str] = mapped_column(
        DateTime, server_default=func.now(), onupdate=func.now(), nullable=False
    )


class AiLessonTranscript(Base):
    __tablename__ = "ai_lesson_transcript"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    lesson_id: Mapped[int] = mapped_column(Integer, index=True, nullable=False)
    segment_index: Mapped[int] = mapped_column(Integer, nullable=False)
    speaker: Mapped[str] = mapped_column(String(32), nullable=False)
    start_ms: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    end_ms: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    text: Mapped[str] = mapped_column(Text, nullable=False)
    is_final: Mapped[bool] = mapped_column(nullable=False, default=True)
    created_at: Mapped[str] = mapped_column(DateTime, server_default=func.now(), nullable=False)


class AiLessonStageSummary(Base):
    __tablename__ = "ai_lesson_stage_summary"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    lesson_id: Mapped[int] = mapped_column(Integer, index=True, nullable=False)
    stage_index: Mapped[int] = mapped_column(Integer, nullable=False)
    summary_json: Mapped[Optional[dict]] = mapped_column(JSON, nullable=True)
    created_at: Mapped[str] = mapped_column(DateTime, server_default=func.now(), nullable=False)


settings = get_settings()
engine = create_engine(
    _normalize_database_url(settings.database_url),
    pool_pre_ping=True,
    connect_args={"check_same_thread": False} if settings.database_url.startswith("sqlite") else {},
)
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False, expire_on_commit=False)


def init_database() -> None:
    Base.metadata.create_all(bind=engine)


@contextmanager
def session_scope() -> Iterator[Session]:
    session = SessionLocal()
    try:
        yield session
        session.commit()
    except Exception:
        session.rollback()
        raise
    finally:
        session.close()
