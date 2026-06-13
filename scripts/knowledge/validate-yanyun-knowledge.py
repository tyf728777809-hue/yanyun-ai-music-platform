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
ALLOWED_SOURCE_TYPES = {
    "official_public",
    "official_public_curated",
    "guide_wiki_synthesis",
    "in_game_recording",
    "community",
}
ALLOWED_FACT_LEVELS = {
    "official_public_seed",
    "in_game_confirmed",
    "accepted_story_synthesis",
    "creative_guidance",
    "creative_boundary",
    "needs_ingame_recording",
    "pending_clues",
}
ALLOWED_STORY_PHASES = {
    "public_seed",
    "global_public_seed",
    "spoiler_synthesis",
    "creative_guidance",
    "always",
    "needs_recording",
}
SOURCE_FACT_LEVELS = {
    "official_public": {"official_public_seed"},
    "official_public_curated": {"creative_guidance", "creative_boundary", "needs_ingame_recording"},
    "guide_wiki_synthesis": {"accepted_story_synthesis", "pending_clues"},
    "in_game_recording": {"in_game_confirmed"},
    "community": {"accepted_story_synthesis", "creative_guidance", "pending_clues"},
}
FACT_STORY_PHASES = {
    "official_public_seed": {"public_seed", "global_public_seed"},
    "in_game_confirmed": {"public_seed", "global_public_seed", "spoiler_synthesis"},
    "accepted_story_synthesis": {"spoiler_synthesis"},
    "creative_guidance": {"creative_guidance", "always"},
    "creative_boundary": {"always"},
    "needs_ingame_recording": {"needs_recording"},
    "pending_clues": {"needs_recording"},
}
PENDING_FACT_LEVELS = {"needs_ingame_recording", "pending_clues"}
DETERMINISTIC_CLAIM_TERMS = ["已确认", "官方定论", "就是", "最终", "必然"]


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
      if entity.get("source_type") not in ALLOWED_SOURCE_TYPES:
          fail(errors, f"entity {key} has unsupported source_type: {entity.get('source_type')}")
      if entity.get("fact_level") not in ALLOWED_FACT_LEVELS:
          fail(errors, f"entity {key} has unsupported fact_level: {entity.get('fact_level')}")
      allowed_fact_levels = SOURCE_FACT_LEVELS.get(entity.get("source_type"), set())
      if allowed_fact_levels and entity.get("fact_level") not in allowed_fact_levels:
          fail(
              errors,
              f"entity {key} source_type {entity.get('source_type')} cannot use fact_level {entity.get('fact_level')}",
          )
      if entity.get("source_type") == "community" and entity.get("fact_level") not in (
          "accepted_story_synthesis",
          "creative_guidance",
          "pending_clues",
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
        entity = entity_keys.get(chunk.get("entity_key"), {})
        fact_level = chunk.get("fact_level") or entity.get("fact_level")
        story_phase = chunk.get("story_phase")
        if fact_level not in ALLOWED_FACT_LEVELS:
            fail(errors, f"chunk {chunk_label} has unsupported fact_level: {fact_level}")
        if story_phase not in ALLOWED_STORY_PHASES:
            fail(errors, f"chunk {chunk_label} has unsupported story_phase: {story_phase}")
        allowed_story_phases = FACT_STORY_PHASES.get(fact_level, set())
        if allowed_story_phases and story_phase not in allowed_story_phases:
            fail(errors, f"chunk {chunk_label} fact_level {fact_level} cannot use story_phase {story_phase}")
        if len(chunk.get("summary_for_prompt", "")) > 420:
            fail(errors, f"chunk {chunk_label} summary_for_prompt too long")
        if len(chunk.get("content", "")) > 240:
            warnings.append(f"chunk {chunk_label} content is long; keep prompt-facing content compressed")
        if len(chunk.get("avoid_claims", [])) < 2:
            fail(errors, f"chunk {chunk_label} needs at least 2 avoid_claims")
        if fact_level in PENDING_FACT_LEVELS:
            combined_text = " ".join(
                [
                    chunk.get("summary_for_prompt", ""),
                    chunk.get("emotional_arc", ""),
                    chunk.get("content", ""),
                ]
            )
            if any(term in combined_text for term in DETERMINISTIC_CLAIM_TERMS):
                fail(errors, f"chunk {chunk_label} pending clue uses deterministic claim wording")

    for eval_case in payload.get("eval_cases", []):
        missing = [
            entity_key
            for entity_key in eval_case.get("expected_entities", [])
            if entity_key not in entity_keys
        ]
        if missing:
            fail(errors, f"eval case {eval_case.get('id', '<unknown>')} references missing entities: {', '.join(missing)}")

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
