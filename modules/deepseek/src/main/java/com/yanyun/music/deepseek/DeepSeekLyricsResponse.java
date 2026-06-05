package com.yanyun.music.deepseek;

import java.math.BigDecimal;
import java.util.List;

public record DeepSeekLyricsResponse(
    String songTitle,
    String songSummary,
    String lyricsText,
    String musicPrompt,
    String coverPromptSeed,
    List<String> riskNotes,
    BigDecimal qualityScore) {

  public DeepSeekLyricsResponse {
    riskNotes = riskNotes == null ? List.of() : List.copyOf(riskNotes);
  }
}
