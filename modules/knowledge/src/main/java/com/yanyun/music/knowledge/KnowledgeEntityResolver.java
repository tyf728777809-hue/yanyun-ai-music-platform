package com.yanyun.music.knowledge;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class KnowledgeEntityResolver {

  public List<ResolvedKnowledgeEntity> resolve(
      String normalizedQuery,
      List<ResolvedKnowledgeEntity> directMatches,
      List<KnowledgeAliasCandidate> aliases,
      int limit) {
    if (normalizedQuery == null || normalizedQuery.isBlank() || limit <= 0) {
      return List.of();
    }
    Map<String, ResolvedKnowledgeEntity> resolved = new LinkedHashMap<>();
    for (ResolvedKnowledgeEntity entity : safeList(directMatches)) {
      resolved.putIfAbsent(entity.entityId(), entity);
    }
    if (normalizedQuery.length() < 3 || resolved.size() >= limit) {
      return resolved.values().stream().limit(limit).toList();
    }
    List<FuzzyMatch> matches =
        safeList(aliases).stream()
            .filter(alias -> !resolved.containsKey(alias.entityId()))
            .map(alias -> bestFuzzyMatch(normalizedQuery, alias))
            .filter(Objects::nonNull)
            .sorted(
                Comparator.comparingInt(FuzzyMatch::distance)
                    .thenComparing(Comparator.comparingDouble(FuzzyMatch::confidence).reversed())
                    .thenComparing(match -> -match.alias().normalizedAlias().length()))
            .limit(Math.max(0, limit - resolved.size()))
            .toList();
    for (FuzzyMatch match : matches) {
      ResolvedKnowledgeEntity entity = match.toResolvedEntity(hasAmbiguousPeer(match, matches));
      resolved.putIfAbsent(entity.entityId(), entity);
    }
    return resolved.values().stream().limit(limit).toList();
  }

  private <T> List<T> safeList(List<T> values) {
    return values == null ? List.of() : values;
  }

  private FuzzyMatch bestFuzzyMatch(String normalizedQuery, KnowledgeAliasCandidate alias) {
    String normalizedAlias = alias.normalizedAlias();
    if (normalizedAlias == null
        || normalizedAlias.length() < 3
        || normalizedQuery.contains(normalizedAlias)) {
      return null;
    }
    int bestDistance = Integer.MAX_VALUE;
    String bestWindow = "";
    int minSize = Math.max(3, normalizedAlias.length() - 1);
    int maxSize = Math.min(normalizedQuery.length(), normalizedAlias.length() + 1);
    for (int size = minSize; size <= maxSize; size++) {
      for (int start = 0; start <= normalizedQuery.length() - size; start++) {
        String window = normalizedQuery.substring(start, start + size);
        int distance = levenshtein(normalizedAlias, window);
        if (distance < bestDistance) {
          bestDistance = distance;
          bestWindow = window;
        }
      }
    }
    int allowedDistance = normalizedAlias.length() >= 5 ? 2 : 1;
    if (bestDistance <= 0 || bestDistance > allowedDistance) {
      return null;
    }
    double confidence = bestDistance == 1 ? 0.92d : 0.86d;
    return confidence >= 0.85d ? new FuzzyMatch(alias, bestWindow, bestDistance, confidence) : null;
  }

  private int levenshtein(String left, String right) {
    int[] previous = new int[right.length() + 1];
    int[] current = new int[right.length() + 1];
    for (int j = 0; j <= right.length(); j++) {
      previous[j] = j;
    }
    for (int i = 1; i <= left.length(); i++) {
      current[0] = i;
      for (int j = 1; j <= right.length(); j++) {
        int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
        current[j] =
            Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
      }
      int[] temp = previous;
      previous = current;
      current = temp;
    }
    return previous[right.length()];
  }

  private boolean hasAmbiguousPeer(FuzzyMatch target, List<FuzzyMatch> matches) {
    return matches.stream()
        .anyMatch(
            candidate ->
                candidate != target
                    && candidate.distance() == target.distance()
                    && Double.compare(candidate.confidence(), target.confidence()) == 0
                    && Objects.equals(candidate.matchedText(), target.matchedText())
                    && !Objects.equals(candidate.alias().entityId(), target.alias().entityId()));
  }

  private record FuzzyMatch(
      KnowledgeAliasCandidate alias, String matchedText, int distance, double confidence) {

    private ResolvedKnowledgeEntity toResolvedEntity(boolean ambiguous) {
      return new ResolvedKnowledgeEntity(
          alias.entityId(),
          alias.canonicalName(),
          alias.category(),
          matchedText,
          KnowledgeEntityMatchKind.FUZZY,
          confidence,
          ambiguous);
    }
  }
}
