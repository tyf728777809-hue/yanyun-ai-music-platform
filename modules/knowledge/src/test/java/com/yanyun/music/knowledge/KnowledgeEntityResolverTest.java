package com.yanyun.music.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class KnowledgeEntityResolverTest {

  private final KnowledgeEntityResolver resolver = new KnowledgeEntityResolver();

  @Test
  void fuzzyMatchesCommonCharacterTypo() {
    List<ResolvedKnowledgeEntity> result =
        resolver.resolve(
            "我要以燕云十六声中姜无浪的人生经历创作一首歌",
            List.of(),
            List.of(alias("character-jiangwulang", "江无浪", "character", "江无浪")),
            3);

    assertEquals(1, result.size());
    assertEquals("江无浪", result.getFirst().canonicalName());
    assertEquals(KnowledgeEntityMatchKind.FUZZY, result.getFirst().matchKind());
    assertFalse(result.getFirst().ambiguous());
    assertTrue(result.getFirst().usableForRetrieval());
  }

  @Test
  void fuzzyMatchesPlaceVariantWithoutForcingShortSingleCharacterInput() {
    List<KnowledgeAliasCandidate> aliases =
        List.of(
            alias("place-ruoshui-an", "弱水岸", "place", "弱水岸"),
            alias("character-xunxin", "寻心", "character", "寻心"));

    List<ResolvedKnowledgeEntity> placeResult = resolver.resolve("若水岸古井旧事", List.of(), aliases, 3);
    assertEquals("弱水岸", placeResult.getFirst().canonicalName());

    List<ResolvedKnowledgeEntity> shortResult = resolver.resolve("寻", List.of(), aliases, 3);
    assertTrue(shortResult.isEmpty());
  }

  @Test
  void doesNotFuzzyMatchGameplayConceptsToWorldTerms() {
    List<KnowledgeAliasCandidate> aliases =
        List.of(alias("gameplay-xunsheng", "寻声", "gameplay", "十六声"));

    List<ResolvedKnowledgeEntity> result =
        resolver.resolve("琴里藏着十六州每一座城的旧调", List.of(), aliases, 3);

    assertTrue(result.isEmpty());
  }

  @Test
  void preservesDirectGameplayMatchesWithoutFuzzyCorrection() {
    ResolvedKnowledgeEntity direct =
        new ResolvedKnowledgeEntity(
            "gameplay-xunsheng",
            "寻声",
            "gameplay",
            "十六声",
            KnowledgeEntityMatchKind.ALIAS,
            1.0d,
            false);

    List<ResolvedKnowledgeEntity> result =
        resolver.resolve(
            "燕云十六声里的寻声玩法",
            List.of(direct),
            List.of(alias("gameplay-xunsheng", "寻声", "gameplay", "十六声")),
            3);

    assertEquals(1, result.size());
    assertEquals(KnowledgeEntityMatchKind.ALIAS, result.getFirst().matchKind());
    assertEquals("十六声", result.getFirst().matchedText());
  }

  @Test
  void marksEquallyLikelyFuzzyMatchesAsAmbiguous() {
    List<KnowledgeAliasCandidate> aliases =
        List.of(
            alias("character-a", "江无浪", "character", "江无浪"),
            alias("character-b", "姜无量", "character", "姜无量"));

    List<ResolvedKnowledgeEntity> result = resolver.resolve("姜无浪", List.of(), aliases, 3);

    assertEquals(2, result.size());
    assertTrue(result.stream().allMatch(ResolvedKnowledgeEntity::ambiguous));
    assertTrue(result.stream().noneMatch(ResolvedKnowledgeEntity::usableForRetrieval));
  }

  private KnowledgeAliasCandidate alias(
      String entityId, String canonicalName, String category, String alias) {
    return new KnowledgeAliasCandidate(
        entityId,
        canonicalName,
        category,
        alias,
        DeterministicKnowledgeEmbeddingService.normalize(alias));
  }
}
