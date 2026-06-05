package com.yanyun.music.lyrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yanyun.music.deepseek.DeepSeekLyricsClient;
import com.yanyun.music.deepseek.DeepSeekLyricsResponse;
import com.yanyun.music.knowledge.KnowledgeReference;
import com.yanyun.music.knowledge.KnowledgeRetrievalRequest;
import com.yanyun.music.knowledge.KnowledgeRetrievalResult;
import com.yanyun.music.knowledge.KnowledgeService;
import com.yanyun.music.prompt.PromptRenderRequest;
import com.yanyun.music.prompt.PromptRenderResult;
import com.yanyun.music.prompt.PromptTemplateService;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DefaultLyricsGenerationServiceTest {

  @Test
  void generationCarriesKnowledgeAndPromptMetadata() {
    DeepSeekLyricsClient deepSeek =
        request ->
            new DeepSeekLyricsResponse(
                "Song",
                "Summary",
                "[Verse]\nLyrics",
                "cinematic folk",
                "cover seed",
                List.of("risk-note"),
                BigDecimal.valueOf(0.86));
    DefaultLyricsGenerationService service =
        new DefaultLyricsGenerationService(knowledgeService(), promptService(), deepSeek);

    LyricsGenerationResult result = service.generate(baseRequest(LyricsOperation.INSPIRATION));

    assertEquals("Song", result.songTitle());
    assertEquals("mock-kb-v1", result.knowledgeBaseVersion());
    assertEquals(List.of("Mock Yanyun Reference"), result.yanyunReferences());
    assertEquals(7, result.promptTemplateVersions().get("lyrics.inspiration.v1"));
    assertEquals(BigDecimal.valueOf(0.86), result.qualityScore());
  }

  @Test
  void lowQualityResponseTriggersOneRewrite() {
    AtomicInteger calls = new AtomicInteger();
    DeepSeekLyricsClient deepSeek =
        request -> {
          int call = calls.incrementAndGet();
          BigDecimal quality = call == 1 ? BigDecimal.valueOf(0.40) : BigDecimal.valueOf(0.91);
          return new DeepSeekLyricsResponse(
              "Song",
              "Summary",
              request.instruction(),
              "cinematic folk",
              "cover seed",
              List.of(),
              quality);
        };
    DefaultLyricsGenerationService service =
        new DefaultLyricsGenerationService(knowledgeService(), promptService(), deepSeek);

    LyricsGenerationResult result = service.generate(baseRequest(LyricsOperation.POLISH));

    assertEquals(2, calls.get());
    assertEquals(BigDecimal.valueOf(0.91), result.qualityScore());
    assertTrue(result.lyricsText().contains("Improve structure"));
  }

  private LyricsGenerationRequest baseRequest(LyricsOperation operation) {
    return new LyricsGenerationRequest(
        "user-1",
        "work-1",
        operation,
        "a vow under the moon",
        "[Verse]\nOld lyrics",
        "Make it warmer",
        "Requested",
        "folk pop",
        "female vocal");
  }

  private KnowledgeService knowledgeService() {
    return (KnowledgeRetrievalRequest request) ->
        new KnowledgeRetrievalResult(
            "mock-kb-v1",
            List.of(
                new KnowledgeReference(
                    "chunk-1",
                    "Mock Yanyun Reference",
                    "mock.md",
                    "world",
                    "Wind, moon, border drums.")));
  }

  private PromptTemplateService promptService() {
    return (PromptRenderRequest request) ->
        new PromptRenderResult(request.templateKey(), 7, "rendered prompt");
  }
}
