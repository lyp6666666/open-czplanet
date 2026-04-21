from __future__ import annotations

import json
from typing import Dict, List

from app.core.config import get_settings
from app.llm.template_provider import TemplateLLMProvider


def _volcark_base_url() -> str:
    return "https://ark.cn-beijing.volces.com/api/v3"


class RealtimeLessonLLM:
    def __init__(self):
        self.settings = get_settings()
        self.template = TemplateLLMProvider()

    def summarize_stage(self, lesson_id: int, state: Dict, segments: List[Dict]) -> Dict:
        if self.settings.llm_provider in {"template", "mock", ""}:
            return self._template_summary(state, segments)
        return self._langchain_summary(lesson_id, state, segments)

    def _template_summary(self, state: Dict, segments: List[Dict]) -> Dict:
        text = "\n".join(seg.get("text", "") for seg in segments[-12:])
        topic = state.get("currentTopic") or "课堂讲解"
        return {
            "currentTopic": topic,
            "stageSummary": text[:160] + ("..." if len(text) > 160 else ""),
            "studentQuestions": state.get("studentQuestions", [])[-5:],
            "homeworkCandidates": state.get("homeworkCandidates", [])[-5:],
            "keyPoints": state.get("keyPoints", [])[-5:],
            "provider": "template",
        }

    def _langchain_summary(self, lesson_id: int, state: Dict, segments: List[Dict]) -> Dict:
        from langchain_openai import ChatOpenAI

        model = self.settings.llm_model or self.settings.llm_ark_endpoint_id
        base_url = self.settings.llm_base_url or _volcark_base_url()
        if not self.settings.llm_api_key:
            raise ValueError("AI_AGENT_LLM_API_KEY is required for volcark/openai-compatible LLM")
        llm = ChatOpenAI(
            model=model,
            api_key=self.settings.llm_api_key,
            base_url=base_url,
            timeout=self.settings.llm_timeout_seconds,
            temperature=0.2,
        )
        transcript = "\n".join(
            f"{seg.get('speaker', 'unknown')}：{seg.get('text', '')}" for seg in segments[-80:]
        )
        prompt = f"""
你是在线一对一课堂的实时 AI 课堂纪要助手。
你的任务是基于“最近一个阶段”的课堂转写，产出供前端右侧实时摘要面板展示的 JSON。
不要输出 Markdown，不要输出任何解释，只能输出一个 JSON 对象。

业务规则：
1. 只总结最近阶段，不要假装知道整节课的完整结论。
2. 以老师讲解内容、学生提问、作业布置、关键提醒为核心。
3. 对学生问题、作业候选、课堂重点必须保守提取；不确定就不要写。
4. `stageSummary` 适合实时展示，控制在 2-4 句，口吻简洁、清楚、偏课堂播报风格。
5. `currentTopic` 尽量提炼为当前讲解主题，不超过 18 个字。
6. `studentQuestions` 只保留学生明确提出的问题，最多 3 条。
7. `homeworkCandidates` 只保留老师明确布置或明显暗示的课后任务，最多 3 条。
8. `keyPoints` 只保留本阶段明确出现的知识点/解题提醒/课堂重点，最多 4 条。

输出 JSON Schema：
{{
  "currentTopic": "string",
  "stageSummary": "string",
  "studentQuestions": ["string"],
  "homeworkCandidates": ["string"],
  "keyPoints": ["string"]
}}

lessonId: {lesson_id}
当前状态: {json.dumps(state, ensure_ascii=False)}
最近转写:
{transcript}
"""
        response = llm.invoke(prompt)
        content = getattr(response, "content", str(response))
        try:
            parsed = json.loads(content)
        except json.JSONDecodeError:
            parsed = {"stageSummary": content}
        parsed["provider"] = self.settings.llm_provider
        return parsed
