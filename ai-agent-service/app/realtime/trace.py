from __future__ import annotations

import json
import logging
from typing import Any, Dict


logger = logging.getLogger("ai_agent.realtime.trace")


def trace_realtime_agent(event: str, **fields: Any) -> None:
    payload: Dict[str, Any] = {"event": event, **fields}
    logger.info(json.dumps(payload, ensure_ascii=False, default=str))
