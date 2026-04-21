from __future__ import annotations

from typing import Any, Dict, List, Optional

from app.llm.base import LLMProvider


def _split_lines(value: Optional[str]) -> List[str]:
    if not value:
        return []
    return [line.strip(" -\t") for line in value.splitlines() if line.strip()]


class TemplateLLMProvider(LLMProvider):
    """Deterministic placeholder provider used before real vendor credentials exist."""

    def generate_lesson_report(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        topic = payload.get("lessonTopic") or "本节课程"
        subject = payload.get("subject") or "学科"
        grade = payload.get("grade") or "学生"
        notes = payload.get("teacherNotes") or ""
        performance = payload.get("studentPerformance") or "老师暂未填写详细课堂表现。"
        homework_items = _split_lines(payload.get("homework")) or ["请按老师要求完成课后巩固。"]
        next_plan = payload.get("nextPlan") or "下节课将结合本节掌握情况继续推进。"

        note_preview = notes[:180] + ("..." if len(notes) > 180 else "")
        return {
            "reportTitle": f"{grade}{subject}课后反馈",
            "parentSummary": (
                f"本节课围绕“{topic}”展开。根据老师记录：{note_preview}"
                if note_preview
                else f"本节课围绕“{topic}”展开，老师已完成课后反馈草稿。"
            ),
            "knowledgePoints": self._infer_knowledge_points(topic, notes),
            "studentPerformance": {
                "summary": performance,
                "strengths": ["能够配合老师完成本节课主要学习任务。"],
                "problems": ["具体薄弱点建议由老师确认后补充。"],
            },
            "homework": homework_items,
            "nextLessonPlan": next_plan,
            "teacherSuggestion": "建议家长关注孩子课后巩固完成情况，并鼓励孩子及时整理错题和疑问。",
            "confidence": 0.35,
            "needTeacherReview": True,
            "provider": "template",
        }

    def generate_chat_summary(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        messages = payload.get("messages") or []
        combined = "\n".join(
            f"{m.get('senderRole') or m.get('senderName') or '用户'}：{m.get('content', '')}"
            for m in messages
        )
        preview = combined[:220] + ("..." if len(combined) > 220 else "")
        return {
            "summary": preview or "暂无足够聊天内容生成摘要。",
            "studentProfile": {
                "grade": self._find_keyword(combined, ["小学", "初中", "高中", "一年级", "二年级", "初一", "初二", "初三"]),
                "subject": self._find_keyword(combined, ["数学", "英语", "语文", "物理", "化学", "编程"]),
                "painPoints": [],
            },
            "parentIntent": {
                "budget": self._extract_budget_hint(combined),
                "preferredTime": self._find_keyword(combined, ["周末", "晚上", "暑假", "寒假", "放学后"]),
                "classMode": self._find_keyword(combined, ["线上", "线下", "上门"]),
                "decisionStage": "待老师进一步确认",
            },
            "riskSignals": self._risk_signals(combined),
            "nextActions": [
                "建议老师确认学生年级、当前成绩和目标分数。",
                "建议尽快确认首次试听或正式上课时间。",
            ],
            "confidence": 0.3,
            "needHumanReview": True,
            "provider": "template",
        }

    @staticmethod
    def _infer_knowledge_points(topic: str, notes: str) -> List[str]:
        points = [part.strip() for part in topic.replace("，", ",").replace("、", ",").split(",")]
        points = [p for p in points if p]
        if points:
            return points[:6]
        return [notes[:30]] if notes else []

    @staticmethod
    def _find_keyword(text: str, keywords: List[str]) -> Optional[str]:
        return next((kw for kw in keywords if kw in text), None)

    @staticmethod
    def _extract_budget_hint(text: str) -> Optional[str]:
        for marker in ["元", "/小时", "每小时", "预算"]:
            if marker in text:
                return "聊天中提到预算信息，建议老师进一步确认。"
        return None

    @staticmethod
    def _risk_signals(text: str) -> List[str]:
        signals = []
        for marker in ["微信", "电话", "手机号", "私下", "加我"]:
            if marker in text:
                signals.append("聊天中疑似出现平台外联系方式或私下沟通意图。")
                break
        return signals
