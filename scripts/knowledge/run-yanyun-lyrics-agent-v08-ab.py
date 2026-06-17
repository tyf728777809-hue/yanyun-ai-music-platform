#!/usr/bin/env python3
import hashlib
import json
import multiprocessing
import os
import subprocess
import sys
import textwrap
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
_EVAL_CASES_PATH = Path(
    os.environ.get(
        "LYRICS_AGENT_AB_CASES_PATH",
        str(ROOT / "knowledge-base/lyrics-agent-v0.8-eval-cases.json"),
    )
)
EVAL_CASES_PATH = _EVAL_CASES_PATH if _EVAL_CASES_PATH.is_absolute() else ROOT / _EVAL_CASES_PATH
KB_DIR = ROOT / "knowledge-base/commercial-final"
REPORT_DIR = ROOT / "build/reports/lyrics-agent"
MANUAL_REVIEW_MD = REPORT_DIR / "yanyun-lyrics-agent-v08-ab-manual-review.md"
MANUAL_REVIEW_JSON = REPORT_DIR / "yanyun-lyrics-agent-v08-ab-manual-review.json"
LYRICS_CLIENT_PATH = (
    ROOT / "modules/deepseek/src/main/java/com/yanyun/music/deepseek/RealDeepSeekLyricsClient.java"
)
BRIEF_AGENT_PATH = (
    ROOT
    / "modules/deepseek/src/main/java/com/yanyun/music/deepseek/RealDeepSeekCreativeBriefAgent.java"
)
BASELINE_REV = os.environ.get("LYRICS_AGENT_V07_GIT_REV", "81703be4")
CURRENT_VARIANT_LABEL = os.environ.get("LYRICS_AGENT_CURRENT_VARIANT_LABEL", "B_v0.11-direct")


def sha256(value):
    return hashlib.sha256((value or "").encode("utf-8")).hexdigest()


def load_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


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


def deepseek_chat_json(system_prompt, user_prompt, temperature=0.45, max_tokens=4096):
    timeout = int(os.environ.get("DEEPSEEK_REQUEST_TIMEOUT_SECONDS", "180"))
    wall_timeout = int(os.environ.get("DEEPSEEK_AB_WALL_TIMEOUT_SECONDS", str(timeout + 30)))
    attempts = int(os.environ.get("DEEPSEEK_AB_MAX_ATTEMPTS", "3"))
    last_error = None
    total_elapsed_ms = 0
    ctx = multiprocessing.get_context("fork")
    for attempt in range(1, attempts + 1):
        parent_conn, child_conn = ctx.Pipe(duplex=False)
        process = ctx.Process(
            target=_deepseek_chat_json_worker,
            args=(child_conn, system_prompt, user_prompt, temperature, max_tokens, timeout),
        )
        started = time.perf_counter()
        process.start()
        child_conn.close()
        try:
            process.join(wall_timeout)
        except KeyboardInterrupt:
            if process.is_alive():
                process.terminate()
                process.join(5)
                if process.is_alive():
                    process.kill()
                    process.join(1)
            parent_conn.close()
            raise
        elapsed_ms = int((time.perf_counter() - started) * 1000)
        total_elapsed_ms += elapsed_ms
        if process.is_alive():
            process.terminate()
            process.join(5)
            if process.is_alive():
                process.kill()
                process.join(1)
            parent_conn.close()
            last_error = RuntimeError("DeepSeek request timed out")
            if attempt >= attempts:
                raise last_error
            continue
        if not parent_conn.poll():
            parent_conn.close()
            last_error = RuntimeError("DeepSeek request ended without result")
            if attempt >= attempts:
                raise last_error
            continue
        status, payload = parent_conn.recv()
        parent_conn.close()
        if status == "ok":
            result, request_elapsed_ms = payload
            return result, total_elapsed_ms if total_elapsed_ms > request_elapsed_ms else request_elapsed_ms
        last_error = RuntimeError(payload)
        if attempt >= attempts:
            raise last_error
        time.sleep(1.0)
    raise last_error or RuntimeError("DeepSeek request failed")


def _deepseek_chat_json_worker(conn, system_prompt, user_prompt, temperature, max_tokens, timeout):
    try:
        conn.send(
            (
                "ok",
                _deepseek_chat_json_once(system_prompt, user_prompt, temperature, max_tokens, timeout),
            )
        )
    except Exception as error:
        conn.send(("error", str(error)))
    finally:
        conn.close()


def _deepseek_chat_json_once(system_prompt, user_prompt, temperature=0.45, max_tokens=4096, timeout=180):
    base_url = os.environ.get("DEEPSEEK_BASE_URL", "https://api.deepseek.com").rstrip("/")
    api_key = os.environ.get("DEEPSEEK_API_KEY", "").strip()
    model = os.environ.get("DEEPSEEK_MODEL_NAME", "deepseek-v4-pro")
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
    started = time.perf_counter()
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        raise RuntimeError(f"DeepSeek HTTP {error.code}") from error
    elapsed_ms = int((time.perf_counter() - started) * 1000)
    choices = payload.get("choices") or []
    if not choices:
        raise RuntimeError("DeepSeek response did not include choices")
    content = choices[0].get("message", {}).get("content", "")
    if not content:
        raise RuntimeError("DeepSeek response content is empty")
    try:
        return parse_json_object(content), elapsed_ms
    except json.JSONDecodeError as error:
        raise RuntimeError("DeepSeek response JSON content is malformed") from error


def extract_java_text_block_from_text(source, method_signature):
    marker = source.find(method_signature)
    if marker < 0:
        return ""
    start = source.find('"""', marker)
    if start < 0:
        return ""
    end = source.find('"""', start + 3)
    if end < 0:
        return ""
    return textwrap.dedent(source[start + 3 : end]).strip()


def read_git_file(revision, relative_path):
    try:
        result = subprocess.run(
            ["git", "show", f"{revision}:{relative_path}"],
            cwd=ROOT,
            text=True,
            capture_output=True,
            check=True,
        )
        return result.stdout
    except subprocess.CalledProcessError:
        return ""


def production_prompt(path, method_signature):
    if not path.exists():
        return ""
    return extract_java_text_block_from_text(path.read_text(encoding="utf-8"), method_signature)


def baseline_prompt(relative_path, method_signature, fallback):
    source = read_git_file(BASELINE_REV, relative_path)
    if source:
        prompt = extract_java_text_block_from_text(source, method_signature)
        if prompt:
            return prompt
    return fallback.strip()


def creative_brief_system_prompt(version):
    if version != "A_v0.7":
        return production_prompt(BRIEF_AGENT_PATH, "private String systemPrompt()")
    return baseline_prompt(
        "modules/deepseek/src/main/java/com/yanyun/music/deepseek/RealDeepSeekCreativeBriefAgent.java",
        "private String systemPrompt()",
        """
        你是燕云十六声 AI 作曲平台的创作可能性判断 Agent。
        判断用户请求是否属于燕云创作域，并输出 creative_core、chosen_angle、anti_cliche_strategy、voice_texture、image_pool、song_energy、song_thesis、pov、central_tension、emotional_turn、chorus_function、memory_device。
        只输出 JSON object。
        """,
    )


def lyrics_system_prompt(version):
    if version != "A_v0.7":
        return production_prompt(LYRICS_CLIENT_PATH, "private String systemPrompt()")
    return baseline_prompt(
        "modules/deepseek/src/main/java/com/yanyun/music/deepseek/RealDeepSeekLyricsClient.java",
        "private String systemPrompt()",
        """
        你是燕云十六声 AI 作曲平台的顶级中文作词 Agent。
        根据用户输入、知识库上下文、曲风偏好和人声偏好，生成属于燕云十六声大世界的中文原创歌词。
        歌词必须可唱、原创、有燕云气质；避免泛古风、剧情百科和廉价堆词。
        输出 song_title、song_summary、lyrics_text、music_prompt、cover_prompt_seed、risk_notes、quality_score 的 JSON object。
        """,
    )


def flatten_knowledge():
    rows = []
    for path in sorted(KB_DIR.glob("*.json")):
        if path.name in {"manifest.json", "eval-cases.json"}:
            continue
        data = load_json(path)
        for entity in data.get("entities", []):
            aliases = entity.get("aliases") or []
            cards = entity.get("cards") or []
            for card in cards:
                rows.append(
                    {
                        "chunk_id": f"{entity.get('key')}:{card.get('id')}",
                        "display_name": entity.get("canonical_name", entity.get("key", "")),
                        "entity_type": entity.get("entity_type", data.get("domain", "")),
                        "aliases": aliases,
                        "fact_level": entity.get("fact_level", ""),
                        "source_ref": entity.get("source_ref", ""),
                        "summary": card.get("summary_for_prompt", ""),
                        "imagery": card.get("usable_imagery", []),
                        "avoid_claims": card.get("avoid_claims", []),
                        "content": card.get("content", ""),
                    }
                )
    return rows


def score_row(row, query):
    text = query.lower()
    score = 0
    for alias in row.get("aliases", []):
        if alias and alias.lower() in text:
            score += 12
    name = row.get("display_name", "")
    if name and name.lower() in text:
        score += 15
    haystack = " ".join(
        [
            row.get("display_name", ""),
            row.get("entity_type", ""),
            " ".join(row.get("aliases", [])),
            row.get("summary", ""),
            " ".join(row.get("imagery", [])),
        ]
    ).lower()
    for token in [part for part in query.replace("，", " ").replace("。", " ").split() if part]:
        if len(token) >= 2 and token.lower() in haystack:
            score += 2
    return score


def retrieve_knowledge(case, knowledge_rows):
    query = " ".join(
        [
            case.get("user_input", ""),
            case.get("mood", ""),
            case.get("music_style", ""),
            " ".join(case.get("expected_focus", [])),
        ]
    )
    ranked = sorted(
        ((score_row(row, query), row) for row in knowledge_rows),
        key=lambda item: item[0],
        reverse=True,
    )
    refs = [row for score, row in ranked if score > 0][:6]
    if refs:
        return refs
    return [
        {
            "chunk_id": "fallback:yanyun-world",
            "display_name": "燕云十六声创作域",
            "entity_type": "world",
            "fact_level": "fallback",
            "source_ref": "local-script-fallback",
            "summary": "保持燕云十六声大世界气质，优先保留用户故事，不强行塞官方名词。",
            "imagery": [],
            "avoid_claims": ["不要写成其他 IP 或泛古风"],
            "content": "",
        }
    ]


def creative_brief_user_prompt(case, references):
    return "\n".join(
        [
            "operation=INSPIRATION",
            "user_input=" + case.get("user_input", ""),
            "current_lyrics=",
            "instruction=",
            "requested_title=",
            "mood=" + case.get("mood", ""),
            "music_style=" + case.get("music_style", ""),
            "vocal_preference=" + case.get("vocal_preference", ""),
            "yanyun_references=" + json.dumps(reference_summaries(references), ensure_ascii=False),
        ]
    )


def reference_summaries(references):
    return [
        {
            "chunk_id": row.get("chunk_id"),
            "display_name": row.get("display_name"),
            "fact_level": row.get("fact_level"),
            "summary": row.get("summary"),
            "usable_imagery": row.get("imagery", []),
            "avoid_claims": row.get("avoid_claims", []),
        }
        for row in references
    ]


def brief_instruction(version, brief, references):
    refs = reference_summaries(references)
    if version != "A_v0.7":
        keys = [
            "song_core",
            "singer_voice",
            "listener_target",
            "emotional_engine",
            "chorus_job",
            "avoid_direction",
            "creative_core",
            "chosen_angle",
            "anti_cliche_strategy",
            "voice_texture",
            "image_pool",
            "song_energy",
            "song_thesis",
            "pov",
            "central_tension",
            "emotional_turn",
            "chorus_function",
            "memory_device",
        ]
        policy = "First complete song_core, singer_voice, emotional_engine, and chorus_job. Knowledge only serves song_core."
    else:
        keys = [
            "creative_core",
            "chosen_angle",
            "anti_cliche_strategy",
            "voice_texture",
            "image_pool",
            "song_energy",
            "song_thesis",
            "pov",
            "central_tension",
            "emotional_turn",
            "chorus_function",
            "memory_device",
        ]
        policy = "Use the creative brief as open guidance. Keep one clear song thesis."
    lines = ["Creative brief:"]
    for key in keys:
        lines.append(f"{key}={brief.get(key, '')}")
    lines.append("knowledge_references=" + json.dumps(refs, ensure_ascii=False))
    lines.append("songcraft_policy=" + policy)
    return "\n".join(lines)


def lyrics_user_prompt(case, brief_instruction_text):
    return "\n".join(
        [
            "operation=INSPIRATION",
            "requested_title=",
            "mood=" + case.get("mood", ""),
            "music_style=" + case.get("music_style", ""),
            "vocal_preference=" + case.get("vocal_preference", ""),
            "user_input=" + case.get("user_input", ""),
            "current_lyrics=",
            "instruction=" + brief_instruction_text,
            "rendered_prompt=lyrics.inspiration.v1 manual A/B render",
            "yanyun_references=" + json.dumps(case.get("expected_focus", []), ensure_ascii=False),
        ]
    )


def select_cases(payload):
    case_by_id = {case["id"]: case for case in payload["cases"]}
    ids_env = os.environ.get("LYRICS_AGENT_AB_SAMPLE_IDS", "").strip()
    if ids_env:
        ids = [item.strip() for item in ids_env.split(",") if item.strip()]
    else:
        ids = list(payload.get("initial_batch_ids", []))
    limit = int(os.environ.get("LYRICS_AGENT_AB_SAMPLE_LIMIT", str(len(ids) or 6)))
    if not ids_env and limit > len(ids):
        seen = set(ids)
        for case in payload["cases"]:
            case_id = case["id"]
            if case_id not in seen:
                ids.append(case_id)
                seen.add(case_id)
    selected = []
    for case_id in ids:
        if case_id not in case_by_id:
            raise RuntimeError(f"Unknown sample id: {case_id}")
        selected.append(case_by_id[case_id])
    return selected[:limit]


def assert_gates(mode):
    if mode in {"plan", "preflight"}:
        return
    if os.environ.get("ALLOW_REAL_MODEL_SMOKE") != "1":
        raise RuntimeError("ALLOW_REAL_MODEL_SMOKE=1 is required for execute mode")
    if os.environ.get("ALLOW_DEEPSEEK_REAL_AB") != "1":
        raise RuntimeError("ALLOW_DEEPSEEK_REAL_AB=1 is required for execute mode")


def percentile(values, ratio):
    if not values:
        return None
    ordered = sorted(values)
    index = int(round((len(ordered) - 1) * ratio))
    return ordered[max(0, min(index, len(ordered) - 1))]


def timing_stats(values):
    clean = [int(value) for value in values if isinstance(value, (int, float))]
    if not clean:
        return {}
    return {
        "count": len(clean),
        "min_ms": min(clean),
        "p50_ms": percentile(clean, 0.50),
        "avg_ms": round(sum(clean) / len(clean), 2),
        "p95_ms": percentile(clean, 0.95),
        "max_ms": max(clean),
    }


def latency_summary(results):
    by_variant = {}
    for result in results:
        for variant in result.get("variants", []):
            name = variant.get("variant", "unknown")
            bucket = by_variant.setdefault(
                name,
                {
                    "brief_elapsed_ms": [],
                    "lyrics_elapsed_ms": [],
                    "total_elapsed_ms": [],
                },
            )
            total = 0
            for key in ("brief_elapsed_ms", "lyrics_elapsed_ms"):
                value = variant.get(key)
                if isinstance(value, (int, float)):
                    bucket[key].append(value)
                    total += value
            if total > 0:
                bucket["total_elapsed_ms"].append(total)
    return {
        variant: {key: timing_stats(values) for key, values in timings.items()}
        for variant, timings in by_variant.items()
    }


def write_reports(results):
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    timings = latency_summary(results)
    payload = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "baseline_revision": BASELINE_REV,
        "current_variant": CURRENT_VARIANT_LABEL,
        "eval_cases_path": str(EVAL_CASES_PATH),
        "latency_summary": timings,
        "results": results,
    }
    MANUAL_REVIEW_JSON.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    lines = [
        "# LyricsAgent v0.11 Direct A/B Manual Review",
        "",
        f"- generated_at: `{payload['generated_at']}`",
        f"- baseline_revision: `{BASELINE_REV}`",
        f"- current_variant: `{CURRENT_VARIANT_LABEL}`",
        f"- eval_cases_path: `{EVAL_CASES_PATH}`",
        "- 注意：本文件包含完整 Prompt 和完整歌词，只保存在 ignored 的 build/ 目录，不要提交。",
        "",
        "## Latency Summary",
        "",
        "```json",
        json.dumps(timings, ensure_ascii=False, indent=2),
        "```",
        "",
    ]
    for result in results:
        case = result["case"]
        lines.extend(
            [
                f"## {case['id']}",
                "",
                f"- 用户输入：{case['user_input']}",
                f"- 心情：{case.get('mood', '')}",
                f"- 曲风：{case.get('music_style', '')}",
                f"- 评审重点：{case.get('review_notes', '')}",
                "",
                "### Knowledge Retrieval",
                "```json",
                json.dumps(result["knowledge_references"], ensure_ascii=False, indent=2),
                "```",
                "",
            ]
        )
        for variant in result["variants"]:
            output = variant.get("lyrics_output", {})
            lines.extend(
                [
                    f"### {variant['variant']}",
                    "",
                    f"- CreativeBrief 耗时：{variant.get('brief_elapsed_ms', '')}ms",
                    f"- Lyrics 耗时：{variant.get('lyrics_elapsed_ms', '')}ms",
                    f"- song_title: {output.get('song_title', '')}",
                    f"- song_summary: {output.get('song_summary', '')}",
                    f"- quality_score: {output.get('quality_score', '')}",
                    "",
                    "#### CreativeBrief",
                    "```json",
                    json.dumps(variant.get("creative_brief", {}), ensure_ascii=False, indent=2),
                    "```",
                    "",
                    "#### System Prompt",
                    "```text",
                    variant["lyrics_system_prompt"],
                    "```",
                    "",
                    "#### User Prompt",
                    "```text",
                    variant["lyrics_user_prompt"],
                    "```",
                    "",
                    "#### Lyrics",
                    "```text",
                    output.get("lyrics_text", ""),
                    "```",
                    "",
                    "#### 评委记录",
                    "- 总分：",
                    "- 结论（金曲潜力 / 可用但不够顶 / 不通过）：",
                    "- 失败归因：",
                    "- Prompt 是否理解用户输入：",
                    "- 知识库是否帮忙或干扰：",
                    "- CreativeBrief 是否讲清歌曲入口：",
                    "- LyricsAgent 是否兑现核心方向：",
                    "",
                ]
            )
    MANUAL_REVIEW_MD.write_text("\n".join(lines), encoding="utf-8")


def run_execute(cases, knowledge_rows):
    results = []
    for case in cases:
        print(f"[case] {case['id']}", file=sys.stderr, flush=True)
        references = retrieve_knowledge(case, knowledge_rows)
        result = {
            "case": case,
            "knowledge_references": reference_summaries(references),
            "variants": [],
        }
        for variant in ["A_v0.7", CURRENT_VARIANT_LABEL]:
            try:
                print(f"[case] {case['id']} [variant] {variant} brief", file=sys.stderr, flush=True)
                brief_system = creative_brief_system_prompt(variant)
                brief_user = creative_brief_user_prompt(case, references)
                brief, brief_elapsed_ms = deepseek_chat_json(
                    brief_system, brief_user, temperature=0.35, max_tokens=3000
                )
                instruction = brief_instruction(variant, brief, references)
                lyrics_system = lyrics_system_prompt(variant)
                lyrics_user = lyrics_user_prompt(case, instruction)
                print(f"[case] {case['id']} [variant] {variant} lyrics", file=sys.stderr, flush=True)
                lyrics, lyrics_elapsed_ms = deepseek_chat_json(
                    lyrics_system, lyrics_user, temperature=0.72, max_tokens=4096
                )
                print(f"[case] {case['id']} [variant] {variant} done", file=sys.stderr, flush=True)
                result["variants"].append(
                    {
                        "variant": variant,
                        "creative_brief_system_prompt_hash": sha256(brief_system),
                        "creative_brief_user_prompt_hash": sha256(brief_user),
                        "lyrics_system_prompt_hash": sha256(lyrics_system),
                        "lyrics_user_prompt_hash": sha256(lyrics_user),
                        "creative_brief": brief,
                        "lyrics_output": lyrics,
                        "brief_elapsed_ms": brief_elapsed_ms,
                        "lyrics_elapsed_ms": lyrics_elapsed_ms,
                        "lyrics_system_prompt": lyrics_system,
                        "lyrics_user_prompt": lyrics_user,
                    }
                )
            except RuntimeError as error:
                result["variants"].append(
                    {
                        "variant": variant,
                        "error": str(error),
                        "lyrics_output": {},
                        "creative_brief": {},
                        "lyrics_system_prompt": "",
                        "lyrics_user_prompt": "",
                    }
                )
        results.append(result)
        write_reports(results)
    return results


def main():
    mode = os.environ.get("MODE", "plan")
    payload = load_json(EVAL_CASES_PATH)
    cases = select_cases(payload)
    assert_gates(mode)
    knowledge_rows = flatten_knowledge()
    if mode == "plan":
        print(
            json.dumps(
                {
                    "mode": mode,
                    "sample_count": len(cases),
                    "sample_ids": [case["id"] for case in cases],
                    "eval_cases_path": str(EVAL_CASES_PATH),
                    "current_variant": CURRENT_VARIANT_LABEL,
                    "report_dir": str(REPORT_DIR),
                    "will_call_deepseek": False,
                    "will_call_music_or_image": False,
                },
                ensure_ascii=False,
                indent=2,
            )
        )
        return 0
    if mode == "preflight":
        print(
            json.dumps(
                {
                    "mode": mode,
                    "sample_count": len(cases),
                    "deepseek_key_present": bool(os.environ.get("DEEPSEEK_API_KEY", "").strip()),
                    "knowledge_rows": len(knowledge_rows),
                    "baseline_revision": BASELINE_REV,
                    "eval_cases_path": str(EVAL_CASES_PATH),
                    "current_variant": CURRENT_VARIANT_LABEL,
                    "reports_ignored_path": str(REPORT_DIR),
                },
                ensure_ascii=False,
                indent=2,
            )
        )
        return 0
    if mode != "execute_manual_review":
        raise RuntimeError("MODE must be plan, preflight, or execute_manual_review")
    results = run_execute(cases, knowledge_rows)
    print(
        json.dumps(
            {
                "mode": mode,
                "sample_count": len(results),
                "manual_review_md": str(MANUAL_REVIEW_MD),
                "manual_review_json": str(MANUAL_REVIEW_JSON),
                "latency_summary": latency_summary(results),
                "lyric_hashes": [
                    {
                        "case_id": item["case"]["id"],
                        "variants": [
                            {
                                "variant": variant["variant"],
                                "lyrics_hash": sha256(
                                    variant.get("lyrics_output", {}).get("lyrics_text", "")
                                ),
                            }
                            for variant in item["variants"]
                        ],
                    }
                    for item in results
                ],
            },
            ensure_ascii=False,
            indent=2,
        )
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
