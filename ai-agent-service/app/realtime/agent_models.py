from __future__ import annotations

import time
from typing import Any, Dict, Iterable, List


TECHNICAL_TERMS = ["ASR", "LLM", "模型", "置信度", "转写"]
QUESTION_MARKERS = ["为什么", "怎么", "不会", "不懂", "可以再讲", "什么意思", "吗", "？", "?"]
HOMEWORK_MARKERS = ["作业", "回去", "课后", "练习", "讲义", "完成", "整理错题"]
KEY_POINT_MARKERS = ["重点", "注意", "记住", "核心", "关键", "一定要"]
TRANSITION_MARKERS = ["接下来", "下面", "然后我们看", "再看", "总结一下", "这题就到这里"]
PRACTICE_MARKERS = ["例题", "练习", "代入", "求出", "计算", "校验"]
CLASSROOM_MANAGEMENT_MARKERS = ["能听到", "摄像头", "麦克风", "稍等", "听得到", "打开"]
SUBJECT_TOPICS = [
    "一次函数",
    "二次函数",
    "几何",
    "方程",
    "阅读理解",
    "作文",
    "语法",
    "单词",
    "物理",
    "化学",
]


def now_ts() -> int:
    return int(time.time())


def clean_text(value: Any, limit: int = 240) -> str:
    return str(value or "").strip()[:limit]


def contains_any(text: str, markers: Iterable[str]) -> bool:
    return any(marker in text for marker in markers)


def detect_topic(text: str, fallback: str | None = None) -> str | None:
    for topic in SUBJECT_TOPICS:
        if topic in text:
            return topic
    return fallback


def unique_texts(items: Iterable[Any], limit: int = 20) -> List[str]:
    result: List[str] = []
    for item in items:
        text = clean_text(item, 260)
        if text and text not in result:
            result.append(text)
    return result[-limit:]


def evidence_event_ids(patch_item: Dict) -> List[str]:
    raw = patch_item.get("evidenceEventIds") or patch_item.get("evidence_event_ids") or []
    if not isinstance(raw, list):
        return []
    return [str(item) for item in raw if str(item or "").strip()]
