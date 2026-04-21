from __future__ import annotations

from app.asr.tencent_provider import TencentRealtimeASRProvider


def get_realtime_asr_provider():
    return TencentRealtimeASRProvider()
