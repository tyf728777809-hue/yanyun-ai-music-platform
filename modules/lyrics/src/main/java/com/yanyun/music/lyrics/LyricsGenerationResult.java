package com.yanyun.music.lyrics;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record LyricsGenerationResult(
    String songTitle,
    String songSummary,
    String lyricsText,
    String musicPrompt,
    String coverPromptSeed,
    List<String> riskNotes,
    List<String> yanyunReferences,
    String knowledgeBaseVersion,
    Map<String, Integer> promptTemplateVersions,
    BigDecimal qualityScore) {

  public LyricsGenerationResult {
    riskNotes = riskNotes == null ? List.of() : List.copyOf(riskNotes);
    yanyunReferences = yanyunReferences == null ? List.of() : List.copyOf(yanyunReferences);
    promptTemplateVersions =
        promptTemplateVersions == null ? Map.of() : Map.copyOf(promptTemplateVersions);
  }
}
