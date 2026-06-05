package com.yanyun.music.deepseek;

import java.math.BigDecimal;
import java.util.List;

public final class MockDeepSeekLyricsClient implements DeepSeekLyricsClient {

  @Override
  public DeepSeekLyricsResponse generate(DeepSeekLyricsRequest request) {
    String title = firstNonBlank(request.requestedTitle(), "Yanyun Mock Song");
    String summary = summaryFor(request);
    String lyrics = lyricsFor(request);
    String musicPrompt = musicPromptFor(request);
    return new DeepSeekLyricsResponse(
        title,
        summary,
        lyrics,
        musicPrompt,
        "warm cinematic yanyun cover seed",
        List.of(),
        BigDecimal.valueOf(0.86));
  }

  private String lyricsFor(DeepSeekLyricsRequest request) {
    String operation = request.operation() == null ? "" : request.operation();
    return switch (operation) {
      case "LYRICS" -> firstNonBlank(request.userInput(), "[Verse]\nMock lyrics");
      case "POLISH" ->
          firstNonBlank(request.currentLyrics(), "[Verse]\nMock lyrics")
              + "\n\n[Polished]\n"
              + firstNonBlank(request.instruction(), "Refine phrasing and imagery.");
      case "CONTINUE" ->
          firstNonBlank(request.currentLyrics(), "[Verse]\nMock lyrics")
              + "\n\n[Bridge]\n"
              + firstNonBlank(request.instruction(), "Continue with a stronger emotional turn.");
      default ->
          "[Verse]\n"
              + firstNonBlank(request.userInput(), "Yanyun story begins")
              + "\n[Chorus]\nA yanyun melody rises under the moon.";
    };
  }

  private String summaryFor(DeepSeekLyricsRequest request) {
    String seed =
        firstNonBlank(
            request.userInput(),
            firstNonBlank(request.instruction(), firstNonBlank(request.currentLyrics(), "Yanyun")));
    return "Lyrics draft shaped from " + trimToLength(seed, 80) + ".";
  }

  private String musicPromptFor(DeepSeekLyricsRequest request) {
    String style = firstNonBlank(request.musicStyle(), "ancient chinese folk pop");
    String vocal = firstNonBlank(request.vocalPreference(), "auto vocal");
    return style + ", cinematic, yanyun-inspired, " + vocal;
  }

  private String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private String trimToLength(String value, int maxLength) {
    String trimmed = value == null ? "" : value.trim();
    return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
  }
}
