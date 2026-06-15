#!/usr/bin/env python3
import hashlib
import json
import math
import os
import shutil
import subprocess
import sys
import unicodedata
import uuid
from pathlib import Path

from yanyun_kb_loader import load_payload, summarize_payload


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


def quote_json(value):
    return quote(json.dumps(value if value is not None else [], ensure_ascii=False, sort_keys=True))


def date_or_null(value):
    return quote(value) if value else "NULL"


def content_hash(value):
    return hashlib.sha256(json.dumps(value, ensure_ascii=False, sort_keys=True).encode("utf-8")).hexdigest()


def sql_for_payload(payload):
    kb_version = payload["kb_version"]
    sql = [
        "BEGIN;",
        "CREATE EXTENSION IF NOT EXISTS vector;",
        f"DELETE FROM knowledge_chunks WHERE kb_version = {quote(kb_version)};",
        f"DELETE FROM knowledge_documents WHERE kb_version = {quote(kb_version)};",
        f"DELETE FROM knowledge_entities WHERE kb_version = {quote(kb_version)};",
        (
            "INSERT INTO knowledge_kb_versions "
            "(kb_version, title, content_as_of, source_policy, notes) VALUES "
            f"({quote(kb_version)}, {quote(payload.get('title'))}, "
            f"{quote(payload.get('content_as_of'))}, {quote(payload.get('source_policy'))}, "
            f"{quote(payload.get('notes'))}) "
            "ON CONFLICT (kb_version) DO UPDATE SET "
            "title = EXCLUDED.title, content_as_of = EXCLUDED.content_as_of, "
            "source_policy = EXCLUDED.source_policy, notes = EXCLUDED.notes;"
        ),
    ]
    document_ids = {}
    for document in payload.get("documents", []):
        document_key = document["id"]
        document_id = stable_uuid(kb_version, "document", document_key)
        document_ids[document_key] = document_id
        metadata = {key: value for key, value in document.items() if key not in {"id"}}
        sql.append(
            "INSERT INTO knowledge_documents "
            "(id, kb_version, file_path, title, content_hash, metadata_json, source_type, "
            "source_ref, source_updated_at, content_as_of, verified_by) VALUES "
            f"({quote(document_id)}, {quote(kb_version)}, {quote(document.get('file_path'))}, "
            f"{quote(document.get('title'))}, {quote(content_hash(document))}, "
            f"{quote_json(metadata)}::jsonb, {quote(document.get('source_type'))}, "
            f"{quote(document.get('source_ref'))}, {date_or_null(document.get('source_updated_at'))}, "
            f"{quote(payload.get('content_as_of'))}, {quote(document.get('verified_by'))}) "
            "ON CONFLICT (id) DO UPDATE SET "
            "title = EXCLUDED.title, content_hash = EXCLUDED.content_hash, "
            "metadata_json = EXCLUDED.metadata_json, source_type = EXCLUDED.source_type, "
            "source_ref = EXCLUDED.source_ref, source_updated_at = EXCLUDED.source_updated_at, "
            "content_as_of = EXCLUDED.content_as_of, verified_by = EXCLUDED.verified_by;"
        )

    entity_ids = {}
    for entity in payload.get("entities", []):
        entity_key = entity["key"]
        entity_id = stable_uuid(kb_version, "entity", entity_key)
        entity_ids[entity_key] = entity_id
        sql.append(
            "INSERT INTO knowledge_entities "
            "(id, kb_version, entity_type, canonical_name, summary, source_type, source_ref, "
            "fact_level, spoiler_level) VALUES "
            f"({quote(entity_id)}, {quote(kb_version)}, {quote(entity.get('entity_type'))}, "
            f"{quote(entity.get('canonical_name'))}, {quote(entity.get('summary'))}, "
            f"{quote(entity.get('source_type'))}, {quote(entity.get('source_ref'))}, "
            f"{quote(entity.get('fact_level'))}, {quote(entity.get('spoiler_level'))}) "
            "ON CONFLICT (kb_version, entity_type, canonical_name) DO UPDATE SET "
            "summary = EXCLUDED.summary, source_type = EXCLUDED.source_type, "
            "source_ref = EXCLUDED.source_ref, fact_level = EXCLUDED.fact_level, "
            "spoiler_level = EXCLUDED.spoiler_level;"
        )
        aliases = entity.get("aliases") or [entity.get("canonical_name")]
        for alias in aliases:
            normalized_alias = normalize(alias)
            if not normalized_alias:
                continue
            alias_id = stable_uuid(kb_version, "alias", entity_key, normalized_alias)
            sql.append(
                "INSERT INTO knowledge_entity_aliases "
                "(id, entity_id, alias, normalized_alias) VALUES "
                f"({quote(alias_id)}, {quote(entity_id)}, {quote(alias)}, {quote(normalized_alias)}) "
                "ON CONFLICT (entity_id, normalized_alias) DO UPDATE SET alias = EXCLUDED.alias;"
            )

    for index, chunk in enumerate(payload.get("chunks", [])):
        document_id = document_ids[chunk.get("document_id")]
        entity_id = entity_ids.get(chunk.get("entity_key"))
        entity = next((item for item in payload.get("entities", []) if item.get("key") == chunk.get("entity_key")), {})
        chunk_fact_level = chunk.get("fact_level") or entity.get("fact_level")
        chunk_source_ref = chunk.get("source_ref") or entity.get("source_ref")
        chunk_id = stable_uuid(kb_version, "chunk", chunk.get("id", index))
        text_for_embedding = "\n".join(
            str(chunk.get(field, ""))
            for field in ["summary_for_prompt", "emotional_arc", "content", "heading_path"]
        )
        token_count = max(1, len(text_for_embedding) // 2)
        sql.append(
            "INSERT INTO knowledge_chunks "
            "(id, document_id, kb_version, chunk_index, heading_path, content, tags_json, token_count, "
            "entity_id, entity_type, entity_name, aliases_json, theme_tags_json, fact_level, "
            "spoiler_level, story_phase, summary_for_prompt, usable_imagery_json, emotional_arc, "
            "avoid_claims_json, source_ref, embedding) VALUES "
            f"({quote(chunk_id)}, {quote(document_id)}, {quote(kb_version)}, {index}, "
            f"{quote(chunk.get('heading_path'))}, {quote(chunk.get('content'))}, "
            f"{quote_json(chunk.get('theme_tags'))}::jsonb, {token_count}, "
            f"{quote(entity_id)}, {quote(entity.get('entity_type'))}, {quote(entity.get('canonical_name'))}, "
            f"{quote_json(entity.get('aliases'))}::jsonb, {quote_json(chunk.get('theme_tags'))}::jsonb, "
            f"{quote(chunk_fact_level)}, {quote(entity.get('spoiler_level'))}, "
            f"{quote(chunk.get('story_phase'))}, {quote(chunk.get('summary_for_prompt'))}, "
            f"{quote_json(chunk.get('usable_imagery'))}::jsonb, {quote(chunk.get('emotional_arc'))}, "
            f"{quote_json(chunk.get('avoid_claims'))}::jsonb, {quote(chunk_source_ref)}, "
            f"{quote(vector_literal(embedding(text_for_embedding)))}::vector) "
            "ON CONFLICT (document_id, chunk_index) DO UPDATE SET "
            "heading_path = EXCLUDED.heading_path, content = EXCLUDED.content, "
            "tags_json = EXCLUDED.tags_json, token_count = EXCLUDED.token_count, "
            "entity_id = EXCLUDED.entity_id, entity_type = EXCLUDED.entity_type, "
            "entity_name = EXCLUDED.entity_name, aliases_json = EXCLUDED.aliases_json, "
            "theme_tags_json = EXCLUDED.theme_tags_json, fact_level = EXCLUDED.fact_level, "
            "spoiler_level = EXCLUDED.spoiler_level, story_phase = EXCLUDED.story_phase, "
            "summary_for_prompt = EXCLUDED.summary_for_prompt, "
            "usable_imagery_json = EXCLUDED.usable_imagery_json, "
            "emotional_arc = EXCLUDED.emotional_arc, avoid_claims_json = EXCLUDED.avoid_claims_json, "
            "source_ref = EXCLUDED.source_ref, embedding = EXCLUDED.embedding;"
        )
    sql.append("COMMIT;")
    return "\n".join(sql) + "\n"


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
        "-v",
        "ON_ERROR_STOP=1",
    ]


def main():
    input_path = Path(sys.argv[1] if len(sys.argv) > 1 else "knowledge-base/commercial-final")
    payload = load_payload(input_path)
    if os.environ.get("DRY_RUN_SQL") == "1":
        print(sql_for_payload(payload))
        return 0
    if os.environ.get("DRY_RUN") == "1":
        summary = summarize_payload(payload)
        summary["input_path"] = str(input_path)
        print(json.dumps(summary, ensure_ascii=False, indent=2, sort_keys=True))
        return 0

    sql = sql_for_payload(payload)
    env = os.environ.copy()
    if os.environ.get("POSTGRES_PASSWORD"):
        env["PGPASSWORD"] = os.environ["POSTGRES_PASSWORD"]
    subprocess.run(psql_command(), input=sql, text=True, env=env, check=True)
    print(f"Imported {payload['kb_version']} from {input_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
