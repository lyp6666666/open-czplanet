from __future__ import annotations

from typing import Any


def assert_non_empty_str(v: Any) -> str:
    if not isinstance(v, str) or not v.strip():
        raise AssertionError("expected non-empty string")
    return v


def assert_int(v: Any) -> int:
    if isinstance(v, bool) or not isinstance(v, int):
        raise AssertionError("expected int")
    return v


def assert_dict(v: Any) -> dict[str, Any]:
    if not isinstance(v, dict):
        raise AssertionError("expected dict")
    return v


def assert_has_keys(obj: Any, keys: list[str]) -> dict[str, Any]:
    d = assert_dict(obj)
    for k in keys:
        if k not in d:
            raise AssertionError(f"missing key: {k}")
    return d
