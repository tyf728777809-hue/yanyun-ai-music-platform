package com.yanyun.music.knowledge;

public record KnowledgeAliasCandidate(
    String entityId, String canonicalName, String category, String alias, String normalizedAlias) {}
