from __future__ import annotations

import os
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import Any, Literal

import jwt

from core.http_client import ApiClient


@dataclass(frozen=True)
class SeedUser:
    user_id: int
    phone: str
    role_enum: Literal["TEACHER", "STUDENT"]
    role_code: Literal["teacher", "student"]


SEED_TEACHER = SeedUser(user_id=206, phone="13812345006", role_enum="TEACHER", role_code="teacher")
SEED_STUDENT = SeedUser(user_id=113, phone="15268836913", role_enum="STUDENT", role_code="student")


def seed_user_from_env(prefix: str, default: SeedUser) -> SeedUser:
    user_id = int(os.getenv(f"{prefix}_USER_ID", str(default.user_id)))
    phone = os.getenv(f"{prefix}_PHONE", default.phone)
    role_enum = os.getenv(f"{prefix}_ROLE_ENUM", default.role_enum).upper()
    role_code = os.getenv(f"{prefix}_ROLE_CODE", default.role_code).lower()
    if role_enum not in {"TEACHER", "STUDENT"}:
        raise RuntimeError(f"{prefix}_ROLE_ENUM must be TEACHER or STUDENT")
    if role_code not in {"teacher", "student"}:
        raise RuntimeError(f"{prefix}_ROLE_CODE must be teacher or student")
    return SeedUser(user_id=user_id, phone=phone, role_enum=role_enum, role_code=role_code)


def mint_jwt(*, phone: str, user_id: int, role_code: str, secret: str, issuer: str, ttl_hours: int = 24) -> str:
    now = datetime.now(tz=timezone.utc)
    payload = {
        "userId": int(user_id),
        "role": role_code,
        "sub": phone,
        "iss": issuer,
        "iat": int(now.timestamp()),
        "exp": int((now + timedelta(hours=ttl_hours)).timestamp()),
    }
    return jwt.encode(payload, secret, algorithm="HS256")


def send_sms_code(client: ApiClient, phone: str) -> None:
    client.post_data("/user/sendcode", json={"phone": phone})


def peek_sms_code(client: ApiClient, phone: str) -> str:
    path = os.getenv("QA_SMS_PEEK_PATH", "/internal/debug/sms-code")
    prefix = os.getenv("QA_SMS_PEEK_PREFIX", "sms:code:")
    data = client.get_data(path, params={"phone": phone, "prefix": prefix})
    if not isinstance(data, str) or not data.strip():
        raise RuntimeError("sms_code_unavailable")
    return data.strip()


def login_with_code(client: ApiClient, phone: str, code: str, role_enum: str) -> dict[str, Any]:
    data = client.post_data("/user/loginOrRegister", json={"phone": phone, "code": code, "userRoleEnum": role_enum})
    if not isinstance(data, dict):
        raise RuntimeError("login_response_invalid")
    return data


def login_or_mint_seed_user(client: ApiClient, seed: SeedUser, *, jwt_secret: str | None, jwt_issuer: str) -> str:
    mode = os.getenv("QA_LOGIN_MODE", "otp").strip().lower()

    if mode == "jwt":
        if not jwt_secret:
            raise RuntimeError("QA_JWT_SECRET_REQUIRED")
        return mint_jwt(
            phone=seed.phone,
            user_id=seed.user_id,
            role_code=seed.role_code,
            secret=jwt_secret,
            issuer=jwt_issuer,
        )

    send_sms_code(client, seed.phone)
    code = peek_sms_code(client, seed.phone)
    login = login_with_code(client, seed.phone, code, seed.role_enum)
    token = login.get("token")
    if not isinstance(token, str) or not token.strip():
        raise RuntimeError("token_missing")
    return token
