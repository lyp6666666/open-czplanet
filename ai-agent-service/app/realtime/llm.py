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
        segment_count = int(state.get("segmentCount") or 0)
        existing_outline = list(state.get("minutesOutline") or [])
        section = {
            "id": f"section-{len(existing_outline) + 1}",
            "title": topic,
            "summary": text[:180] + ("..." if len(text) > 180 else ""),
            "startSegment": max(1, segment_count - min(len(segments), 12) + 1),
            "endSegment": segment_count,
            "items": [
                {
                    "title": "本段课堂重点",
                    "detail": text[:180] + ("..." if len(text) > 180 else ""),
                }
            ] if text.strip() else [],
        }
        return {
            "currentTopic": topic,
            "stageSummary": text[:160] + ("..." if len(text) > 160 else ""),
            "minutesOutline": [*existing_outline, section][-12:],
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
        existing_outline = json.dumps(state.get("minutesOutline") or [], ensure_ascii=False)
        prompt = f"""
你是在线一对一课堂的实时 AI 课堂纪要编排助手。
你的任务是基于“最近一个阶段”的课堂转写，维护供前端右侧实时纪要面板展示的结构化 JSON。
不要输出 Markdown，不要输出任何解释，只能输出一个 JSON 对象。

业务规则：
1. 面向老师/学生展示，禁止出现 ASR、LLM、转写、模型、置信度等技术词。
2. `minutesOutline` 是课堂实时纪要树：大标题是课堂阶段/主题，小标题是该阶段下的关键内容。
3. 如果最近转写仍在讲同一主题，请更新最后一个大标题下的 `summary` 和 `items`；如果明显进入新主题（如从作业讲评切到新知识点、从概念切到例题、从讲解切到答疑），新增一个大标题。
4. 大标题必须由 AI 根据课堂内容生成，最多 16 个汉字，像“作业错题回顾”“一次函数概念引入”“例题建模步骤”。
5. 每个大标题下 `items` 最多 5 个；小标题最多 14 个汉字，说明 `detail` 1-2 句，保留教学过程和结论，不要堆关键词。
6. `stageSummary` 是最近阶段一句话摘要，控制在 40-90 个汉字。
7. 对学生问题、作业候选、课堂重点必须保守提取；不确定就不要写。
8. 输出只保留最近 8 个大标题；不要删除仍有价值的历史章节。
9. 内容要忠实于转写，不要编造未出现的知识点、作业或结论。

输出 JSON Schema：
{{
  "currentTopic": "string",
  "stageSummary": "string",
  "minutesOutline": [
    {{
      "id": "稳定唯一 id，比如 section-1",
      "title": "大标题",
      "summary": "该大主题的整体概括",
      "startSegment": 1,
      "endSegment": 8,
      "items": [
        {{ "title": "小标题", "detail": "具体说明" }}
      ]
    }}
  ],
  "studentQuestions": ["string"],
  "homeworkCandidates": ["string"],
  "keyPoints": ["string"]
}}

lessonId: {lesson_id}
当前状态: {json.dumps(state, ensure_ascii=False)}
已有实时纪要树: {existing_outline}
最近转写:
{transcript}
"""
        response = llm.invoke(prompt)
        content = getattr(response, "content", str(response))
        try:
            parsed = json.loads(content)
        except json.JSONDecodeError:
            parsed = {"stageSummary": content}
        parsed.setdefault("minutesOutline", state.get("minutesOutline") or [])
        parsed["provider"] = self.settings.llm_provider
        return parsed
