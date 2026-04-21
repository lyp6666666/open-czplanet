from __future__ import annotations

from typing import Dict, List


QUESTION_MARKERS = ["为什么", "怎么", "不会", "不懂", "可以再讲", "什么意思", "吗", "？", "?"]
HOMEWORK_MARKERS = ["作业", "回去", "课后", "练习", "讲义", "完成", "整理错题"]
KEY_POINT_MARKERS = ["重点", "注意", "记住", "核心", "关键", "一定要"]
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


def extract_realtime_signals(text: str) -> Dict[str, List[str]]:
    questions = [text] if any(marker in text for marker in QUESTION_MARKERS) else []
    homework = [text] if any(marker in text for marker in HOMEWORK_MARKERS) else []
    key_points = [text] if any(marker in text for marker in KEY_POINT_MARKERS) else []
    topics = [topic for topic in SUBJECT_TOPICS if topic in text]
    return {
        "studentQuestions": questions[:3],
        "homeworkCandidates": homework[:3],
        "keyPoints": key_points[:3],
        "topics": topics[:3],
    }
