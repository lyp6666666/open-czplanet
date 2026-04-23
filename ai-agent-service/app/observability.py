from __future__ import annotations

from collections import Counter
from dataclasses import dataclass, field
from threading import Lock
from time import perf_counter
from fastapi import FastAPI, Request, Response


@dataclass
class _MetricsState:
    request_total: Counter[tuple[str, str, int]] = field(default_factory=Counter)
    request_duration_count: Counter[tuple[str, str]] = field(default_factory=Counter)
    request_duration_sum: Counter[tuple[str, str]] = field(default_factory=Counter)
    in_progress: int = 0
    lock: Lock = field(default_factory=Lock)


def register_metrics(app: FastAPI) -> None:
    state = _MetricsState()
    app.state.metrics_state = state

    @app.middleware("http")
    async def record_http_metrics(request: Request, call_next):
        method = request.method.upper()
        started = perf_counter()
        with state.lock:
            state.in_progress += 1
        status_code = 500
        path_template = _normalize_path(request)
        try:
            response = await call_next(request)
            status_code = response.status_code
            route = request.scope.get("route")
            if route is not None and getattr(route, "path", None):
                path_template = route.path
            return response
        finally:
            elapsed = perf_counter() - started
            with state.lock:
                state.in_progress -= 1
                state.request_total[(method, path_template, status_code)] += 1
                state.request_duration_count[(method, path_template)] += 1
                state.request_duration_sum[(method, path_template)] += elapsed

    @app.get("/metrics", include_in_schema=False)
    def metrics() -> Response:
        body = render_metrics(state)
        return Response(content=body, media_type="text/plain; version=0.0.4; charset=utf-8")


def render_metrics(state: _MetricsState) -> str:
    lines = [
        "# HELP ai_agent_service_up ai-agent-service process health indicator.",
        "# TYPE ai_agent_service_up gauge",
        "ai_agent_service_up 1",
        "# HELP ai_agent_http_requests_total Total HTTP requests handled by ai-agent-service.",
        "# TYPE ai_agent_http_requests_total counter",
    ]
    with state.lock:
        request_total_items = sorted(state.request_total.items())
        duration_count_items = sorted(state.request_duration_count.items())
        duration_sum_items = sorted(state.request_duration_sum.items())
        in_progress = state.in_progress

    lines.extend(
        _format_sample(
            "ai_agent_http_requests_total",
            {"method": method, "path": path, "status": str(status)},
            float(value),
        )
        for (method, path, status), value in request_total_items
    )
    lines.extend(
        [
            "# HELP ai_agent_http_request_duration_seconds_count Total completed HTTP requests by route.",
            "# TYPE ai_agent_http_request_duration_seconds_count counter",
        ]
    )
    lines.extend(
        _format_sample(
            "ai_agent_http_request_duration_seconds_count",
            {"method": method, "path": path},
            float(value),
        )
        for (method, path), value in duration_count_items
    )
    lines.extend(
        [
            "# HELP ai_agent_http_request_duration_seconds_sum Total request duration seconds by route.",
            "# TYPE ai_agent_http_request_duration_seconds_sum counter",
        ]
    )
    lines.extend(
        _format_sample(
            "ai_agent_http_request_duration_seconds_sum",
            {"method": method, "path": path},
            float(value),
        )
        for (method, path), value in duration_sum_items
    )
    lines.extend(
        [
            "# HELP ai_agent_http_requests_in_progress In-flight HTTP requests.",
            "# TYPE ai_agent_http_requests_in_progress gauge",
            f"ai_agent_http_requests_in_progress {in_progress}",
        ]
    )
    return "\n".join(lines) + "\n"


def _format_sample(metric: str, labels: dict[str, str], value: float) -> str:
    serialized_labels = ",".join(f'{key}="{_escape_label(label)}"' for key, label in labels.items())
    return f"{metric}{{{serialized_labels}}} {value}"


def _escape_label(value: str) -> str:
    return value.replace("\\", "\\\\").replace("\n", "\\n").replace('"', '\\"')


def _normalize_path(request: Request) -> str:
    path = request.url.path or "/"
    return path if path.startswith("/") else f"/{path}"
