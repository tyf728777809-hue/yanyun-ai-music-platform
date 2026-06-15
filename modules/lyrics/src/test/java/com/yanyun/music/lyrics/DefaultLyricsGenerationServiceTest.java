package com.yanyun.music.lyrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunStatus;
import com.yanyun.music.creativeagent.CreativeBriefRequest;
import com.yanyun.music.creativeagent.MockCreativeBriefAgent;
import com.yanyun.music.creativeagent.QualityDecision;
import com.yanyun.music.creativeagent.QualityEvaluationResult;
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
import java.util.Map;
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
    assertEquals(7, result.promptTemplateVersions().get("creative.brief.v7"));
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

    assertEquals(4, records.size());
    AgentRunRecord knowledgeRecord = records.get(0);
    assertEquals("KnowledgeRetrieve", knowledgeRecord.agentName());
    assertEquals("v0.3", knowledgeRecord.agentVersion());
    assertEquals("mock-kb-v1", knowledgeRecord.modelName());
    assertEquals("knowledge.retrieve.v1", knowledgeRecord.promptTemplateKey());
    assertEquals(AgentRunStatus.SUCCEEDED, knowledgeRecord.status());

    AgentRunRecord briefRecord = records.get(1);
    assertEquals("work-1", briefRecord.workId());
    assertEquals("CreativeBriefAgent", briefRecord.agentName());
    assertEquals("v0.5", briefRecord.agentVersion());
    assertEquals("INSPIRATION", briefRecord.operation());
    assertEquals("mock-creative-brief", briefRecord.modelName());
    assertEquals("creative.brief.v5", briefRecord.promptTemplateKey());
    assertEquals(5, briefRecord.promptTemplateVersion());
    assertEquals(AgentRunStatus.SUCCEEDED, briefRecord.status());
    assertNotNull(briefRecord.inputHash());
    assertNotNull(briefRecord.outputHash());
    assertTrue(briefRecord.latencyMs() >= 0);

    AgentRunRecord lyricsRecord = records.get(2);
    assertEquals("LyricsAgent", lyricsRecord.agentName());
    assertEquals("lyrics.inspiration.v1", lyricsRecord.promptTemplateKey());
    assertEquals(AgentRunStatus.SUCCEEDED, lyricsRecord.status());
    assertEquals("QualityEvaluationAgent", records.get(3).agentName());
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
    assertTrue(renderedInstructions.getFirst().contains("creative_core="));
    assertTrue(renderedInstructions.getFirst().contains("chosen_angle="));
    assertTrue(renderedInstructions.getFirst().contains("alternative_angles="));
    assertTrue(renderedInstructions.getFirst().contains("anti_cliche_strategy="));
    assertTrue(renderedInstructions.getFirst().contains("voice_texture="));
    assertTrue(renderedInstructions.getFirst().contains("image_pool="));
    assertTrue(renderedInstructions.getFirst().contains("song_energy="));
    assertTrue(renderedInstructions.getFirst().contains("songcraft_policy="));
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
    assertEquals(6, records.size());
    assertEquals("KnowledgeRetrieve", records.get(0).agentName());
    assertEquals("CreativeBriefAgent", records.get(1).agentName());
    assertEquals("LyricsAgent", records.get(2).agentName());
    assertEquals("QualityEvaluationAgent", records.get(3).agentName());
    assertEquals("LyricsAgent", records.get(4).agentName());
    assertEquals("QualityEvaluationAgent", records.get(5).agentName());
    assertEquals(AgentRunStatus.SUCCEEDED, records.get(0).status());
    assertEquals(AgentRunStatus.SUCCEEDED, records.get(1).status());
    assertEquals(AgentRunStatus.SUCCEEDED, records.get(2).status());
    assertEquals(BigDecimal.valueOf(0.91), result.qualityScore());
    assertTrue(result.lyricsText().contains("quality gate"));
    assertTrue(result.lyricsText().contains("Yanyun Sixteen Sounds"));
  }

  @Test
  void rewriteStillRejectedByQualityGateStopsBeforeReturningLyrics() {
    AtomicInteger deepSeekCalls = new AtomicInteger();
    AtomicInteger qualityCalls = new AtomicInteger();
    List<AgentRunRecord> records = new ArrayList<>();
    DeepSeekLyricsClient deepSeek =
        request -> {
          deepSeekCalls.incrementAndGet();
          return new DeepSeekLyricsResponse(
              "Song",
              "A generic rivers-and-moon story.",
              "[Verse]\n泛舟江湖看月明",
              "cinematic folk",
              "cover seed",
              List.of(),
              BigDecimal.valueOf(0.92));
        };
    DefaultLyricsGenerationService service =
        new DefaultLyricsGenerationService(
            knowledgeService(),
            promptService(),
            new MockCreativeBriefAgent(records::add),
            deepSeek,
            request -> {
              qualityCalls.incrementAndGet();
              return new QualityEvaluationResult(
                  request.gate(),
                  QualityDecision.REWRITE,
                  62,
                  List.of("歌词缺少明确的燕云十六声锚点，容易变成泛古风武侠。"),
                  "rewrite_lyrics",
                  true,
                  Map.of());
            },
            records::add);

    LyricsQualityException exception =
        assertThrows(
            LyricsQualityException.class,
            () -> service.generate(baseRequest(LyricsOperation.INSPIRATION)));

    assertTrue(exception.getMessage().contains("燕云十六声锚点"));
    assertEquals(2, deepSeekCalls.get());
    assertEquals(2, qualityCalls.get());
    assertEquals(4, records.size());
    assertEquals("KnowledgeRetrieve", records.get(0).agentName());
    assertEquals("CreativeBriefAgent", records.get(1).agentName());
    assertEquals("LyricsAgent", records.get(2).agentName());
    assertEquals("LyricsAgent", records.get(3).agentName());
  }

  @Test
  void rewriteUsesQualityGateRecommendedActionForTargetedInstruction() {
    AtomicInteger deepSeekCalls = new AtomicInteger();
    List<String> seenInstructions = new ArrayList<>();
    DeepSeekLyricsClient deepSeek =
        request -> {
          seenInstructions.add(request.instruction());
          int call = deepSeekCalls.incrementAndGet();
          return new DeepSeekLyricsResponse(
              "Song",
              "Summary",
              call == 1 ? "[Chorus]\n天高地阔任我游" : request.instruction(),
              "cinematic folk",
              "cover seed",
              List.of(),
              BigDecimal.valueOf(0.92));
        };
    AtomicInteger qualityCalls = new AtomicInteger();
    DefaultLyricsGenerationService service =
        new DefaultLyricsGenerationService(
            knowledgeService(),
            promptService(),
            new MockCreativeBriefAgent(),
            deepSeek,
            request -> {
              if (qualityCalls.incrementAndGet() == 1) {
                return new QualityEvaluationResult(
                    request.gate(),
                    QualityDecision.REWRITE,
                    76,
                    List.of("副歌没有声音记忆点。"),
                    "rewrite_memory_point",
                    true,
                    Map.of());
              }
              return new QualityEvaluationResult(
                  request.gate(), QualityDecision.PASS, 88, List.of(), "PASS", false, Map.of());
            },
            new ArrayList<AgentRunRecord>()::add);

    LyricsGenerationResult result = service.generate(baseRequest(LyricsOperation.INSPIRATION));

    assertEquals(2, deepSeekCalls.get());
    assertEquals(2, qualityCalls.get());
    assertTrue(seenInstructions.get(1).contains("Targeted rewrite action=rewrite_memory_point"));
    assertTrue(seenInstructions.get(1).contains("do not force a slogan"));
    assertTrue(result.lyricsText().contains("rewrite_memory_point"));
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

    LyricsGenerationTransientException exception =
        assertThrows(
            LyricsGenerationTransientException.class,
            () -> service.generate(baseRequest(LyricsOperation.LYRICS)));

    assertEquals(3, records.size());
    assertTrue(exception.getMessage().contains("AI 写词暂时失败"));
    assertEquals("KnowledgeRetrieve", records.get(0).agentName());
    assertEquals("CreativeBriefAgent", records.get(1).agentName());
    AgentRunRecord record = records.get(2);
    assertEquals(AgentRunStatus.FAILED, record.status());
    assertEquals("DEEPSEEK_LYRICS_FAILED", record.failureCode());
    assertTrue(record.failureMessage().contains("Bearer [REDACTED]"));
    assertTrue(record.failureMessage().contains("SecretKey=[REDACTED]"));
    assertFalse(record.failureMessage().contains("dummy-value"));
  }

  @Test
  void failedPolishReturnsTransientRetryableMessage() {
    DeepSeekLyricsClient deepSeek =
        request -> {
          throw new IllegalStateException("DeepSeek response content JSON is invalid");
        };
    DefaultLyricsGenerationService service =
        new DefaultLyricsGenerationService(knowledgeService(), promptService(), deepSeek);

    LyricsGenerationTransientException exception =
        assertThrows(
            LyricsGenerationTransientException.class,
            () -> service.generate(baseRequest(LyricsOperation.POLISH)));

    assertEquals("AI 润色暂时失败，本次未消耗改词次数，请稍后重试。", exception.getMessage());
  }

  @Test
  void failedCreativeBriefFallsBackAndContinuesGeneration() {
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
              throw new IllegalStateException("DeepSeek response content is empty");
            },
            request -> {
              deepSeekCalls.incrementAndGet();
              return new DeepSeekLyricsResponse(
                  "Song",
                  "Summary",
                  request.instruction(),
                  "cinematic folk",
                  "cover seed",
                  List.of(),
                  BigDecimal.valueOf(0.86));
            },
            records::add);

    LyricsGenerationResult result = service.generate(baseRequest(LyricsOperation.INSPIRATION));

    assertEquals(1, deepSeekCalls.get());
    assertTrue(result.lyricsText().contains("creative_brief_unavailable_fallback"));
    assertEquals(4, records.size());
    assertEquals("KnowledgeRetrieve", records.get(0).agentName());
    AgentRunRecord record = records.get(1);
    assertEquals("CreativeBriefAgent", record.agentName());
    assertEquals(AgentRunStatus.FAILED, record.status());
    assertEquals("CREATIVE_BRIEF_AGENT_FAILED", record.failureCode());
    assertEquals("LyricsAgent", records.get(2).agentName());
    assertEquals(AgentRunStatus.SUCCEEDED, records.get(2).status());
    assertEquals("QualityEvaluationAgent", records.get(3).agentName());
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
