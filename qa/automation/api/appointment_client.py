from __future__ import annotations

from typing import Any

from core.http_client import ApiClient


class AppointmentClient:
    def __init__(self, client: ApiClient):
        self.client = client

    def reschedule(
        self,
        appointment_id: int,
        *,
        proposed_start_time: str,
        duration_minutes: int | None = None,
        remark: str | None = None,
    ) -> Any:
        payload: dict[str, Any] = {"proposedStartTime": proposed_start_time}
        if duration_minutes is not None:
            payload["durationMinutes"] = int(duration_minutes)
        if remark:
            payload["remark"] = remark
        return self.client.post_data(f"/appointment/{int(appointment_id)}/reschedule", json=payload)

    def confirm_reschedule(self, appointment_id: int) -> Any:
        return self.client.post_data(f"/appointment/{int(appointment_id)}/confirmReschedule")

    def complete(self, appointment_id: int) -> Any:
        return self.client.post_data(f"/appointment/{int(appointment_id)}/complete")
