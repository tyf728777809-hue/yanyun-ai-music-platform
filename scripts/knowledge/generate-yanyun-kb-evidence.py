#!/usr/bin/env python3
import json
import re
from collections import Counter, defaultdict
from pathlib import Path

from yanyun_kb_loader import load_payload, summarize_payload


ROOT = Path(__file__).resolve().parents[2]
KB_DIR = ROOT / "knowledge-base/commercial-final"
INVENTORY_PATH = ROOT / "knowledge-base/commercial-final-gap-inventory.json"
PROVENANCE_PATH = ROOT / "knowledge-base/commercial-final-provenance-manifest.json"
POLLUTION_PATH = ROOT / "knowledge-base/commercial-final-pollution-audit.json"
REPORT_PATH = ROOT / "docs/knowledge/yanyun-knowledge-p5-evidence-v0.1.md"

OTHER_IP_TERMS = [
    "高达",
    "原神",
    "崩坏",
    "鸣潮",
    "王者荣耀",
    "逆水寒",
    "周杰伦",
    "林俊杰",
    "Taylor Swift",
]
ABSOLUTE_CLAIM_TERMS = ["官方定论", "已确认", "最终", "必然"]
SENSITIVE_PATTERNS = ["sk-", "Bearer ", "AccessKey", "SecretKey", "Authorization:", "X-Access-Token"]
FUTURE_CONTENT_TERMS = ["江南", "杭州", "未上线", "预告"]


def grouped_counts(items, key):
    counts = Counter(item.get(key, "<missing>") for item in items)
    return dict(sorted(counts.items(), key=lambda pair: str(pair[0])))


def collect_domain_payloads():
    manifest = json.loads((KB_DIR / "manifest.json").read_text(encoding="utf-8"))
    domains = {}
    for domain_file in manifest["domains"]:
        path = KB_DIR / domain_file
        domains[domain_file] = json.loads(path.read_text(encoding="utf-8"))
    return manifest, domains


def build_provenance(payload, domains):
    entity_by_key = {entity["key"]: entity for entity in payload["entities"]}
    chunks_by_entity = defaultdict(list)
    for chunk in payload["chunks"]:
        chunks_by_entity[chunk["entity_key"]].append(chunk["id"])

    entities = []
    for domain_file, domain_payload in domains.items():
        for entity in domain_payload.get("entities", []):
            entities.append(
                {
                    "key": entity["key"],
                    "domain_file": domain_file,
                    "entity_type": entity.get("entity_type"),
                    "canonical_name": entity.get("canonical_name"),
                    "source_type": entity.get("source_type"),
                    "source_ref": entity.get("source_ref"),
                    "fact_level": entity.get("fact_level"),
                    "spoiler_level": entity.get("spoiler_level"),
                    "chunk_count": len(entity.get("cards", [])),
                    "chunk_ids": chunks_by_entity.get(entity["key"], []),
                }
            )

    chunks = []
    for chunk in payload["chunks"]:
        entity = entity_by_key[chunk["entity_key"]]
        chunks.append(
            {
                "chunk_id": chunk["id"],
                "entity_key": chunk["entity_key"],
                "entity_name": entity.get("canonical_name"),
                "entity_type": entity.get("entity_type"),
                "fact_level": chunk.get("fact_level") or entity.get("fact_level"),
                "source_ref": chunk.get("source_ref") or entity.get("source_ref"),
                "story_phase": chunk.get("story_phase"),
                "theme_tags": chunk.get("theme_tags", []),
            }
        )

    return {
        "manifest_version": "yanyun-commercial-kb-provenance-v0.1",
        "generated_at": "2026-06-14T04:20:00+08:00",
        "kb_version": payload["kb_version"],
        "content_as_of": payload.get("content_as_of"),
        "summary": summarize_payload(payload),
        "source_type_counts": grouped_counts(payload["entities"], "source_type"),
        "fact_level_counts": grouped_counts(payload["entities"], "fact_level"),
        "entity_type_counts": grouped_counts(payload["entities"], "entity_type"),
        "entities": entities,
        "chunks": chunks,
    }


def scan_text(text, terms):
    return [term for term in terms if term in text]


def slugify_heading(value):
    text = value.strip().lower()
    text = re.sub(r"`([^`]*)`", r"\1", text)
    text = re.sub(r"[^\w\u4e00-\u9fff\s-]", "", text)
    text = re.sub(r"\s+", "-", text)
    text = re.sub(r"-+", "-", text)
    return text.strip("-")


def markdown_anchors(path):
    if not path.exists():
        return set()
    anchors = set()
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.startswith("#"):
            continue
        heading = line.lstrip("#").strip()
        if heading:
            anchors.add(slugify_heading(heading))
    return anchors


def source_ref_issue(source_ref):
    if not source_ref or source_ref.startswith(("http://", "https://")):
        return None
    path_part, _, anchor = source_ref.partition("#")
    if not path_part.startswith("docs/"):
        return None
    target = ROOT / path_part
    if not target.exists():
        return "source_ref_file_missing"
    if anchor and anchor not in markdown_anchors(target):
        return "source_ref_anchor_missing"
    return None


def build_pollution_audit(payload, inventory):
    entity_findings = []
    chunk_findings = []
    eval_findings = []
    sensitive_findings = []

    for entity in payload["entities"]:
        text = " ".join(
            [
                entity.get("canonical_name", ""),
                entity.get("summary", ""),
                " ".join(entity.get("aliases", [])),
            ]
        )
        other_ip_hits = scan_text(text, OTHER_IP_TERMS)
        if other_ip_hits and entity.get("entity_type") != "boundary":
            entity_findings.append(
                {
                    "severity": "P1",
                    "entity_key": entity["key"],
                    "issue": "non_boundary_other_ip_or_real_artist_term",
                    "terms": other_ip_hits,
                    "recommended_action": "确认是否应迁移到 creative-boundaries 或 eval case；正式创作资料不应混入其他 IP / 现实歌手名。",
                }
            )
        sensitive_hits = scan_text(text, SENSITIVE_PATTERNS)
        if sensitive_hits:
            sensitive_findings.append({"scope": "entity", "key": entity["key"], "terms": sensitive_hits})
        issue = source_ref_issue(entity.get("source_ref"))
        if issue:
            entity_findings.append(
                {
                    "severity": "P1",
                    "entity_key": entity["key"],
                    "issue": issue,
                    "source_ref": entity.get("source_ref"),
                    "recommended_action": "修复 source_ref 文件路径或 Markdown 锚点，保证来源可追溯。",
                }
            )

    for chunk in payload["chunks"]:
        prompt_text = " ".join(
            [
                chunk.get("summary_for_prompt", ""),
                chunk.get("emotional_arc", ""),
                chunk.get("content", ""),
            ]
        )
        fact_level = chunk.get("fact_level")
        if fact_level == "pending_clues":
            hits = scan_text(prompt_text, ABSOLUTE_CLAIM_TERMS)
            if hits:
                chunk_findings.append(
                    {
                        "severity": "P0",
                        "chunk_id": chunk["id"],
                        "issue": "pending_clue_uses_absolute_claim",
                        "terms": hits,
                        "recommended_action": "改为暗线、传闻、旧事、可能、未明等口径。",
                    }
                )
        source_issue = source_ref_issue(chunk.get("source_ref"))
        if source_issue:
            chunk_findings.append(
                {
                    "severity": "P1",
                    "chunk_id": chunk["id"],
                    "issue": source_issue,
                    "source_ref": chunk.get("source_ref"),
                    "recommended_action": "修复 chunk source_ref 文件路径或 Markdown 锚点。",
                }
            )
        if "。，" in prompt_text or "。。" in prompt_text or "，，" in prompt_text:
            chunk_findings.append(
                {
                    "severity": "P3",
                    "chunk_id": chunk["id"],
                    "issue": "bad_punctuation",
                    "recommended_action": "清理重复标点或坏句，避免污染 prompt 语感。",
                }
            )
        future_hits = scan_text(prompt_text, FUTURE_CONTENT_TERMS)
        if future_hits and chunk.get("story_phase") != "needs_recording":
            chunk_findings.append(
                {
                    "severity": "P2",
                    "chunk_id": chunk["id"],
                    "issue": "possible_future_or_preview_content_in_live_prompt",
                    "terms": future_hits,
                    "recommended_action": "确认是否已上线；未上线预告应移入 future/watchlist 或 pending_clues。",
                }
            )
        if not chunk.get("avoid_claims") or len(chunk.get("avoid_claims", [])) < 2:
            chunk_findings.append(
                {
                    "severity": "P1",
                    "chunk_id": chunk["id"],
                    "issue": "weak_avoid_claims",
                    "recommended_action": "补至少两个可执行禁写误区。",
                }
            )
        safety_text = " ".join([prompt_text, " ".join(chunk.get("avoid_claims", []))])
        sensitive_hits = scan_text(safety_text, SENSITIVE_PATTERNS)
        if sensitive_hits:
            sensitive_findings.append({"scope": "chunk", "key": chunk["id"], "terms": sensitive_hits})

    entity_keys = {entity["key"] for entity in payload["entities"]}
    for case in payload["eval_cases"]:
        missing = [key for key in case.get("expected_entities", []) if key not in entity_keys]
        if missing:
            eval_findings.append(
                {
                    "severity": "P0",
                    "eval_case": case["id"],
                    "issue": "missing_expected_entities",
                    "missing": missing,
                    "recommended_action": "修正 expected_entities 或补实体。",
                }
            )
        text = " ".join([case.get("input", ""), case.get("expected_behavior", "")])
        sensitive_hits = scan_text(text, SENSITIVE_PATTERNS)
        if sensitive_hits:
            sensitive_findings.append({"scope": "eval_case", "key": case["id"], "terms": sensitive_hits})

    inventory_remaining = [row for row in inventory["inventory"] if row.get("current_status") != "present"]
    if inventory_remaining:
        entity_findings.append(
            {
                "severity": "P1",
                "issue": "inventory_has_non_present_rows",
                "count": len(inventory_remaining),
                "recommended_action": "继续补库或明确降级为 future/watchlist。",
            }
        )

    return {
        "audit_version": "yanyun-commercial-kb-pollution-audit-v0.1",
        "generated_at": "2026-06-14T04:20:00+08:00",
        "kb_version": payload["kb_version"],
        "summary": {
            "entity_findings": len(entity_findings),
            "chunk_findings": len(chunk_findings),
            "eval_findings": len(eval_findings),
            "sensitive_findings": len(sensitive_findings),
            "inventory_remaining": len(inventory_remaining),
        },
        "entity_findings": entity_findings,
        "chunk_findings": chunk_findings,
        "eval_findings": eval_findings,
        "sensitive_findings": sensitive_findings,
    }


def select_eval_samples(payload, limit=24):
    samples = []
    for case in payload["eval_cases"]:
        expected = case.get("expected_entities", [])
        if expected:
            samples.append(case)
        if len(samples) >= limit:
            break
    return samples


def write_report(payload, inventory, provenance, audit):
    summary = summarize_payload(payload)
    inventory_rows = inventory["inventory"]
    inventory_remaining = sum(1 for row in inventory_rows if row.get("current_status") != "present")
    eval_samples = select_eval_samples(payload)

    lines = [
        "# 燕云创作知识库 P5 证据包 v0.1",
        "",
        "生成时间：2026-06-14",
        "",
        "## 总体结论",
        "",
        f"- 当前知识库版本：`{payload['kb_version']}`，内容边界截至 `{payload.get('content_as_of')}`。",
        f"- 正式库规模：`{summary['entities']}` 个实体、`{summary['chunks']}` 个创作 chunk、`{summary['eval_cases']}` 条 eval case。",
        f"- 缺口盘点表：`{len(inventory_rows)}` 行，剩余非 `present` 为 `{inventory_remaining}`。",
        f"- 污染审计：实体问题 `{audit['summary']['entity_findings']}`，chunk 问题 `{audit['summary']['chunk_findings']}`，eval 问题 `{audit['summary']['eval_findings']}`，敏感信息问题 `{audit['summary']['sensitive_findings']}`。",
        "- 本报告为离线证据包，不调用真实 DeepSeek、Yunwu、WellAPI、DreamMaker，不联网，不导入数据库。",
        "",
        "## 分域覆盖",
        "",
        "| 类型 | 数量 |",
        "| --- | ---: |",
    ]
    for entity_type, count in sorted(summary["entity_types"].items()):
        lines.append(f"| `{entity_type}` | {count} |")

    lines += [
        "",
        "## 来源与事实等级",
        "",
        "| source_type | 实体数 |",
        "| --- | ---: |",
    ]
    for source_type, count in provenance["source_type_counts"].items():
        lines.append(f"| `{source_type}` | {count} |")
    lines += [
        "",
        "| fact_level | 实体数 | 使用口径 |",
        "| --- | ---: | --- |",
    ]
    fact_notes = {
        "official_public_seed": "官方公开资料种子，可作为基础事实。",
        "accepted_story_synthesis": "攻略/维基/剧情整理综合，可服务创作，不对外宣称官方定论。",
        "creative_guidance": "创作指导，不是剧情事实。",
        "creative_boundary": "创作/审核/交付边界。",
        "needs_ingame_recording": "需游戏内实录补证。",
        "pending_clues": "待核线索，只能作暗线、传闻、旧事、情绪或意象。",
    }
    for fact_level, count in provenance["fact_level_counts"].items():
        lines.append(f"| `{fact_level}` | {count} | {fact_notes.get(fact_level, '')} |")

    lines += [
        "",
        "## A/B 评测方案",
        "",
        "本阶段不直接跑真实模型，先固定评测协议。后续执行时对同一批输入分别使用：",
        "",
        "- A 组：`KNOWLEDGE_RETRIEVAL_MODE=disabled`。",
        "- B 组：`KNOWLEDGE_RETRIEVAL_MODE=pgvector`，知识库版本为当前 `kb_version`。",
        "",
        "评分维度：",
        "",
        "| 维度 | 通过标准 |",
        "| --- | --- |",
        "| 世界归属感 | 歌词像发生在燕云大世界里，不靠硬塞“燕云/十六声”关键词。 |",
        "| 用户故事保真 | 玩家原始角色、情绪和故事没有被知识库强行改写。 |",
        "| 角色/剧情准确性 | 点名角色、地域、门派、剧情线时不写错核心经历和关系。 |",
        "| 可唱性 | 结构可唱，有副歌记忆点，不像百科或散文。 |",
        "| 泛古风污染 | 不堆砌红尘、宿命、刀光剑影等空泛词。 |",
        "| 边界安全 | 其他 IP、现实歌手仿唱、待核线索和商用发布边界处理正确。 |",
        "",
        "建议判定：B 组在世界归属感、角色/剧情准确性上应显著高于 A 组；用户故事保真和可唱性不能下降。",
        "",
        "## 抽样 Eval Case",
        "",
        "| id | 输入 | 期望实体 |",
        "| --- | --- | --- |",
    ]
    for case in eval_samples:
        expected = ", ".join(f"`{key}`" for key in case.get("expected_entities", []))
        user_input = case.get("input", "").replace("|", "\\|")
        if len(user_input) > 90:
            user_input = user_input[:89] + "…"
        lines.append(f"| `{case['id']}` | {user_input} | {expected} |")

    lines += [
        "",
        "## 污染审计结论",
        "",
    ]
    if all(value == 0 for value in audit["summary"].values()):
        lines.append("- 未发现阻塞级污染、缺失实体引用、敏感信息或未完成缺口。")
    else:
        lines.append("- 发现需跟进项，详见 `knowledge-base/commercial-final-pollution-audit.json`。")
    lines += [
        "",
        "## 交付文件",
        "",
        "- `knowledge-base/commercial-final-provenance-manifest.json`：chunk/entity 级来源与事实等级清单。",
        "- `knowledge-base/commercial-final-pollution-audit.json`：污染、缺失引用和敏感信息审计摘要。",
        "- `docs/knowledge/yanyun-knowledge-p5-evidence-v0.1.md`：本报告。",
        "",
        "## 剩余工作",
        "",
        "- 执行真实 A/B 样本生成与人工评分，确认 B 组实际歌词质量提升。",
        "- 公司提供官方资料包后，对 `accepted_story_synthesis` 和 `pending_clues` 逐条升降级。",
        "- 公司确认品牌禁区、剧透口径和社区发布审核规则。",
        "- 将 pgvector 导入真实本地数据库，跑检索召回延迟 P50/P95。",
    ]
    REPORT_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    payload = load_payload(KB_DIR)
    manifest, domains = collect_domain_payloads()
    inventory = json.loads(INVENTORY_PATH.read_text(encoding="utf-8"))
    provenance = build_provenance(payload, domains)
    audit = build_pollution_audit(payload, inventory)
    PROVENANCE_PATH.write_text(json.dumps(provenance, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    POLLUTION_PATH.write_text(json.dumps(audit, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    write_report(payload, inventory, provenance, audit)
    print(json.dumps({"summary": summarize_payload(payload), "audit": audit["summary"]}, ensure_ascii=False))
    return 1 if any(audit["summary"].values()) else 0


if __name__ == "__main__":
    raise SystemExit(main())
