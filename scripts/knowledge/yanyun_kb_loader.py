import json
from pathlib import Path


def load_payload(input_path):
    path = Path(input_path)
    if path.is_dir():
        return load_domain_directory(path)
    return json.loads(path.read_text(encoding="utf-8"))


def load_domain_directory(directory):
    manifest_path = directory / "manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    payload = {
        "kb_version": manifest["kb_version"],
        "title": manifest["title"],
        "content_as_of": manifest["content_as_of"],
        "source_policy": manifest["source_policy"],
        "notes": manifest.get("notes", ""),
        "source_registry": manifest.get("source_registry", ""),
        "documents": list(manifest.get("documents", [])),
        "entities": [],
        "chunks": [],
        "eval_cases": [],
        "quality_targets": manifest.get("quality_targets", {}),
    }
    seen_documents = {document["id"] for document in payload["documents"]}
    for domain_file in manifest.get("domains", []):
        domain_path = directory / domain_file
        domain = json.loads(domain_path.read_text(encoding="utf-8"))
        for document in domain.get("documents", []):
            if document["id"] not in seen_documents:
                payload["documents"].append(document)
                seen_documents.add(document["id"])
        for entity in domain.get("entities", []):
            cards = entity.get("cards", [])
            flat_entity = {key: value for key, value in entity.items() if key != "cards"}
            payload["entities"].append(flat_entity)
            for card in cards:
                payload["chunks"].append(card_to_chunk(entity, card))
        payload["chunks"].extend(domain.get("chunks", []))
        payload["eval_cases"].extend(domain.get("eval_cases", []))
    return payload


def card_to_chunk(entity, card):
    card_id = card.get("id", "creative")
    return {
        "id": f"{entity['key']}-{card_id}",
        "document_id": card.get("document_id", entity.get("document_id", "commercial-final-seed")),
        "entity_key": entity["key"],
        "heading_path": card.get(
            "heading_path", f"{entity.get('entity_type', 'entity')}/{entity['canonical_name']}/{card_id}"
        ),
        "theme_tags": card.get("theme_tags", []),
        "story_phase": card.get("story_phase", entity.get("story_phase", "open")),
        "summary_for_prompt": card.get("summary_for_prompt", entity.get("summary", "")),
        "usable_imagery": card.get("usable_imagery", []),
        "emotional_arc": card.get("emotional_arc", ""),
        "avoid_claims": card.get("avoid_claims", []),
        "content": card.get("content", f"{entity['canonical_name']}创作资料卡。"),
        "fact_level": card.get("fact_level", entity.get("fact_level")),
        "source_ref": card.get("source_ref", entity.get("source_ref")),
    }


def summarize_payload(payload):
    entity_types = {}
    for entity in payload.get("entities", []):
        entity_type = entity.get("entity_type", "unknown")
        entity_types[entity_type] = entity_types.get(entity_type, 0) + 1
    return {
        "kb_version": payload.get("kb_version"),
        "documents": len(payload.get("documents", [])),
        "entities": len(payload.get("entities", [])),
        "chunks": len(payload.get("chunks", [])),
        "eval_cases": len(payload.get("eval_cases", [])),
        "entity_types": entity_types,
    }
