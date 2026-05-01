from __future__ import annotations

from collections import Counter
from dataclasses import dataclass, field
from threading import Lock
from time import perf_counter
from fastapi import FastAPI, Request, Response


_agent_metrics_lock = Lock()
_agent_trigger_total: Counter[str] = Counter()
_agent_patch_total: Counter[tuple[str, str]] = Counter()
_agent_guard_reject_total: Counter[str] = Counter()
_agent_llm_calls_total: Counter[tuple[str, str]] = Counter()
_agent_first_summary_latency_sum: Counter[str] = Counter()
_agent_first_summary_latency_count: Counter[str] = Counter()


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
    with _agent_metrics_lock:
        trigger_items = sorted(_agent_trigger_total.items())
        patch_items = sorted(_agent_patch_total.items())
        guard_items = sorted(_agent_guard_reject_total.items())
        llm_items = sorted(_agent_llm_calls_total.items())
        first_latency_sum_items = sorted(_agent_first_summary_latency_sum.items())
        first_latency_count_items = sorted(_agent_first_summary_latency_count.items())

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
    lines.extend(
        [
            "# HELP realtime_agent_trigger_total Realtime classroom agent trigger decisions.",
            "# TYPE realtime_agent_trigger_total counter",
        ]
    )
    lines.extend(
        _format_sample("realtime_agent_trigger_total", {"reason": reason}, float(value))
        for reason, value in trigger_items
    )
    lines.extend(
        [
            "# HELP realtime_agent_patch_total Realtime classroom summary patches by type and status.",
            "# TYPE realtime_agent_patch_total counter",
        ]
    )
    lines.extend(
        _format_sample(
            "realtime_agent_patch_total",
            {"type": patch_type, "status": status},
            float(value),
        )
        for (patch_type, status), value in patch_items
    )
    lines.extend(
        [
            "# HELP realtime_agent_guard_reject_total Realtime classroom guard rejects by reason.",
            "# TYPE realtime_agent_guard_reject_total counter",
        ]
    )
    lines.extend(
        _format_sample("realtime_agent_guard_reject_total", {"reason": reason}, float(value))
        for reason, value in guard_items
    )
    lines.extend(
        [
            "# HELP realtime_agent_llm_calls_total Realtime classroom LLM calls by agent and model tier.",
            "# TYPE realtime_agent_llm_calls_total counter",
        ]
    )
    lines.extend(
        _format_sample(
            "realtime_agent_llm_calls_total",
            {"agent": agent, "model_tier": model_tier},
            float(value),
        )
        for (agent, model_tier), value in llm_items
    )
    lines.extend(
        [
            "# HELP realtime_agent_first_summary_latency_seconds First realtime summary latency seconds.",
            "# TYPE realtime_agent_first_summary_latency_seconds summary",
        ]
    )
    lines.extend(
        _format_sample(
            "realtime_agent_first_summary_latency_seconds_sum",
            {"bucket": bucket},
            float(value),
        )
        for bucket, value in first_latency_sum_items
    )
    lines.extend(
        _format_sample(
            "realtime_agent_first_summary_latency_seconds_count",
            {"bucket": bucket},
            float(value),
        )
        for bucket, value in first_latency_count_items
    )
    return "\n".join(lines) + "\n"


def record_realtime_agent_trigger(reasons: list[str]) -> None:
    with _agent_metrics_lock:
        for reason in reasons:
            _agent_trigger_total[reason] += 1


def record_realtime_agent_patch(patch_type: str, status: str) -> None:
    with _agent_metrics_lock:
        _agent_patch_total[(patch_type, status)] += 1


def record_realtime_agent_guard_reject(reasons: list[str]) -> None:
    with _agent_metrics_lock:
        for reason in reasons:
            _agent_guard_reject_total[reason] += 1


def record_realtime_agent_llm_call(agent: str, model_tier: str) -> None:
    with _agent_metrics_lock:
        _agent_llm_calls_total[(agent, model_tier)] += 1


def record_realtime_agent_first_summary_latency(seconds: float) -> None:
    with _agent_metrics_lock:
        _agent_first_summary_latency_sum["all"] += seconds
        _agent_first_summary_latency_count["all"] += 1


def _format_sample(metric: str, labels: dict[str, str], value: float) -> str:
    serialized_labels = ",".join(f'{key}="{_escape_label(label)}"' for key, label in labels.items())
    return f"{metric}{{{serialized_labels}}} {value}"


def _escape_label(value: str) -> str:
    return value.replace("\\", "\\\\").replace("\n", "\\n").replace('"', '\\"')


def _normalize_path(request: Request) -> str:
    path = request.url.path or "/"
    return path if path.startswith("/") else f"/{path}"
