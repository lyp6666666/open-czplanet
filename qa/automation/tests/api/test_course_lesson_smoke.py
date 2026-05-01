from __future__ import annotations

import os
from datetime import datetime, timedelta, timezone

import pytest

from api.appointment_client import AppointmentClient
from api.course_client import CourseClient
from api.live_client import LiveClient
from api.schedule_client import ScheduleClient
from core.http_client import ApiClient, ApiError


def _env_int(name: str, default: int = 0) -> int:
    return int(os.getenv(name, str(default)))


def _need_course_seed() -> tuple[int, int, int]:
    course_id = _env_int("QA_COURSE_SMOKE_COURSE_ID", 982001)
    teacher_uid = _env_int("QA_COURSE_SMOKE_TEACHER_USER_ID", 910103)
    student_uid = _env_int("QA_COURSE_SMOKE_STUDENT_USER_ID", 910003)
    if course_id <= 0:
        pytest.skip("QA_COURSE_SMOKE_COURSE_ID is not set")
    return course_id, teacher_uid, student_uid


def _authed_clients(qa_config, token: str) -> tuple[ApiClient, CourseClient, ScheduleClient, AppointmentClient, LiveClient]:
    api = ApiClient(qa_config.api_base_url, timeout_s=10)
    api.set_bearer_token(token)
    return api, CourseClient(api), ScheduleClient(api), AppointmentClient(api), LiveClient(api)


def _status_upper(value: object) -> str:
    return str(value or "").strip().upper()


def _need_env_event(name: str) -> int:
    value = _env_int(name)
    if value <= 0:
        pytest.skip(f"{name} is not set")
    return value


def _need_course_context(course_client: CourseClient, course_id: int) -> dict:
    try:
        detail = course_client.detail(course_id)
    except ApiError as exc:
        if exc.code == 40400:
            pytest.skip(
                f"course seed {course_id} unavailable in current QA env: {exc.message}"
            )
        raise
    if not isinstance(detail, dict) or int(detail.get("courseId", 0)) != course_id:
        pytest.skip(f"course seed {course_id} invalid in current QA env")
    return detail


def _need_existing_event(
    schedule_client: ScheduleClient,
    *,
    course_id: int,
    event_id: int,
    label: str,
) -> dict:
    events = schedule_client.list_course_events(course_id)
    matched = next((item for item in events if int(item.get("id", 0)) == event_id), None)
    if not matched:
        pytest.skip(f"{label} seed {event_id} unavailable in current QA env")
    return matched


@pytest.mark.api
@pytest.mark.smoke
def test_course_detail_and_events_load(qa_config, course_teacher_token: str):
    course_id, _, _ = _need_course_seed()
    _, course_client, schedule_client, _, _ = _authed_clients(qa_config, course_teacher_token)

    detail = _need_course_context(course_client, course_id)
    assert int(detail.get("courseId", 0)) == course_id
    assert str(detail.get("status", "")).strip()

    events = schedule_client.list_course_events(course_id)
    assert isinstance(events, list)


@pytest.mark.api
@pytest.mark.regression
def test_create_lesson_and_cancel_it(qa_config, course_teacher_token: str):
    course_id, _, student_uid = _need_course_seed()
    _, course_client, schedule_client, _, _ = _authed_clients(qa_config, course_teacher_token)

    detail = _need_course_context(course_client, course_id)
    title = f"QA 小程序课节 {datetime.now(tz=timezone.utc).strftime('%H%M%S')}"
    start = datetime.now(tz=timezone.utc) + timedelta(days=2)
    end = start + timedelta(hours=1)

    created = schedule_client.create_event(
        course_id=course_id,
        lesson_type="NORMAL",
        title=title,
        participant_user_id=int(detail.get("studentUid") or student_uid),
        start_at=int(start.timestamp() * 1000),
        end_at=int(end.timestamp() * 1000),
        description="QA automation created lesson",
    )
    event_id = int(created.get("id", 0))
    assert event_id > 0

    canceled = schedule_client.cancel(event_id, remark="qa automation cleanup")
    assert int(canceled.get("id", 0)) == event_id
    assert str(canceled.get("status", "")).upper() in {"CANCELED", "CANCELLED"}


@pytest.mark.api
@pytest.mark.regression
def test_submit_weekly_schedule_returns_events(qa_config, course_student_token: str):
    course_id, teacher_uid, _ = _need_course_seed()
    _, course_client, schedule_client, _, _ = _authed_clients(qa_config, course_student_token)

    detail = _need_course_context(course_client, course_id)
    result = schedule_client.submit_weekly_schedule(
        course_id,
        participant_user_id=int(detail.get("teacherUid") or teacher_uid),
        room_id=detail.get("roomId"),
        title="QA 正式课表",
        description="QA automation weekly schedule",
        weeks=2,
        slots=[{"dayOfWeek": 6, "startMinute": 19 * 60, "endMinute": 20 * 60}],
    )
    assert isinstance(result, list)


@pytest.mark.api
@pytest.mark.regression
def test_live_prepare_or_session_loads_when_online_course(qa_config, course_teacher_token: str):
    course_id, _, _ = _need_course_seed()
    _, course_client, _, _, live_client = _authed_clients(qa_config, course_teacher_token)

    detail = _need_course_context(course_client, course_id)
    teaching_mode = str(detail.get("teachingMode") or "").upper()
    if teaching_mode != "ONLINE":
        pytest.skip("course is not ONLINE")

    live = live_client.by_course(course_id)
    assert int(live.get("courseId", 0)) == course_id

    prepare = live_client.prepare(course_id, client_type="MP_WEIXIN", source_page="QA_AUTOMATION")
    assert int(prepare.get("sessionId", 0)) > 0
    assert "canJoin" in prepare


@pytest.mark.api
@pytest.mark.regression
def test_student_can_accept_teacher_created_event(qa_config, course_teacher_token: str, course_student_token: str):
    course_id, _, student_uid = _need_course_seed()
    _, course_client, teacher_schedule, _, _ = _authed_clients(qa_config, course_teacher_token)
    _, _, student_schedule, _, _ = _authed_clients(qa_config, course_student_token)

    detail = _need_course_context(course_client, course_id)
    title = f"QA 待确认课节 {datetime.now(tz=timezone.utc).strftime('%H%M%S')}"
    start = datetime.now(tz=timezone.utc) + timedelta(days=3)
    end = start + timedelta(hours=1)
    created = teacher_schedule.create_event(
        course_id=course_id,
        lesson_type="NORMAL",
        title=title,
        participant_user_id=int(detail.get("studentUid") or student_uid),
        start_at=int(start.timestamp() * 1000),
        end_at=int(end.timestamp() * 1000),
        description="QA automation pending lesson",
    )
    event_id = int(created.get("id", 0))
    assert event_id > 0

    accepted = student_schedule.respond(event_id, "ACCEPT")
    assert int(accepted.get("id", 0)) == event_id
    assert _status_upper(accepted.get("status")) in {"ACCEPTED", "CONFIRMED"}


@pytest.mark.api
@pytest.mark.regression
def test_teacher_reschedule_and_student_confirm(qa_config, course_teacher_token: str, course_student_token: str):
    course_id, _, _ = _need_course_seed()
    event_id = _need_env_event("QA_COURSE_SMOKE_ACCEPTED_EVENT_ID")
    _, course_client, teacher_schedule, teacher_appointment, _ = _authed_clients(qa_config, course_teacher_token)
    _, _, student_schedule, student_appointment, _ = _authed_clients(qa_config, course_student_token)
    _need_course_context(course_client, course_id)
    _need_existing_event(
        teacher_schedule,
        course_id=course_id,
        event_id=event_id,
        label="accepted event",
    )

    proposed_start = datetime.now(tz=timezone.utc) + timedelta(days=4)
    proposed_start = proposed_start.replace(hour=12, minute=0, second=0, microsecond=0)
    teacher_appointment.reschedule(
        event_id,
        proposed_start_time=proposed_start.isoformat().replace("+00:00", "Z"),
        duration_minutes=60,
        remark="QA automation reschedule",
    )

    confirmed = student_appointment.confirm_reschedule(event_id)
    if isinstance(confirmed, dict) and confirmed.get("id") is not None:
        assert int(confirmed.get("id", 0)) == event_id
    else:
        events = student_schedule.list_course_events(course_id)
        matched = next((item for item in events if int(item.get("id", 0)) == event_id), None)
        assert matched is not None
        assert _status_upper(matched.get("status")) in {"ACCEPTED", "CONFIRMED"}


@pytest.mark.api
@pytest.mark.regression
def test_completed_lesson_has_ai_result_when_session_seed_exists(qa_config, course_teacher_token: str):
    course_id, _, _ = _need_course_seed()
    session_id = _env_int("QA_COURSE_SMOKE_LIVE_SESSION_ID", 984002)
    completed_event_id = _env_int("QA_COURSE_SMOKE_COMPLETED_EVENT_ID", 983002)
    _, course_client, schedule_client, _, live_client = _authed_clients(qa_config, course_teacher_token)
    _need_course_context(course_client, course_id)

    _need_existing_event(
        schedule_client,
        course_id=course_id,
        event_id=completed_event_id,
        label="completed lesson",
    )

    if session_id <= 0:
        live = live_client.by_course(course_id)
        session_id = int(live.get("sessionId", 0))
    if session_id <= 0:
        pytest.skip("live session id unavailable")

    result = live_client.ai_result(session_id)
    assert int(result.get("sessionId", 0)) == session_id
    assert _status_upper(result.get("resultStatus")) in {"READY", "FAILED", "PENDING", "GENERATING"}
