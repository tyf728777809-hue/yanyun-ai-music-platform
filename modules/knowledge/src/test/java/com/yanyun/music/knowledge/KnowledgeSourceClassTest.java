package com.yanyun.music.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KnowledgeSourceClassTest {

  @Test
  void mapsFactLevelsToStableSourceClasses() {
    assertEquals(
        KnowledgeSourceClass.CONFIRMED_FACTS,
        KnowledgeSourceClass.fromFactLevel("official_public_seed"));
    assertEquals(
        KnowledgeSourceClass.CONFIRMED_FACTS,
        KnowledgeSourceClass.fromFactLevel("in_game_confirmed"));
    assertEquals(
        KnowledgeSourceClass.CREATIVE_MATERIALS,
        KnowledgeSourceClass.fromFactLevel("accepted_story_synthesis"));
    assertEquals(
        KnowledgeSourceClass.CREATIVE_MATERIALS,
        KnowledgeSourceClass.fromFactLevel("creative_guidance"));
    assertEquals(
        KnowledgeSourceClass.CREATIVE_MATERIALS,
        KnowledgeSourceClass.fromFactLevel("creative_boundary"));
    assertEquals(
        KnowledgeSourceClass.PENDING_CLUES,
        KnowledgeSourceClass.fromFactLevel("needs_ingame_recording"));
    assertEquals(KnowledgeSourceClass.PENDING_CLUES, KnowledgeSourceClass.fromFactLevel(null));
    assertEquals(
        KnowledgeSourceClass.PENDING_CLUES, KnowledgeSourceClass.fromFactLevel("unexpected"));
  }

  @Test
  void allSourceClassesArePromptInjectable() {
    assertTrue(KnowledgeSourceClass.CONFIRMED_FACTS.promptInjectable());
    assertTrue(KnowledgeSourceClass.CREATIVE_MATERIALS.promptInjectable());
    assertTrue(KnowledgeSourceClass.PENDING_CLUES.promptInjectable());
  }
}
