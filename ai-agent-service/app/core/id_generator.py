from __future__ import annotations

from uuid import uuid4


def new_task_id(prefix: str = "task") -> str:
    return f"{prefix}_{uuid4().hex}"
