from __future__ import annotations

import json
from typing import Any, Dict

import httpx

from app.core.config import Settings
from app.llm.base import LLMProvider
from app.llm.template_provider import TemplateLLMProvider


class OpenAICompatibleProvider(LLMProvider):
    """Reserved implementation for DeepSeek/Qwen/OpenAI-compatible chat APIs."""

    def __init__(self, settings: Settings):
        if not settings.llm_base_url or not settings.llm_api_key:
            raise ValueError("llm_base_url and llm_api_key are required")
        if not settings.llm_model and not settings.llm_ark_endpoint_id:
            raise ValueError("llm_model or llm_ark_endpoint_id is required")
        self.settings = settings
        self.fallback = TemplateLLMProvider()

    def generate_lesson_report(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        prompt = self._lesson_report_prompt(payload)
        return self._complete_json(prompt, fallback=self.fallback.generate_lesson_report(payload))

    def generate_chat_summary(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        prompt = self._chat_summary_prompt(payload)
        return self._complete_json(prompt, fallback=self.fallback.generate_chat_summary(payload))

    def _complete_json(self, prompt: str, *, fallback: Dict[str, Any]) -> Dict[str, Any]:
        url = self.settings.llm_base_url.rstrip("/") + "/chat/completions"
        body = {
            "model": self.settings.llm_model or self.settings.llm_ark_endpoint_id,
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "你是 AI Tutor 平台的内部教育助手。"
                        "你只能输出一个合法 JSON 对象，不能输出 Markdown、解释、代码块或额外前后缀。"
                        "不得编造课堂事实、学生情况、预算或风险；无法确认时使用空字符串、空数组或较低 confidence。"
                    ),
                },
                {"role": "user", "content": prompt},
            ],
            "temperature": 0.2,
            "response_format": {"type": "json_object"},
        }
        headers = {"Authorization": f"Bearer {self.settings.llm_api_key}"}
        with httpx.Client(timeout=self.settings.llm_timeout_seconds) as client:
            response = client.post(url, json=body, headers=headers)
            response.raise_for_status()
            data = response.json()
        content = data["choices"][0]["message"]["content"]
        try:
            parsed = json.loads(content)
        except json.JSONDecodeError:
            parsed = fallback
            parsed["llmRawContent"] = content
        parsed["provider"] = "openai-compatible"
        return parsed

    @staticmethod
    def _lesson_report_prompt(payload: Dict[str, Any]) -> str:
        schema = {
            "reportTitle": "string",
            "parentSummary": "string",
            "knowledgePoints": ["string"],
            "studentPerformance": {
                "summary": "string",
                "strengths": ["string"],
                "problems": ["string"],
            },
            "homework": ["string"],
            "nextLessonPlan": "string",
            "teacherSuggestion": "string",
            "confidence": "0-1 float",
            "needTeacherReview": True,
        }
        return (
            "任务：生成家教平台课后报告草稿，读者是家长，风格专业、温和、清晰。\n"
            "业务要求：\n"
            "1. 只能基于输入事实总结，不得补充未出现的成绩、态度、作业量、教学结论。\n"
            "2. `parentSummary` 用 2-4 句概括本节课学了什么、学生表现如何、接下来怎么跟进。\n"
            "3. `knowledgePoints` 只提炼课堂明确涉及的知识点，最多 6 条。\n"
            "4. `studentPerformance.summary` 应贴近老师原始描述；`strengths` 与 `problems` 各 1-3 条，避免空泛鸡汤。\n"
            "5. `homework` 拆成数组；若输入没有明确作业，用保守表达，不要擅自布置新作业。\n"
            "6. `nextLessonPlan` 聚焦下一节课计划；`teacherSuggestion` 聚焦家长如何配合。\n"
            "7. `confidence` 反映信息充分度；信息不全时降低分数并保持 `needTeacherReview=true`。\n"
            "输出 JSON Schema：\n"
            f"{json.dumps(schema, ensure_ascii=False)}\n"
            "输入数据：\n"
            f"{json.dumps(payload, ensure_ascii=False)}"
        )

    @staticmethod
    def _chat_summary_prompt(payload: Dict[str, Any]) -> str:
        schema = {
            "summary": "string",
            "studentProfile": {
                "grade": "string",
                "subject": "string",
                "painPoints": ["string"],
            },
            "parentIntent": {
                "budget": "string",
                "preferredTime": "string",
                "classMode": "string",
                "decisionStage": "string",
            },
            "riskSignals": ["string"],
            "nextActions": ["string"],
            "confidence": "0-1 float",
            "needHumanReview": True,
        }
        return (
            "任务：生成家教平台 IM 沟通摘要，给后续跟进老师或顾问使用。\n"
            "业务要求：\n"
            "1. 重点提炼学生画像、家长诉求、排课偏好、预算线索、成交阶段、风险点和下一步动作。\n"
            "2. 绝不编造预算、联系方式、试听意愿、成绩水平；无法确认就留空或不写。\n"
            "3. `summary` 用 3-5 句说明当前沟通进展。\n"
            "4. `studentProfile.painPoints` 只保留聊天中明确提到的薄弱项或目标。\n"
            "5. `riskSignals` 仅在聊天中出现平台外联系方式、强烈压价、明显流失、投诉倾向等信号时填写。\n"
            "6. `nextActions` 输出 2-4 条可执行建议，便于销售/老师继续跟进。\n"
            "7. `confidence` 反映信息完备度；信息不足时降低分数并保持 `needHumanReview=true`。\n"
            "输出 JSON Schema：\n"
            f"{json.dumps(schema, ensure_ascii=False)}\n"
            "输入数据：\n"
            f"{json.dumps(payload, ensure_ascii=False)}"
        )
