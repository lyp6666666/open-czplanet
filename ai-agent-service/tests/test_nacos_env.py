from __future__ import annotations

from scripts import export_nacos_env


class _FakeResponse:
    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, traceback):
        return False

    def read(self) -> bytes:
        return b"""
ai-agent:
  tencent-asr:
    enabled: true
    app-id: "123"
    secret-id: "sid"
    secret-key: "skey"
    engine-model-type: "16k_zh"
    voice-format: 1
    need-vad: 1
    speech-sdk-path: "/tmp/local-only-sdk"
"""


def test_export_nacos_env_does_not_export_local_speech_sdk_path(monkeypatch, capsys):
    monkeypatch.setattr(export_nacos_env.urllib.request, "urlopen", lambda *args, **kwargs: _FakeResponse())

    exit_code = export_nacos_env.main()

    assert exit_code == 0
    output = capsys.readouterr().out
    assert "AI_AGENT_TENCENT_ASR_ENABLED='true'" in output
    assert "AI_AGENT_TENCENT_SPEECH_SDK_PATH" not in output
    assert "/tmp/local-only-sdk" not in output
