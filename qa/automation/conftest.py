from __future__ import annotations

import os
from typing import Iterator

import pytest

from core.auth import SEED_STUDENT, SEED_TEACHER, login_or_mint_seed_user
from core.config import QAConfig, load_config
from core.http_client import ApiClient


@pytest.fixture(scope="session")
def qa_config() -> QAConfig:
    return load_config()


@pytest.fixture(scope="session")
def api_client(qa_config: QAConfig) -> ApiClient:
    timeout_s = float(os.getenv("QA_API_TIMEOUT_S", "10"))
    return ApiClient(qa_config.api_base_url, timeout_s=timeout_s)


@pytest.fixture(scope="session")
def teacher_token(api_client: ApiClient, qa_config: QAConfig) -> str:
    return login_or_mint_seed_user(api_client, SEED_TEACHER, jwt_secret=qa_config.jwt_secret, jwt_issuer=qa_config.jwt_issuer)


@pytest.fixture(scope="session")
def student_token(api_client: ApiClient, qa_config: QAConfig) -> str:
    return login_or_mint_seed_user(api_client, SEED_STUDENT, jwt_secret=qa_config.jwt_secret, jwt_issuer=qa_config.jwt_issuer)


@pytest.fixture
def authed_client(api_client: ApiClient, teacher_token: str) -> Iterator[ApiClient]:
    api_client.set_bearer_token(teacher_token)
    yield api_client
    api_client.set_bearer_token(None)
