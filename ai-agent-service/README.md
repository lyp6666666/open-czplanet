# ai-agent-service

`ai-agent-service` is the first internal Python AI microservice for the AI tutor platform. It is designed to be called by Java services rather than exposed directly to end users. The initial scope is intentionally narrow:

- Generate post-lesson report drafts from structured teacher notes.
- Generate IM chat summaries from message history.
- Run realtime classroom AI P1/P2 flows: session state, transcript ingestion, stage summaries and finalize.
- Run tasks through Redis/RQ when enabled, while still supporting inline execution for local development.
- Keep the LLM provider pluggable. The default `template` provider is deterministic and requires no external API key.
- Reserve clean extension points for LangChain / LangGraph based multi-agent workflows in later iterations.

## Quick Start

```bash
cd ai-agent-service
python -m venv .venv
. .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --host 0.0.0.0 --port 18086
```

The default setup uses Redis/RQ as the task queue. Run the worker when `AI_AGENT_USE_ASYNC_WORKER=true`:

```bash
python -m app.worker
```

For quick local API tests without Redis, set:

```bash
export AI_AGENT_USE_ASYNC_WORKER=false
```

## Main APIs

- `GET /health`
- `POST /internal/ai/lessons/{lesson_id}/report-tasks`
- `GET /internal/ai/lessons/{lesson_id}/report`
- `POST /internal/ai/chat-rooms/{room_id}/summary-tasks`
- `GET /internal/ai/chat-rooms/{room_id}/summary`
- `GET /internal/ai/tasks/{task_id}`
- `POST /internal/ai/live-lessons/{lesson_id}/sessions`
- `POST /internal/ai/live-lessons/{lesson_id}/transcript-segments`
- `GET /internal/ai/live-lessons/{lesson_id}/state`
- `POST /internal/ai/live-lessons/{lesson_id}/finalize`

All endpoints are internal-facing and should be called by platform services, typically through the gateway. They are not intended to be bound directly to public frontend traffic.

## LLM Provider

First release defaults to:

```text
AI_AGENT_LLM_PROVIDER=template
```

This keeps the service runnable without vendor credentials. Later, switch to an OpenAI-compatible provider by filling the reserved config keys.
