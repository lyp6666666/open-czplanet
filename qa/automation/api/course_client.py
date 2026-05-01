from __future__ import annotations

from typing import Any

from core.http_client import ApiClient


class CourseClient:
    def __init__(self, client: ApiClient):
        self.client = client

    def my_courses(self, *, role: str | None = None, page: int | None = None, size: int | None = None) -> list[dict[str, Any]]:
        params: dict[str, Any] = {}
        if role:
          params["role"] = role
        if page is not None:
          params["page"] = int(page)
        if size is not None:
          params["size"] = int(size)
        data = self.client.get_data("/courses/my", params=params or None)
        if not isinstance(data, list):
            raise RuntimeError("my_courses_invalid")
        return [item for item in data if isinstance(item, dict)]

    def detail(self, course_id: int) -> dict[str, Any]:
        data = self.client.get_data(f"/courses/{int(course_id)}")
        if not isinstance(data, dict):
            raise RuntimeError("course_detail_invalid")
        return data

    def submit_trial_result(
        self,
        course_id: int,
        *,
        result: str,
        reason: str | None = None,
        evidence_image_urls: list[str] | None = None,
        evidence_video_url: str | None = None,
        evidence_video_duration_seconds: int | None = None,
    ) -> Any:
        payload: dict[str, Any] = {"result": result}
        if reason:
            payload["reason"] = reason
        if evidence_image_urls:
            payload["evidenceImageUrls"] = evidence_image_urls
        if evidence_video_url:
            payload["evidenceVideoUrl"] = evidence_video_url
        if evidence_video_duration_seconds is not None:
            payload["evidenceVideoDurationSeconds"] = int(evidence_video_duration_seconds)
        return self.client.post_data(f"/courses/{int(course_id)}/trial-result", json=payload)

    def apply_trial_refund(
        self,
        course_id: int,
        *,
        reason: str,
        evidence_image_urls: list[str] | None = None,
        evidence_video_url: str | None = None,
        evidence_video_duration_seconds: int | None = None,
    ) -> Any:
        payload: dict[str, Any] = {"reason": reason}
        if evidence_image_urls:
            payload["evidenceImageUrls"] = evidence_image_urls
        if evidence_video_url:
            payload["evidenceVideoUrl"] = evidence_video_url
        if evidence_video_duration_seconds is not None:
            payload["evidenceVideoDurationSeconds"] = int(evidence_video_duration_seconds)
        return self.client.post_data(f"/courses/{int(course_id)}/trial-refund/apply", json=payload)
