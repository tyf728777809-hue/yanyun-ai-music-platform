package com.yanyun.music.lyrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunStatus;
import com.yanyun.music.creativeagent.CreativeBriefRequest;
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
import java.util.ArrayList;
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
    assertEquals(1, result.promptTemplateVersions().get("creative.brief.v1"));
    assertEquals(BigDecimal.valueOf(0.86), result.qualityScore());
  }

  @Test
  void successfulGenerationRecordsCreativeBriefBeforeLyricsAgentRun() {
    List<AgentRunRecord> records = new ArrayList<>();
    DeepSeekLyricsClient deepSeek =
        request ->
            new DeepSeekLyricsResponse(
                "Song",
                "Summary",
                "[Verse]\nLyrics",
                "cinematic folk",
                "cover seed",
                List.of(),
                BigDecimal.valueOf(0.86));
    DefaultLyricsGenerationService service =
        new DefaultLyricsGenerationService(
            knowledgeService(), promptService(), deepSeek, records::add);

    service.generate(baseRequest(LyricsOperation.INSPIRATION));

    assertEquals(2, records.size());
    AgentRunRecord briefRecord = records.get(0);
    assertEquals("work-1", briefRecord.workId());
    assertEquals("CreativeBriefAgent", briefRecord.agentName());
    assertEquals("v0.1", briefRecord.agentVersion());
    assertEquals("INSPIRATION", briefRecord.operation());
    assertEquals("mock-creative-brief", briefRecord.modelName());
    assertEquals("creative.brief.v1", briefRecord.promptTemplateKey());
    assertEquals(1, briefRecord.promptTemplateVersion());
    assertEquals(AgentRunStatus.SUCCEEDED, briefRecord.status());
    assertNotNull(briefRecord.inputHash());
    assertNotNull(briefRecord.outputHash());
    assertTrue(briefRecord.latencyMs() >= 0);

    AgentRunRecord lyricsRecord = records.get(1);
    assertEquals("LyricsAgent", lyricsRecord.agentName());
    assertEquals("lyrics.inspiration.v1", lyricsRecord.promptTemplateKey());
    assertEquals(AgentRunStatus.SUCCEEDED, lyricsRecord.status());
  }

  @Test
  void creativeBriefIsPassedIntoPromptContext() {
    List<String> renderedInstructions = new ArrayList<>();
    DeepSeekLyricsClient deepSeek =
        request ->
            new DeepSeekLyricsResponse(
                "Song",
                "Summary",
                "[Verse]\nLyrics",
                "cinematic folk",
                "cover seed",
                List.of(),
                BigDecimal.valueOf(0.86));
    PromptTemplateService promptService =
        request -> {
          renderedInstructions.add(request.instruction());
          return new PromptRenderResult(request.templateKey(), 7, "rendered prompt");
        };
    DefaultLyricsGenerationService service =
        new DefaultLyricsGenerationService(
            knowledgeService(), promptService, deepSeek, new ArrayList<AgentRunRecord>()::add);

    service.generate(baseRequest(LyricsOperation.INSPIRATION));

    assertEquals(1, renderedInstructions.size());
    assertTrue(renderedInstructions.getFirst().contains("Creative brief:"));
    assertTrue(renderedInstructions.getFirst().contains("intent=Shape a song"));
    assertTrue(
        renderedInstructions.getFirst().contains("yanyun_references=[Mock Yanyun Reference]"));
  }

  @Test
  void lowQualityResponseTriggersOneRewrite() {
    AtomicInteger calls = new AtomicInteger();
    List<AgentRunRecord> records = new ArrayList<>();
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
        new DefaultLyricsGenerationService(
            knowledgeService(), promptService(), deepSeek, records::add);

    LyricsGenerationResult result = service.generate(baseRequest(LyricsOperation.POLISH));

    assertEquals(2, calls.get());
    assertEquals(3, records.size());
    assertEquals("CreativeBriefAgent", records.get(0).agentName());
    assertEquals("LyricsAgent", records.get(1).agentName());
    assertEquals("LyricsAgent", records.get(2).agentName());
    assertEquals(AgentRunStatus.SUCCEEDED, records.get(0).status());
    assertEquals(AgentRunStatus.SUCCEEDED, records.get(1).status());
    assertEquals(AgentRunStatus.SUCCEEDED, records.get(2).status());
    assertEquals(BigDecimal.valueOf(0.91), result.qualityScore());
    assertTrue(result.lyricsText().contains("Improve structure"));
  }

  @Test
  void failedGenerationRecordsSanitizedAgentRun() {
    List<AgentRunRecord> records = new ArrayList<>();
    DeepSeekLyricsClient deepSeek =
        request -> {
          throw new IllegalStateException("Bearer abc.def SecretKey=dummy-value");
        };
    DefaultLyricsGenerationService service =
        new DefaultLyricsGenerationService(
            knowledgeService(), promptService(), deepSeek, records::add);

    assertThrows(
        IllegalStateException.class, () -> service.generate(baseRequest(LyricsOperation.LYRICS)));

    assertEquals(2, records.size());
    assertEquals("CreativeBriefAgent", records.get(0).agentName());
    AgentRunRecord record = records.get(1);
    assertEquals(AgentRunStatus.FAILED, record.status());
    assertEquals("DEEPSEEK_LYRICS_FAILED", record.failureCode());
    assertTrue(record.failureMessage().contains("Bearer [REDACTED]"));
    assertTrue(record.failureMessage().contains("SecretKey=[REDACTED]"));
    assertFalse(record.failureMessage().contains("dummy-value"));
  }

  @Test
  void failedCreativeBriefRecordsFailureAndStopsBeforeDeepSeek() {
    List<AgentRunRecord> records = new ArrayList<>();
    AtomicInteger deepSeekCalls = new AtomicInteger();
    DefaultLyricsGenerationService service =
        new DefaultLyricsGenerationService(
            knowledgeService(),
            promptService(),
            (CreativeBriefRequest request) -> {
              records.add(
                  new AgentRunRecord(
                      request.workId(),
                      null,
                      "CreativeBriefAgent",
                      "v0.1",
                      request.operation(),
                      "mock-creative-brief",
                      "creative.brief.v1",
                      1,
                      "input-hash",
                      null,
                      AgentRunStatus.FAILED,
                      0,
                      null,
                      null,
                      null,
                      "CREATIVE_BRIEF_AGENT_FAILED",
                      "brief failed"));
              throw new IllegalArgumentException("brief failed");
            },
            request -> {
              deepSeekCalls.incrementAndGet();
              return new DeepSeekLyricsResponse(
                  "Song",
                  "Summary",
                  "[Verse]\nLyrics",
                  "cinematic folk",
                  "cover seed",
                  List.of(),
                  BigDecimal.valueOf(0.86));
            },
            records::add);

    assertThrows(
        IllegalArgumentException.class,
        () -> service.generate(baseRequest(LyricsOperation.INSPIRATION)));

    assertEquals(0, deepSeekCalls.get());
    assertEquals(1, records.size());
    AgentRunRecord record = records.getFirst();
    assertEquals("CreativeBriefAgent", record.agentName());
    assertEquals(AgentRunStatus.FAILED, record.status());
    assertEquals("CREATIVE_BRIEF_AGENT_FAILED", record.failureCode());
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
