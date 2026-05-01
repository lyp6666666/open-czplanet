from __future__ import annotations

from typing import Any

from core.http_client import ApiClient


class ScheduleClient:
    def __init__(self, client: ApiClient):
        self.client = client

    def list_course_events(self, course_id: int) -> list[dict[str, Any]]:
        data = self.client.get_data(f"/api/v1/schedule/courses/{int(course_id)}/events")
        if not isinstance(data, list):
            raise RuntimeError("list_course_events_invalid")
        return [item for item in data if isinstance(item, dict)]

    def list_events(self, *, start_at: int, end_at: int, include_pending: bool | None = None) -> list[dict[str, Any]]:
        params: dict[str, Any] = {"startAt": int(start_at), "endAt": int(end_at)}
        if include_pending is not None:
            params["includePending"] = bool(include_pending)
        data = self.client.get_data("/api/v1/schedule/events", params=params)
        if not isinstance(data, list):
            raise RuntimeError("list_events_invalid")
        return [item for item in data if isinstance(item, dict)]

    def create_event(
        self,
        *,
        course_id: int | None,
        lesson_type: str | None,
        title: str,
        participant_user_id: int,
        start_at: int,
        end_at: int,
        lesson_price_fen: int | None = None,
        trial_price_percent: int | None = None,
        description: str | None = None,
        subject_id: int | None = None,
    ) -> dict[str, Any]:
        payload: dict[str, Any] = {
            "title": title,
            "participantUserId": int(participant_user_id),
            "startAt": int(start_at),
            "endAt": int(end_at),
        }
        if course_id is not None:
            payload["courseId"] = int(course_id)
        if lesson_type:
            payload["lessonType"] = lesson_type
        if lesson_price_fen is not None:
            payload["lessonPriceFen"] = int(lesson_price_fen)
        if trial_price_percent is not None:
            payload["trialPricePercent"] = int(trial_price_percent)
        if description:
            payload["description"] = description
        if subject_id is not None:
            payload["subjectId"] = int(subject_id)
        data = self.client.post_data("/api/v1/schedule/events", json=payload)
        if not isinstance(data, dict):
            raise RuntimeError("create_event_invalid")
        return data

    def respond(self, event_id: int, action: str) -> dict[str, Any]:
        data = self.client.post_data(f"/api/v1/schedule/events/{int(event_id)}/response", json={"action": action})
        if not isinstance(data, dict):
            raise RuntimeError("respond_event_invalid")
        return data

    def cancel(self, event_id: int, remark: str | None = None) -> dict[str, Any]:
        payload = {"remark": remark} if remark else {}
        data = self.client.post_data(f"/api/v1/schedule/events/{int(event_id)}/cancel", json=payload)
        if not isinstance(data, dict):
            raise RuntimeError("cancel_event_invalid")
        return data

    def submit_weekly_schedule(
        self,
        course_id: int,
        *,
        participant_user_id: int,
        slots: list[dict[str, int]],
        room_id: int | None = None,
        title: str | None = None,
        description: str | None = None,
        lesson_price_fen: int | None = None,
        weeks: int | None = None,
    ) -> list[dict[str, Any]]:
        payload: dict[str, Any] = {
            "participantUserId": int(participant_user_id),
            "slots": slots,
        }
        if room_id is not None:
            payload["roomId"] = int(room_id)
        if title:
            payload["title"] = title
        if description:
            payload["description"] = description
        if lesson_price_fen is not None:
            payload["lessonPriceFen"] = int(lesson_price_fen)
        if weeks is not None:
            payload["weeks"] = int(weeks)
        data = self.client.post_data(f"/api/v1/schedule/courses/{int(course_id)}/weekly-schedule", json=payload)
        if not isinstance(data, list):
            raise RuntimeError("submit_weekly_schedule_invalid")
        return [item for item in data if isinstance(item, dict)]
