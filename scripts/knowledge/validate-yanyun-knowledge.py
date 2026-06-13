#!/usr/bin/env python3
import os
import sys
import unicodedata
from pathlib import Path

from yanyun_kb_loader import load_payload, summarize_payload


REQUIRED_ENTITY_FIELDS = [
    "key",
    "entity_type",
    "canonical_name",
    "aliases",
    "summary",
    "source_type",
    "source_ref",
    "fact_level",
    "spoiler_level",
]
REQUIRED_CHUNK_FIELDS = [
    "id",
    "document_id",
    "entity_key",
    "heading_path",
    "theme_tags",
    "story_phase",
    "summary_for_prompt",
    "usable_imagery",
    "emotional_arc",
    "avoid_claims",
    "content",
]


def normalize(value):
    return "".join(unicodedata.normalize("NFKC", str(value)).lower().split())


def fail(errors, message):
    errors.append(message)


def validate(payload, strict=False):
    errors = []
    warnings = []
    documents = {document["id"]: document for document in payload.get("documents", [])}
    entities = payload.get("entities", [])
    chunks = payload.get("chunks", [])
    entity_keys = {}
    alias_owner = {}
    chunk_ids = set()

    if not payload.get("kb_version"):
        fail(errors, "missing kb_version")
    if not documents:
        fail(errors, "missing documents")

    for entity in entities:
      missing = [field for field in REQUIRED_ENTITY_FIELDS if field not in entity or entity[field] in (None, "", [])]
      if missing:
          fail(errors, f"entity {entity.get('key', '<unknown>')} missing fields: {', '.join(missing)}")
      key = entity.get("key")
      if key in entity_keys:
          fail(errors, f"duplicate entity key: {key}")
      entity_keys[key] = entity
      if entity.get("source_type") == "raw_community_thread":
          fail(errors, f"raw community thread cannot enter knowledge base directly: {key}")
      if entity.get("source_type") == "community" and entity.get("fact_level") not in (
          "accepted_story_synthesis",
          "creative_guidance",
      ):
          fail(errors, f"community source needs synthesis fact_level before import: {key}")
      aliases = entity.get("aliases", [])
      if entity.get("canonical_name") and entity.get("canonical_name") not in aliases:
          warnings.append(f"canonical name not listed as alias: {key}")
      for alias in aliases:
          normalized = normalize(alias)
          if not normalized:
              fail(errors, f"blank alias on entity: {key}")
          previous = alias_owner.get(normalized)
          if previous and previous != key:
              fail(errors, f"alias conflict '{alias}': {previous} vs {key}")
          alias_owner[normalized] = key

    for index, chunk in enumerate(chunks):
        missing = [field for field in REQUIRED_CHUNK_FIELDS if field not in chunk or chunk[field] in (None, "", [])]
        chunk_label = chunk.get("id", f"#{index}")
        if missing:
            fail(errors, f"chunk {chunk_label} missing fields: {', '.join(missing)}")
        if chunk.get("id") in chunk_ids:
            fail(errors, f"duplicate chunk id: {chunk.get('id')}")
        chunk_ids.add(chunk.get("id"))
        if chunk.get("document_id") not in documents:
            fail(errors, f"chunk {chunk_label} references missing document: {chunk.get('document_id')}")
        if chunk.get("entity_key") not in entity_keys:
            fail(errors, f"chunk {chunk_label} references missing entity: {chunk.get('entity_key')}")
        if len(chunk.get("summary_for_prompt", "")) > 420:
            fail(errors, f"chunk {chunk_label} summary_for_prompt too long")
        if len(chunk.get("content", "")) > 240:
            warnings.append(f"chunk {chunk_label} content is long; keep prompt-facing content compressed")
        if len(chunk.get("avoid_claims", [])) < 2:
            fail(errors, f"chunk {chunk_label} needs at least 2 avoid_claims")

    if strict:
        targets = payload.get("quality_targets", {})
        summary = summarize_payload(payload)
        for key in ["entities", "chunks", "eval_cases"]:
            minimum = int(targets.get(f"min_{key}", 0))
            if minimum and summary.get(key, 0) < minimum:
                fail(errors, f"{key} below commercial target: {summary.get(key, 0)} < {minimum}")
        min_types = targets.get("min_entity_types", {})
        for entity_type, minimum in min_types.items():
            actual = summary["entity_types"].get(entity_type, 0)
            if actual < int(minimum):
                fail(errors, f"entity type {entity_type} below target: {actual} < {minimum}")

    return errors, warnings


def main():
    input_path = Path(sys.argv[1] if len(sys.argv) > 1 else "knowledge-base/commercial-final")
    strict = os.environ.get("STRICT_COMMERCIAL") == "1"
    payload = load_payload(input_path)
    errors, warnings = validate(payload, strict=strict)
    summary = summarize_payload(payload)
    print(summary)
    for warning in warnings:
        print(f"WARN: {warning}")
    for error in errors:
        print(f"ERROR: {error}")
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
