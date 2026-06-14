#!/usr/bin/env python3
import hashlib
import importlib.util
import json
import os
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

from yanyun_kb_loader import load_payload


ROOT = Path(__file__).resolve().parents[2]
KB_DIR = ROOT / "knowledge-base/commercial-final"
RETRIEVAL_SCRIPT = ROOT / "scripts/knowledge/run-yanyun-kb-retrieval-ab.py"
REPORT_PATH = ROOT / "docs/knowledge/yanyun-knowledge-real-lyrics-ab-v0.1.md"
RESULT_PATH = ROOT / "knowledge-base/commercial-final-real-lyrics-ab-result.json"
MANUAL_REVIEW_REPORT_PATH = ROOT / "build/reports/knowledge/yanyun-real-lyrics-ab-manual-review.md"
MANUAL_REVIEW_RESULT_PATH = ROOT / "build/reports/knowledge/yanyun-real-lyrics-ab-manual-review.json"
DEFAULT_SAMPLE_IDS = [
    "eval-character-hanxiangxun-001",
    "eval-character-ayinuer-001",
    "eval-story-faction-fall-001",
    "eval-heavy-story-003-jiuliu-common",
]


def load_retrieval_module():
    spec = importlib.util.spec_from_file_location("yanyun_kb_retrieval_ab", RETRIEVAL_SCRIPT)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def sha256(value):
    return hashlib.sha256((value or "").encode("utf-8")).hexdigest()


def first_non_blank(*values):
    for value in values:
        if isinstance(value, str) and value.strip():
            return value.strip()
    return ""


def parse_json_object(content):
    text = (content or "").strip()
    if text.startswith("```"):
        lines = text.splitlines()
        if lines and lines[0].startswith("```"):
            lines = lines[1:]
        if lines and lines[-1].startswith("```"):
            lines = lines[:-1]
        text = "\n".join(lines).strip()
    start = text.find("{")
    end = text.rfind("}")
    if start >= 0 and end > start:
        text = text[start : end + 1]
    return json.loads(text)


def deepseek_chat_json(system_prompt, user_prompt, temperature=0.55, max_tokens=4096):
    base_url = os.environ.get("DEEPSEEK_BASE_URL", "https://api.deepseek.com").rstrip("/")
    api_key = os.environ.get("DEEPSEEK_API_KEY", "").strip()
    model = os.environ.get("DEEPSEEK_MODEL_NAME", "deepseek-v4-pro")
    timeout = int(os.environ.get("DEEPSEEK_REQUEST_TIMEOUT_SECONDS", "120"))
    if not api_key:
        raise RuntimeError("DEEPSEEK_API_KEY is required")
    body = {
        "model": model,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ],
        "response_format": {"type": "json_object"},
        "temperature": temperature,
        "max_tokens": max_tokens,
    }
    request = urllib.request.Request(
        f"{base_url}/chat/completions",
        data=json.dumps(body, ensure_ascii=False).encode("utf-8"),
        headers={
            "Accept": "application/json",
            "Content-Type": "application/json",
            "Authorization": f"Bearer {api_key}",
        },
        method="POST",
    )
    max_attempts = int(os.environ.get("DEEPSEEK_AB_MAX_ATTEMPTS", "3"))
    last_error = None
    total_elapsed_ms = 0.0
    for attempt in range(1, max_attempts + 1):
        started = time.perf_counter()
        try:
            with urllib.request.urlopen(request, timeout=timeout) as response:
                payload = json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as error:
            raise RuntimeError(f"DeepSeek HTTP {error.code}") from error
        elapsed_ms = round((time.perf_counter() - started) * 1000, 2)
        total_elapsed_ms += elapsed_ms
        choices = payload.get("choices") or []
        if not choices:
            last_error = RuntimeError("DeepSeek response did not include choices")
        else:
            content = choices[0].get("message", {}).get("content", "")
            if content:
                try:
                    return parse_json_object(content), round(total_elapsed_ms, 2)
                except json.JSONDecodeError as exception:
                    last_error = RuntimeError("DeepSeek response JSON content is malformed")
                    if attempt >= max_attempts:
                        raise last_error from exception
                    time.sleep(1.0)
                    continue
            last_error = RuntimeError("DeepSeek response content is empty")
        if attempt < max_attempts:
            time.sleep(1.0)
    raise last_error or RuntimeError("DeepSeek request failed")


def lyrics_system_prompt():
    return """
你是燕云十六声 AI 作曲平台的顶级中文作词 Agent。

你的身份：
你是世界级中文作词家、游戏主题曲歌词创作者、音乐叙事导演。

任务：
根据用户输入、曲风偏好和知识库上下文，生成适合 AI 音乐模型演唱的中文原创歌词，并输出歌名、摘要、音乐方向和封面视觉种子。

硬规则：
1. 歌词内容必须属于《燕云十六声》大世界气质，但不要求反复出现“燕云”“十六声”等字面关键词。
2. 知识库上下文只作为事实、人物情绪、场景气质和意象素材；不要照抄资料，不要写成剧情百科。
3. 不编造官方设定、人物关系、阵营结论或未公开内容；待核线索只能写成暗线、传闻、旧事或情绪。
4. 歌词必须可唱，不能像散文、设定介绍或宣传文案。
5. 生成前先在内部选择最适合本题的作词路径，而不是套固定流程；这些规划不要输出。
6. 可选路径包括但不限于：叙事型、意象型、口语型、对白/独白型、群像型、反差型、重复型、反讽型、留白型、反套路型。
7. 不按曲风套模板；music_style 只影响语言声口、句子松紧、重复程度、节奏感和能量，不决定创意结构。
8. Hook 不限定为金句或口号；可以是一句、一个重复句式、一个口头禅、一个声音动作、一个意象回环或一段节奏记忆点。
9. 避开第一反应俗套，例如“侠=自由、离别=月光、江湖=风雨、少年=逍遥”；必须找到更有作品感、更具体、更有人的入口。
10. 歌词要像一个真实的人、一类人或一个可信声音在唱；不要像平台宣传文案、剧情简介或漂亮作文。
11. 允许留白，不要解释透所有剧情；用动作、物件、场景、重复或沉默让听众自己补完。
12. 生成前内部确定声音记忆点、主韵脚或节奏回环、段落情绪递进和 3-5 个具体画面；这些规划不要输出。
13. 整首歌不得完全无韵；副歌必须有一个主韵脚或清晰节奏回环，至少 2-4 行自然使用相同或相近韵母、句式或声音节奏。
14. 中文歌词优先 7-14 字短句，长句要拆分，避免像散文、小说分行或剧情梗概。
15. 每个主要段落至少有一个具体动作、物件或场景，不只写抽象情绪和漂亮形容词。
16. 允许讲故事，但不能只按事件顺序说明发生了什么；每段都要有可唱的情绪句、声音记忆点或回环句。
17. 必须有情绪变化或意义加深，不要从头到尾只是同一种正确情绪。
18. 避免廉价古风堆词，不要过度使用“红尘、宿命、刀光剑影、天涯、此生无悔”等空泛表达。
19. 减少“明月、山河、江湖、风烟、长夜、流浪、故乡”等万能诗性词连续堆叠；这些词可以使用，但每段不要当填充词连用，出现时必须落到具体动作、物件或场景上。
20. 如果一段里出现 3 个以上万能词，内部重写替换为更具体的燕云画面、人物动作或生活细节；禁止只靠“酒、剑、月、风”撑完整首歌。
21. 押韵要自然，不为了押韵牺牲内容，不使用生硬倒装、土味口号、网络梗或廉价古风词。
22. 必须原创，不模仿、不改写、不借用现实歌曲歌词、影视台词或已有商业歌词。
23. 不写真实歌手名、现实歌曲名、翻唱导向、仿唱导向。
24. 只输出 JSON object，不输出 Markdown 或解释。

建议结构：
[Verse 1]
[Pre-Chorus]
[Chorus]
[Verse 2]
[Bridge]
[Final Chorus]
[Outro]

输出 JSON 字段：
{
  "song_title": "中文歌名，不超过 10 个字",
  "song_summary": "一句话概括歌曲故事和情绪",
  "lyrics_text": "完整歌词，包含段落标签",
  "music_prompt": "简洁音乐方向",
  "cover_prompt_seed": "封面视觉方向",
  "risk_notes": [],
  "quality_score": 0.0
}
""".strip()


def generation_user_prompt(case, references):
    reference_blocks = []
    for index, ref in enumerate(references, start=1):
        reference_blocks.append(
            "\n".join(
                [
                    f"[ref {index}]",
                    f"entity={ref.get('entity_name', '')}",
                    f"heading={ref.get('heading_path', '')}",
                    f"fact_level={ref.get('fact_level', '')}",
                    f"summary={ref.get('summary_for_prompt', '')}",
                ]
            )
        )
    knowledge_context = "\n\n".join(reference_blocks) if reference_blocks else "无"
    return "\n".join(
        [
            "operation=INSPIRATION",
            "requested_title=",
            "music_style=开放曲风；按用户题材自行选择合适声口、节奏和能量，不默认国风",
            "vocal_preference=中文人声",
            f"user_input={case['input']}",
            "instruction=保留用户故事核心；写成燕云十六声大世界下的可唱歌曲；不要把资料复述成百科。",
            f"knowledge_context={knowledge_context}",
        ]
    )


def judge_system_prompt():
    return """
你是燕云十六声 AI 作曲平台的严格歌词 A/B 评审。
你会看到同一个用户输入下的 A 组歌词和 B 组歌词。
A 组没有知识库上下文；B 组使用燕云创作知识库上下文。

请严格评分，不要默认偏向 B 组。重点判断：
1. 燕云世界归属感：像不像发生在燕云十六声大世界，而不是泛古风。
2. 用户故事保真：是否保留用户想表达的核心。
3. 角色/剧情/地域准确性：是否跑题、混入其他 IP、乱编定论。
4. 创作入口：是否有独特角度，而不是第一反应的泛江湖套话。
5. 可唱性：是否像歌词而不是散文、剧情简介或漂亮作文。
6. 声音记忆点：可以是句子、重复、节奏、意象回环或口吻，不限定为金句。
7. 押韵自然度：整首是否避免完全无韵，副歌是否有自然韵脚或清晰节奏回环，且不硬凑。
8. 句长可唱性：中文短句是否利于演唱，是否避免散文化长句。
9. 情绪变化：是否有推进、变义或留白，而不是从头到尾只有同一种正确情绪。
10. 万能词密度：是否把“明月、山河、江湖、风烟、长夜、流浪、故乡”等当填充词连续堆叠；越低越好。
11. 泛古风污染：越低越好。

不要在输出中引用完整歌词原文，不要长段摘录。只输出 JSON object。

输出字段：
{
  "winner": "A | B | TIE",
  "a_scores": {
    "world_fit": 0,
    "user_story_preservation": 0,
    "accuracy": 0,
    "singability": 0,
    "hook": 0,
    "rhyme": 0,
    "line_length": 0,
    "generic_word_density_penalty": 0,
    "generic_wuxia_penalty": 0,
    "overall": 0
  },
  "b_scores": {
    "world_fit": 0,
    "user_story_preservation": 0,
    "accuracy": 0,
    "singability": 0,
    "hook": 0,
    "rhyme": 0,
    "line_length": 0,
    "generic_word_density_penalty": 0,
    "generic_wuxia_penalty": 0,
    "overall": 0
  },
  "reasons": ["最多 4 条简短、可执行原因"],
  "b_improvement_summary": "B 组如果更好，说明好在哪里；否则说明没有改善",
  "recommended_next_action": "下一步建议"
}
""".strip()


def judge_user_prompt(case, a_output, b_output, b_entities):
    def compact_output(output):
        lyrics = output.get("lyrics_text") or ""
        max_chars = int(os.environ.get("DEEPSEEK_AB_JUDGE_LYRICS_MAX_CHARS", "4200"))
        if len(lyrics) > max_chars:
            lyrics = lyrics[:max_chars] + "\n[TRUNCATED_FOR_JUDGE]"
        return {
            "song_title": output.get("song_title"),
            "song_summary": output.get("song_summary"),
            "lyrics_text": lyrics,
            "music_prompt": output.get("music_prompt"),
            "risk_notes": output.get("risk_notes", []),
        }

    return json.dumps(
        {
            "case_id": case["id"],
            "user_input": case["input"],
            "expected_entities": case.get("expected_entities", []),
            "b_retrieved_entities": b_entities,
            "a_output": compact_output(a_output),
            "b_output": compact_output(b_output),
        },
        ensure_ascii=False,
    )


def sanitize_output(output):
    lyrics = output.get("lyrics_text") or ""
    lines = [line for line in lyrics.splitlines() if line.strip()]
    return {
        "song_title": first_non_blank(output.get("song_title"), "<missing>"),
        "song_summary": first_non_blank(output.get("song_summary"), "<missing>"),
        "quality_score": output.get("quality_score"),
        "lyrics_hash": sha256(lyrics),
        "lyrics_line_count": len(lines),
        "has_chorus": "[Chorus]" in lyrics or "副歌" in lyrics,
        "risk_notes_count": len(output.get("risk_notes") or []),
    }


def selected_cases(payload):
    sample_ids = [
        item.strip()
        for item in os.environ.get("REAL_AB_SAMPLE_IDS", ",".join(DEFAULT_SAMPLE_IDS)).split(",")
        if item.strip()
    ]
    limit = int(os.environ.get("REAL_AB_SAMPLE_LIMIT", str(len(sample_ids))))
    cases_by_id = {case["id"]: case for case in payload["eval_cases"]}
    cases = [cases_by_id[case_id] for case_id in sample_ids if case_id in cases_by_id]
    if len(cases) < limit:
        for case in payload["eval_cases"]:
            if case.get("expected_entities") and case not in cases:
                cases.append(case)
            if len(cases) >= limit:
                break
    return cases[:limit]


def ensure_execute_allowed():
    missing = []
    if os.environ.get("ALLOW_REAL_MODEL_SMOKE") != "1":
        missing.append("ALLOW_REAL_MODEL_SMOKE=1")
    if os.environ.get("ALLOW_DEEPSEEK_REAL_AB") != "1":
        missing.append("ALLOW_DEEPSEEK_REAL_AB=1")
    if not os.environ.get("DEEPSEEK_API_KEY"):
        missing.append("DEEPSEEK_API_KEY")
    if missing:
        raise RuntimeError("missing required execution gates: " + ", ".join(missing))


def run_execute():
    ensure_execute_allowed()
    retrieval = load_retrieval_module()
    payload = load_payload(KB_DIR)
    id_to_key = retrieval.entity_id_map(payload)
    cases = selected_cases(payload)
    case_results = []
    for index, case in enumerate(cases, start=1):
        print(f"running real lyrics A/B {index}/{len(cases)}: {case['id']}", flush=True)
        refs = retrieval.retrieve_pgvector(payload, case["input"], limit=6, entity_limit=4)
        b_entities = [id_to_key.get(ref["entity_id"], "<unknown>") for ref in refs]
        a_output, a_elapsed = deepseek_chat_json(
            lyrics_system_prompt(), generation_user_prompt(case, []), temperature=0.62
        )
        b_output, b_elapsed = deepseek_chat_json(
            lyrics_system_prompt(), generation_user_prompt(case, refs), temperature=0.62
        )
        judge, judge_elapsed = deepseek_chat_json(
            judge_system_prompt(),
            judge_user_prompt(case, a_output, b_output, b_entities),
            temperature=0.1,
            max_tokens=1800,
        )
        case_results.append(
            {
                "id": case["id"],
                "input": case["input"],
                "expected_entities": case.get("expected_entities", []),
                "b_retrieved_entities": b_entities,
                "a_disabled": {
                    "reference_count": 0,
                    "elapsed_ms": a_elapsed,
                    "output": sanitize_output(a_output),
                },
                "b_pgvector": {
                    "reference_count": len(refs),
                    "elapsed_ms": b_elapsed,
                    "output": sanitize_output(b_output),
                },
                "judge": judge,
                "judge_elapsed_ms": judge_elapsed,
            }
        )
    return summarize(payload, case_results)


def run_execute_manual_review():
    ensure_execute_allowed()
    retrieval = load_retrieval_module()
    payload = load_payload(KB_DIR)
    id_to_key = retrieval.entity_id_map(payload)
    cases = selected_cases(payload)
    case_results = []
    for index, case in enumerate(cases, start=1):
        print(f"running real lyrics A/B manual review {index}/{len(cases)}: {case['id']}", flush=True)
        refs = retrieval.retrieve_pgvector(payload, case["input"], limit=6, entity_limit=4)
        b_entities = [id_to_key.get(ref["entity_id"], "<unknown>") for ref in refs]
        a_user_prompt = generation_user_prompt(case, [])
        b_user_prompt = generation_user_prompt(case, refs)
        a_output, a_elapsed = deepseek_chat_json(
            lyrics_system_prompt(), a_user_prompt, temperature=0.62
        )
        b_output, b_elapsed = deepseek_chat_json(
            lyrics_system_prompt(), b_user_prompt, temperature=0.62
        )
        case_results.append(
            {
                "id": case["id"],
                "input": case["input"],
                "expected_entities": case.get("expected_entities", []),
                "b_retrieved_entities": b_entities,
                "prompts": {
                    "system_prompt": lyrics_system_prompt(),
                    "a_user_prompt": a_user_prompt,
                    "b_user_prompt": b_user_prompt,
                },
                "a_disabled": {
                    "reference_count": 0,
                    "elapsed_ms": a_elapsed,
                    "output": a_output,
                },
                "b_pgvector": {
                    "reference_count": len(refs),
                    "elapsed_ms": b_elapsed,
                    "output": b_output,
                },
            }
        )
    return {
        "summary": {
            "ab_version": "yanyun-kb-real-lyrics-ab-manual-review-v0.1",
            "generated_at": datetime.now(timezone.utc).isoformat(),
            "kb_version": payload["kb_version"],
            "model": os.environ.get("DEEPSEEK_MODEL_NAME", "deepseek-v4-pro"),
            "sample_count": len(case_results),
            "a_disabled": "no knowledge context",
            "b_pgvector": "pgvector knowledge context",
            "judge": "manual-human-review",
            "deepseek_judge_called": False,
            "full_lyrics_persisted": True,
            "full_prompts_persisted": True,
        },
        "cases": case_results,
    }


def summarize(payload, case_results):
    b_wins = sum(1 for result in case_results if result["judge"].get("winner") == "B")
    a_wins = sum(1 for result in case_results if result["judge"].get("winner") == "A")
    ties = sum(1 for result in case_results if result["judge"].get("winner") == "TIE")
    avg_a = average_score(case_results, "a_scores", "overall")
    avg_b = average_score(case_results, "b_scores", "overall")
    avg_world_a = average_score(case_results, "a_scores", "world_fit")
    avg_world_b = average_score(case_results, "b_scores", "world_fit")
    return {
        "summary": {
            "ab_version": "yanyun-kb-real-lyrics-ab-v0.1",
            "generated_at": datetime.now(timezone.utc).isoformat(),
            "kb_version": payload["kb_version"],
            "model": os.environ.get("DEEPSEEK_MODEL_NAME", "deepseek-v4-pro"),
            "sample_count": len(case_results),
            "a_disabled": "no knowledge context",
            "b_pgvector": "pgvector knowledge context",
            "b_wins": b_wins,
            "a_wins": a_wins,
            "ties": ties,
            "avg_a_overall": avg_a,
            "avg_b_overall": avg_b,
            "avg_a_world_fit": avg_world_a,
            "avg_b_world_fit": avg_world_b,
            "full_lyrics_persisted": False,
        },
        "cases": case_results,
    }


def average_score(case_results, score_group, field):
    values = []
    for result in case_results:
        score = result["judge"].get(score_group, {}).get(field)
        if isinstance(score, (int, float)):
            values.append(float(score))
    return round(sum(values) / len(values), 2) if values else 0


def write_report(result):
    summary = result["summary"]
    lines = [
        "# 燕云创作知识库真实歌词 A/B v0.1",
        "",
        f"生成时间：`{summary['generated_at']}`",
        "",
        "## 结论",
        "",
        f"- 知识库版本：`{summary['kb_version']}`。",
        f"- 模型：`{summary['model']}`。",
        f"- 样本数：`{summary['sample_count']}`。",
        "- A 组：禁用知识库上下文。",
        "- B 组：注入本地 PostgreSQL/pgvector 召回的知识库上下文。",
        f"- 胜负：B 胜 `{summary['b_wins']}`，A 胜 `{summary['a_wins']}`，平局 `{summary['ties']}`。",
        f"- 平均 overall：A `{summary['avg_a_overall']}`，B `{summary['avg_b_overall']}`。",
        f"- 平均燕云世界归属感：A `{summary['avg_a_world_fit']}`，B `{summary['avg_b_world_fit']}`。",
        "- 本报告不保存完整歌词、完整 prompt、供应商原始响应、密钥或媒体 URL；歌词仅保存 hash、标题、摘要和结构统计。",
        "",
        "## 样本明细",
        "",
        "| id | 期望实体 | B 召回实体 | winner | A overall | B overall | 评审摘要 |",
        "| --- | --- | --- | --- | ---: | ---: | --- |",
    ]
    for case in result["cases"]:
        judge = case["judge"]
        reasons = "; ".join(str(item) for item in judge.get("reasons", [])[:2])
        reasons = reasons.replace("|", "\\|")
        if len(reasons) > 140:
            reasons = reasons[:139] + "…"
        expected = ", ".join(f"`{item}`" for item in case.get("expected_entities", []))
        retrieved = ", ".join(f"`{item}`" for item in case.get("b_retrieved_entities", []))
        lines.append(
            "| `{id}` | {expected} | {retrieved} | `{winner}` | {a} | {b} | {reasons} |".format(
                id=case["id"],
                expected=expected,
                retrieved=retrieved,
                winner=judge.get("winner", "<missing>"),
                a=judge.get("a_scores", {}).get("overall", ""),
                b=judge.get("b_scores", {}).get("overall", ""),
                reasons=reasons,
            )
        )
    lines += [
        "",
        "## 后续建议",
        "",
        "- 如果 B 组优势稳定，下一步把 `KNOWLEDGE_RETRIEVAL_MODE=pgvector` 纳入本地真实用户测试默认配置。",
        "- all-hit 未覆盖的多实体题，后续补实体关系扩展检索：角色 -> 地域/剧情/势力的轻量关联召回。",
        "- 真实生产前仍需要公司确认官方资料包、品牌禁区和可剧透边界。",
    ]
    REPORT_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_manual_review_report(result):
    summary = result["summary"]
    lines = [
        "# 燕云创作知识库真实歌词 A/B 手动评审稿 v0.1",
        "",
        f"生成时间：`{summary['generated_at']}`",
        "",
        "## 说明",
        "",
        f"- 知识库版本：`{summary['kb_version']}`。",
        f"- 模型：`{summary['model']}`。",
        f"- 样本数：`{summary['sample_count']}`。",
        "- A 组：禁用知识库上下文。",
        "- B 组：注入本地 PostgreSQL/pgvector 召回的知识库上下文。",
        "- 本轮 DeepSeek 只负责生成歌词，没有调用 DeepSeek 评审。",
        "- 本文件包含完整歌词和完整写词 Prompt，仅用于本地人工评审，位于 `build/` 目录，不应提交到 Git。",
        "",
        "## 快速目录",
        "",
    ]
    for index, case in enumerate(result["cases"], start=1):
        lines.append(f"{index}. `{case['id']}`：{case['input']}")
    lines.append("")

    for index, case in enumerate(result["cases"], start=1):
        lines += [
            f"## 样本 {index}: `{case['id']}`",
            "",
            f"用户输入：{case['input']}",
            "",
            "期望实体：" + ", ".join(f"`{item}`" for item in case.get("expected_entities", [])),
            "",
            "B 组召回实体：" + ", ".join(f"`{item}`" for item in case.get("b_retrieved_entities", [])),
            "",
        ]
        prompts = case.get("prompts") or {}
        if prompts:
            lines += [
                "### 写词 Prompt",
                "",
                "#### System Prompt",
                "",
                "```text",
                first_non_blank(prompts.get("system_prompt"), "<missing>"),
                "```",
                "",
                "#### A 组 User Prompt：无知识库",
                "",
                "```text",
                first_non_blank(prompts.get("a_user_prompt"), "<missing>"),
                "```",
                "",
                "#### B 组 User Prompt：pgvector 知识库",
                "",
                "```text",
                first_non_blank(prompts.get("b_user_prompt"), "<missing>"),
                "```",
                "",
            ]
        for label, key in [("A 组：无知识库", "a_disabled"), ("B 组：pgvector 知识库", "b_pgvector")]:
            output = case[key]["output"]
            lines += [
                f"### {label}",
                "",
                f"- 生成耗时：`{case[key]['elapsed_ms']}ms`",
                f"- 歌名：{first_non_blank(output.get('song_title'), '<missing>')}",
                f"- 摘要：{first_non_blank(output.get('song_summary'), '<missing>')}",
                f"- 模型自评分：`{output.get('quality_score')}`",
                f"- 风险提示：{json.dumps(output.get('risk_notes') or [], ensure_ascii=False)}",
                f"- 音乐方向：{first_non_blank(output.get('music_prompt'), '<missing>')}",
                f"- 封面种子：{first_non_blank(output.get('cover_prompt_seed'), '<missing>')}",
                "",
                "```text",
                first_non_blank(output.get("lyrics_text"), "<missing>"),
                "```",
                "",
            ]
        lines += [
            "### 人工评审记录",
            "",
            "- 我的判断：",
            "- 押韵 / 句长 / 副歌 hook：",
            "- 你的判断：",
            "- 后续改 Prompt / 知识库建议：",
            "",
        ]
    MANUAL_REVIEW_REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
    MANUAL_REVIEW_REPORT_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")


def mode_plan():
    print(
        json.dumps(
            {
                "mode": "plan",
                "will_call_real_deepseek": False,
                "execute_requires": [
                    "ALLOW_REAL_MODEL_SMOKE=1",
                    "ALLOW_DEEPSEEK_REAL_AB=1",
                    "DEEPSEEK_API_KEY",
                ],
                "default_sample_ids": DEFAULT_SAMPLE_IDS,
                "outputs": [str(REPORT_PATH), str(RESULT_PATH)],
                "manual_review_outputs": [str(MANUAL_REVIEW_REPORT_PATH), str(MANUAL_REVIEW_RESULT_PATH)],
                "full_lyrics_persisted": False,
                "manual_review_mode": "MODE=execute_manual_review persists full generated lyrics under build/ only",
            },
            ensure_ascii=False,
            indent=2,
        )
    )


def mode_preflight():
    ensure_execute_allowed()
    payload = load_payload(KB_DIR)
    retrieval = load_retrieval_module()
    first_case = selected_cases(payload)[0]
    refs = retrieval.retrieve_pgvector(payload, first_case["input"], limit=2, entity_limit=2)
    print(
        json.dumps(
            {
                "mode": "preflight",
                "ready": True,
                "kb_version": payload["kb_version"],
                "sample_count": len(selected_cases(payload)),
                "first_case": first_case["id"],
                "first_case_reference_count": len(refs),
            },
            ensure_ascii=False,
        )
    )


def main():
    mode = os.environ.get("MODE", "plan")
    if mode == "plan":
        mode_plan()
        return 0
    if mode == "preflight":
        mode_preflight()
        return 0
    if mode == "execute_manual_review":
        result = run_execute_manual_review()
        MANUAL_REVIEW_RESULT_PATH.parent.mkdir(parents=True, exist_ok=True)
        MANUAL_REVIEW_RESULT_PATH.write_text(
            json.dumps(result, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        write_manual_review_report(result)
        print(json.dumps(result["summary"], ensure_ascii=False))
        return 0
    if mode != "execute":
        raise RuntimeError("MODE must be plan, preflight, execute, or execute_manual_review")
    result = run_execute()
    RESULT_PATH.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    write_report(result)
    print(json.dumps(result["summary"], ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
