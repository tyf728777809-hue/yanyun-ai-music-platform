#!/usr/bin/env python3
import json
import math
import os
import shutil
import subprocess
import sys
import time
import unicodedata
import uuid
from datetime import datetime, timezone
from pathlib import Path

from yanyun_kb_loader import load_payload


ROOT = Path(__file__).resolve().parents[2]
KB_DIR = ROOT / "knowledge-base/commercial-final"
REPORT_PATH = ROOT / "docs/knowledge/yanyun-knowledge-ab-retrieval-v0.1.md"
RESULT_PATH = ROOT / "knowledge-base/commercial-final-ab-retrieval-result.json"
VECTOR_DIMENSIONS = 64
NAMESPACE = uuid.UUID("5b1a51f3-5932-4a39-8d0d-5e91f6c9e5d7")


def normalize(value):
    if value is None:
        return ""
    return "".join(unicodedata.normalize("NFKC", str(value)).lower().split())


def stable_uuid(*parts):
    return str(uuid.uuid5(NAMESPACE, "::".join(str(part) for part in parts)))


def embedding(text):
    vector = [0.0] * VECTOR_DIMENSIONS
    normalized = normalize(text)
    if not normalized:
        vector[0] = 1.0
        return vector
    for character in normalized:
        codepoint = ord(character)
        index = (codepoint * 31 + 17) % VECTOR_DIMENSIONS
        vector[index] += 1.0 + (codepoint % 7) * 0.03
    norm = math.sqrt(sum(value * value for value in vector))
    if norm <= 0:
        vector[0] = 1.0
        return vector
    return [value / norm for value in vector]


def vector_literal(values):
    return "[" + ",".join(f"{value:.8f}" for value in values) + "]"


def quote(value):
    if value is None:
        return "NULL"
    return "'" + str(value).replace("'", "''") + "'"


def psql_command():
    user = os.environ.get("POSTGRES_USER", "postgres")
    database = os.environ.get("POSTGRES_DB", "yanyun_music")
    if shutil.which("psql"):
        return [
            "psql",
            "-h",
            os.environ.get("POSTGRES_HOST", "localhost"),
            "-p",
            os.environ.get("POSTGRES_PORT", "5432"),
            "-U",
            user,
            "-d",
            database,
            "-q",
            "-t",
            "-A",
            "-v",
            "ON_ERROR_STOP=1",
        ]
    return [
        "docker",
        "exec",
        "-i",
        os.environ.get("POSTGRES_CONTAINER", "yanyun-postgres"),
        "psql",
        "-U",
        user,
        "-d",
        database,
        "-q",
        "-t",
        "-A",
        "-v",
        "ON_ERROR_STOP=1",
    ]


def query_json(sql):
    env = os.environ.copy()
    if os.environ.get("POSTGRES_PASSWORD"):
        env["PGPASSWORD"] = os.environ["POSTGRES_PASSWORD"]
    completed = subprocess.run(
        psql_command(),
        input=sql,
        text=True,
        env=env,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=True,
    )
    output = completed.stdout.strip()
    if not output:
        return []
    return json.loads(output)


def entity_id_map(payload):
    kb_version = payload["kb_version"]
    mapping = {}
    for entity in payload["entities"]:
        entity_id = stable_uuid(kb_version, "entity", entity["key"])
        mapping[entity_id] = entity["key"]
    return mapping


def retrieve_pgvector(payload, case_input, limit=6, entity_limit=4):
    kb_version = payload["kb_version"]
    normalized_query = normalize(case_input)
    entity_sql = f"""
    SELECT COALESCE(json_agg(row_to_json(t)), '[]'::json)
    FROM (
      SELECT e.id::text AS entity_id,
             e.canonical_name,
             e.entity_type,
             max(length(a.normalized_alias)) AS alias_length
      FROM knowledge_entity_aliases a
      JOIN knowledge_entities e ON e.id = a.entity_id
      WHERE e.kb_version = {quote(kb_version)}
        AND {quote(normalized_query)} LIKE '%' || a.normalized_alias || '%'
      GROUP BY e.id, e.canonical_name, e.entity_type
      ORDER BY alias_length DESC
      LIMIT {entity_limit}
    ) t;
    """
    entity_rows = query_json(entity_sql)
    references = []
    seen_chunk_ids = set()
    per_entity_limit = max(1, math.ceil(limit / len(entity_rows))) if entity_rows else limit
    for entity in entity_rows:
        if len(references) >= limit:
            break
        remaining = min(limit - len(references), per_entity_limit)
        chunk_sql = f"""
        SELECT COALESCE(json_agg(row_to_json(t)), '[]'::json)
        FROM (
          SELECT c.id::text AS chunk_id,
                 c.entity_id::text AS entity_id,
                 c.entity_name,
                 c.entity_type,
                 c.heading_path,
                 c.summary_for_prompt,
                 c.fact_level,
                 c.chunk_index
          FROM knowledge_chunks c
          WHERE c.kb_version = {quote(kb_version)}
            AND c.entity_id::text = {quote(entity["entity_id"])}
          ORDER BY c.chunk_index ASC
          LIMIT {remaining}
        ) t;
        """
        for row in query_json(chunk_sql):
            if row["chunk_id"] not in seen_chunk_ids:
                references.append(row)
                seen_chunk_ids.add(row["chunk_id"])

    remaining = limit - len(references)
    if remaining > 0:
        semantic_sql = f"""
        SELECT COALESCE(json_agg(row_to_json(t)), '[]'::json)
        FROM (
          SELECT c.id::text AS chunk_id,
                 c.entity_id::text AS entity_id,
                 c.entity_name,
                 c.entity_type,
                 c.heading_path,
                 c.summary_for_prompt,
                 c.fact_level,
                 c.chunk_index
          FROM knowledge_chunks c
          WHERE c.kb_version = {quote(kb_version)}
            AND c.embedding IS NOT NULL
          ORDER BY c.embedding <=> {quote(vector_literal(embedding(case_input)))}::vector
          LIMIT {remaining}
        ) t;
        """
        for row in query_json(semantic_sql):
            if row["chunk_id"] not in seen_chunk_ids:
                references.append(row)
                seen_chunk_ids.add(row["chunk_id"])
    return references[:limit]


def percentile(values, ratio):
    if not values:
        return 0
    ordered = sorted(values)
    index = min(len(ordered) - 1, max(0, math.ceil(len(ordered) * ratio) - 1))
    return ordered[index]


def selected_cases(payload):
    limit = int(os.environ.get("AB_SAMPLE_LIMIT", "48"))
    cases = [case for case in payload["eval_cases"] if case.get("expected_entities")]
    # Keep a broad but deterministic mix: early curated cases plus later gap-generated recall cases.
    if len(cases) <= limit:
        return cases
    head = cases[:24]
    tail = cases[24:]
    step = max(1, len(tail) // max(1, limit - len(head)))
    mixed = head + tail[::step]
    return mixed[:limit]


def run_ab():
    payload = load_payload(KB_DIR)
    id_to_key = entity_id_map(payload)
    cases = selected_cases(payload)
    results = []
    latencies = []
    for case in cases:
        started = time.perf_counter()
        refs = retrieve_pgvector(payload, case["input"])
        latency_ms = round((time.perf_counter() - started) * 1000, 2)
        latencies.append(latency_ms)
        retrieved_keys = [id_to_key.get(ref["entity_id"], "<unknown>") for ref in refs]
        expected = case.get("expected_entities", [])
        expected_set = set(expected)
        retrieved_set = set(retrieved_keys)
        hit_count = len(expected_set & retrieved_set)
        results.append(
            {
                "id": case["id"],
                "input": case["input"],
                "expected_entities": expected,
                "a_disabled": {"reference_count": 0, "retrieved_entities": []},
                "b_pgvector": {
                    "reference_count": len(refs),
                    "retrieved_entities": retrieved_keys,
                    "top_references": [
                        {
                            "entity_key": id_to_key.get(ref["entity_id"], "<unknown>"),
                            "entity_name": ref["entity_name"],
                            "heading_path": ref["heading_path"],
                            "fact_level": ref["fact_level"],
                        }
                        for ref in refs
                    ],
                    "latency_ms": latency_ms,
                    "hit_count": hit_count,
                    "any_hit": hit_count > 0,
                    "all_hit": expected_set.issubset(retrieved_set),
                },
            }
        )
    any_hit = sum(1 for result in results if result["b_pgvector"]["any_hit"])
    all_hit = sum(1 for result in results if result["b_pgvector"]["all_hit"])
    summary = {
        "ab_version": "yanyun-kb-retrieval-ab-v0.1",
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "kb_version": payload["kb_version"],
        "sample_count": len(results),
        "a_disabled_reference_count": 0,
        "b_pgvector_any_hit_rate": round(any_hit / len(results), 4) if results else 0,
        "b_pgvector_all_hit_rate": round(all_hit / len(results), 4) if results else 0,
        "b_pgvector_p50_latency_ms": percentile(latencies, 0.50),
        "b_pgvector_p95_latency_ms": percentile(latencies, 0.95),
        "b_pgvector_max_latency_ms": max(latencies) if latencies else 0,
        "target_p50_ms": 500,
        "target_p95_ms": 1500,
        "real_model_ab_executed": False,
        "real_model_ab_blocker": "DEEPSEEK_API_KEY is not present in current shell; retrieval/context A/B executed only.",
    }
    return {"summary": summary, "cases": results}


def write_report(result):
    summary = result["summary"]
    failed = [case for case in result["cases"] if not case["b_pgvector"]["any_hit"]]
    partial = [
        case
        for case in result["cases"]
        if case["b_pgvector"]["any_hit"] and not case["b_pgvector"]["all_hit"]
    ]
    lines = [
        "# 燕云创作知识库 A/B 检索评测 v0.1",
        "",
        "生成时间：2026-06-14",
        "",
        "## 结论",
        "",
        f"- 知识库版本：`{summary['kb_version']}`。",
        f"- 样本数：`{summary['sample_count']}`。",
        "- A 组：`KNOWLEDGE_RETRIEVAL_MODE=disabled`，知识上下文数量恒为 `0`。",
        "- B 组：`KNOWLEDGE_RETRIEVAL_MODE=pgvector`，从本地 PostgreSQL/pgvector 召回上下文。",
        f"- B 组 any-hit 召回率：`{summary['b_pgvector_any_hit_rate']}`。",
        f"- B 组 all-hit 召回率：`{summary['b_pgvector_all_hit_rate']}`。",
        f"- B 组延迟：P50 `{summary['b_pgvector_p50_latency_ms']}ms`，P95 `{summary['b_pgvector_p95_latency_ms']}ms`，max `{summary['b_pgvector_max_latency_ms']}ms`。",
        "- 本轮没有调用真实 DeepSeek；这是检索/上下文层 A/B，不是最终歌词质量 A/B。",
        "",
        "## 判定",
        "",
    ]
    if summary["b_pgvector_any_hit_rate"] >= 0.9 and summary["b_pgvector_p95_latency_ms"] < 1500:
        lines.append("- 通过：知识库已经足够进入真实模型 A/B。")
    else:
        lines.append("- 未完全通过：进入真实模型 A/B 前应先修召回失败样本。")
    lines += [
        "",
        "## 失败与部分命中",
        "",
        f"- any-hit 失败样本：`{len(failed)}`。",
        f"- 部分命中样本：`{len(partial)}`。",
        "",
        "| id | 输入 | 期望实体 | B 组召回实体 |",
        "| --- | --- | --- | --- |",
    ]
    for case in (failed + partial)[:30]:
        expected = ", ".join(f"`{key}`" for key in case["expected_entities"])
        retrieved = ", ".join(f"`{key}`" for key in case["b_pgvector"]["retrieved_entities"])
        user_input = case["input"].replace("|", "\\|")
        if len(user_input) > 90:
            user_input = user_input[:89] + "…"
        lines.append(f"| `{case['id']}` | {user_input} | {expected} | {retrieved} |")
    lines += [
        "",
        "## 后续真实模型 A/B",
        "",
        "当前 shell 没有 `DEEPSEEK_API_KEY`，所以本轮不执行真实写词 A/B。拿到可用 key 后，建议使用同一批样本：",
        "",
        "- A 组：禁用知识库生成歌词。",
        "- B 组：启用 `pgvector` 生成歌词。",
        "- 人工或 QualityEvaluationAgent 对世界归属感、用户故事保真、角色/剧情准确性、可唱性和泛古风污染打分。",
    ]
    REPORT_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    result = run_ab()
    RESULT_PATH.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    write_report(result)
    print(json.dumps(result["summary"], ensure_ascii=False))
    return 0 if result["summary"]["b_pgvector_any_hit_rate"] >= 0.9 else 1


if __name__ == "__main__":
    raise SystemExit(main())
