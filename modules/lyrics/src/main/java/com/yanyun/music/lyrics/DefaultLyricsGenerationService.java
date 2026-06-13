package com.yanyun.music.lyrics;

import com.yanyun.music.agentruntime.AgentRunHashing;
import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunRecorder;
import com.yanyun.music.agentruntime.AgentRunStatus;
import com.yanyun.music.agentruntime.NoopAgentRunRecorder;
import com.yanyun.music.creativeagent.CreativeBriefAgent;
import com.yanyun.music.creativeagent.CreativeBriefRequest;
import com.yanyun.music.creativeagent.CreativeBriefResult;
import com.yanyun.music.creativeagent.MockCreativeBriefAgent;
import com.yanyun.music.creativeagent.MockQualityEvaluationAgent;
import com.yanyun.music.creativeagent.QualityDecision;
import com.yanyun.music.creativeagent.QualityEvaluationAgent;
import com.yanyun.music.creativeagent.QualityEvaluationRequest;
import com.yanyun.music.creativeagent.QualityEvaluationResult;
import com.yanyun.music.creativeagent.QualityGate;
import com.yanyun.music.deepseek.DeepSeekLyricsClient;
import com.yanyun.music.deepseek.DeepSeekLyricsRequest;
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
import java.util.Map;

public final class DefaultLyricsGenerationService implements LyricsGenerationService {

  private static final BigDecimal QUALITY_REWRITE_THRESHOLD = BigDecimal.valueOf(0.70);
  private static final String CREATIVE_BRIEF_TEMPLATE_KEY = "creative.brief.v5";
  private static final int CREATIVE_BRIEF_TEMPLATE_VERSION = 5;

  private final KnowledgeService knowledgeService;
  private final PromptTemplateService promptTemplateService;
  private final CreativeBriefAgent creativeBriefAgent;
  private final DeepSeekLyricsClient deepSeekLyricsClient;
  private final QualityEvaluationAgent qualityEvaluationAgent;
  private final AgentRunRecorder agentRunRecorder;

  public DefaultLyricsGenerationService(
      KnowledgeService knowledgeService,
      PromptTemplateService promptTemplateService,
      DeepSeekLyricsClient deepSeekLyricsClient) {
    this(
        knowledgeService,
        promptTemplateService,
        new MockCreativeBriefAgent(NoopAgentRunRecorder.INSTANCE),
        deepSeekLyricsClient,
        new MockQualityEvaluationAgent(NoopAgentRunRecorder.INSTANCE),
        NoopAgentRunRecorder.INSTANCE);
  }

  public DefaultLyricsGenerationService(
      KnowledgeService knowledgeService,
      PromptTemplateService promptTemplateService,
      DeepSeekLyricsClient deepSeekLyricsClient,
      AgentRunRecorder agentRunRecorder) {
    this(
        knowledgeService,
        promptTemplateService,
        new MockCreativeBriefAgent(agentRunRecorder),
        deepSeekLyricsClient,
        new MockQualityEvaluationAgent(agentRunRecorder),
        agentRunRecorder);
  }

  public DefaultLyricsGenerationService(
      KnowledgeService knowledgeService,
      PromptTemplateService promptTemplateService,
      CreativeBriefAgent creativeBriefAgent,
      DeepSeekLyricsClient deepSeekLyricsClient,
      AgentRunRecorder agentRunRecorder) {
    this(
        knowledgeService,
        promptTemplateService,
        creativeBriefAgent,
        deepSeekLyricsClient,
        new MockQualityEvaluationAgent(agentRunRecorder),
        agentRunRecorder);
  }

  public DefaultLyricsGenerationService(
      KnowledgeService knowledgeService,
      PromptTemplateService promptTemplateService,
      CreativeBriefAgent creativeBriefAgent,
      DeepSeekLyricsClient deepSeekLyricsClient,
      QualityEvaluationAgent qualityEvaluationAgent,
      AgentRunRecorder agentRunRecorder) {
    this.knowledgeService = knowledgeService;
    this.promptTemplateService = promptTemplateService;
    this.creativeBriefAgent =
        creativeBriefAgent == null
            ? new MockCreativeBriefAgent(agentRunRecorder)
            : creativeBriefAgent;
    this.deepSeekLyricsClient = deepSeekLyricsClient;
    this.qualityEvaluationAgent =
        qualityEvaluationAgent == null
            ? new MockQualityEvaluationAgent(agentRunRecorder)
            : qualityEvaluationAgent;
    this.agentRunRecorder =
        agentRunRecorder == null ? NoopAgentRunRecorder.INSTANCE : agentRunRecorder;
  }

  @Override
  public LyricsGenerationResult generate(LyricsGenerationRequest request) {
    KnowledgeRetrievalResult knowledge = retrieveKnowledge(request);
    CreativeBriefResult creativeBrief = generateCreativeBrief(request, knowledge);
    ensureCreativeDomainAllowed(creativeBrief);
    LyricsGenerationRequest briefedRequest = withCreativeBrief(request, creativeBrief);
    PromptRenderResult prompt = renderPrompt(briefedRequest, knowledge);
    DeepSeekLyricsResponse response = generateWithDeepSeek(briefedRequest, prompt, knowledge);
    QualityEvaluationResult quality =
        evaluateLyricsQuality(briefedRequest, response, creativeBrief, knowledge);
    if (isLowQuality(response) || shouldRewrite(quality)) {
      LyricsGenerationRequest rewriteRequest = rewriteRequest(briefedRequest);
      prompt = renderPrompt(rewriteRequest, knowledge);
      response = generateWithDeepSeek(rewriteRequest, prompt, knowledge);
      quality = evaluateLyricsQuality(rewriteRequest, response, creativeBrief, knowledge);
    }
    ensureLyricsQualityAllowed(response, quality);
    return toResult(response, creativeBrief, knowledge, prompt);
  }

  private void ensureCreativeDomainAllowed(CreativeBriefResult creativeBrief) {
    if (creativeBrief.domainDecision().rejected()) {
      throw new LyricsCreativeDomainException(
          firstNonBlank(
              creativeBrief.userFacingMessage(), "当前只支持燕云十六声相关创作，请改成燕云里的江湖、武学、奇术、乱世同行或寻声记忆方向。"),
          creativeBrief.yanyunRewriteSuggestion());
    }
  }

  private PromptRenderResult renderPrompt(
      LyricsGenerationRequest request, KnowledgeRetrievalResult knowledge) {
    return promptTemplateService.render(
        new PromptRenderRequest(
            templateKey(request.operation()),
            request.operation().name(),
            request.userInput(),
            request.currentLyrics(),
            request.instruction(),
            request.musicStyle(),
            request.vocalPreference(),
            knowledge.references().stream().map(KnowledgeReference::content).toList()));
  }

  private CreativeBriefResult generateCreativeBrief(
      LyricsGenerationRequest request, KnowledgeRetrievalResult knowledge) {
    return creativeBriefAgent.generate(
        new CreativeBriefRequest(
            request.userId(),
            request.workId(),
            request.operation().name(),
            request.userInput(),
            request.currentLyrics(),
            request.instruction(),
            request.requestedTitle(),
            request.musicStyle(),
            request.vocalPreference(),
            knowledge.references().stream().map(KnowledgeReference::displayName).toList()));
  }

  private LyricsGenerationRequest withCreativeBrief(
      LyricsGenerationRequest request, CreativeBriefResult creativeBrief) {
    return new LyricsGenerationRequest(
        request.userId(),
        request.workId(),
        request.operation(),
        request.userInput(),
        request.currentLyrics(),
        appendInstruction(request.instruction(), creativeBriefInstruction(creativeBrief)),
        request.requestedTitle(),
        firstNonBlank(request.musicStyle(), creativeBrief.musicDirection()),
        request.vocalPreference());
  }

  private String creativeBriefInstruction(CreativeBriefResult creativeBrief) {
    return """
        Creative brief:
        domain_decision=%s
        intent=%s
        theme=%s
        mood_tags=%s
        narrative_viewpoint=%s
        music_direction=%s
        yanyun_references=%s
        constraints=%s
        knowledge_policy=Knowledge references are optional creative material. Preserve the user's story core. Do not force official names or the words Yanyun/Sixteen Sounds unless they fit naturally. Do not turn lyrics into plot summary.
        """
        .formatted(
            creativeBrief.domainDecision(),
            creativeBrief.userIntentSummary(),
            creativeBrief.theme(),
            creativeBrief.moodTags(),
            creativeBrief.narrativeViewpoint(),
            creativeBrief.musicDirection(),
            creativeBrief.yanyunReferences(),
            creativeBrief.constraints()
                + (creativeBrief.yanyunRewriteSuggestion() == null
                    ? ""
                    : "\nyanyun_rewrite_suggestion=" + creativeBrief.yanyunRewriteSuggestion()))
        .trim();
  }

  private QualityEvaluationResult evaluateLyricsQuality(
      LyricsGenerationRequest request,
      DeepSeekLyricsResponse response,
      CreativeBriefResult creativeBrief,
      KnowledgeRetrievalResult knowledge) {
    return qualityEvaluationAgent.evaluate(
        new QualityEvaluationRequest(
            request.workId(),
            QualityGate.LYRICS,
            response.songTitle(),
            response.lyricsText(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Map.ofEntries(
                Map.entry("operation", request.operation().name()),
                Map.entry("user_input", firstNonBlank(request.userInput(), "")),
                Map.entry("instruction", firstNonBlank(request.instruction(), "")),
                Map.entry("song_summary", firstNonBlank(response.songSummary(), "")),
                Map.entry("music_prompt", firstNonBlank(response.musicPrompt(), "")),
                Map.entry("cover_prompt_seed", firstNonBlank(response.coverPromptSeed(), "")),
                Map.entry("music_style", firstNonBlank(request.musicStyle(), "")),
                Map.entry(
                    "creative_brief_theme",
                    creativeBrief == null ? "" : firstNonBlank(creativeBrief.theme(), "")),
                Map.entry(
                    "creative_brief_yanyun_references",
                    creativeBrief == null ? List.of() : creativeBrief.yanyunReferences()),
                Map.entry(
                    "creative_brief_constraints",
                    creativeBrief == null ? List.of() : creativeBrief.constraints()),
                Map.entry("knowledge_base_version", knowledge == null ? "" : knowledge.kbVersion()),
                Map.entry(
                    "knowledge_reference_ids",
                    knowledge == null
                        ? List.of()
                        : knowledge.references().stream()
                            .map(KnowledgeReference::chunkId)
                            .toList()),
                Map.entry("risk_notes", response.riskNotes()))));
  }

  private boolean shouldRewrite(QualityEvaluationResult quality) {
    return quality.decision() == QualityDecision.REWRITE
        || quality.decision() == QualityDecision.RETRY;
  }

  private void ensureLyricsQualityAllowed(
      DeepSeekLyricsResponse response, QualityEvaluationResult quality) {
    if (quality.decision() == QualityDecision.BLOCK
        || quality.decision() == QualityDecision.MANUAL_REVIEW
        || quality.decision() == QualityDecision.REWRITE
        || quality.decision() == QualityDecision.RETRY
        || isLowQuality(response)) {
      throw new LyricsQualityException(lyricsQualityMessage(response, quality));
    }
  }

  private String lyricsQualityMessage(
      DeepSeekLyricsResponse response, QualityEvaluationResult quality) {
    if (!quality.reasons().isEmpty()) {
      return String.join("; ", quality.reasons());
    }
    if (isLowQuality(response)) {
      return "歌词质量分不足，请补充更明确的燕云十六声主题后重试。";
    }
    return "歌词不够贴合燕云十六声，请调整灵感后重试。";
  }

  private DeepSeekLyricsResponse generateWithDeepSeek(
      LyricsGenerationRequest request,
      PromptRenderResult prompt,
      KnowledgeRetrievalResult knowledge) {
    DeepSeekLyricsRequest deepSeekRequest =
        new DeepSeekLyricsRequest(
            request.operation().name(),
            prompt.prompt(),
            request.userInput(),
            request.currentLyrics(),
            request.instruction(),
            request.requestedTitle(),
            request.musicStyle(),
            request.vocalPreference(),
            knowledge.references().stream().map(KnowledgeReference::displayName).toList());
    long startedAt = System.nanoTime();
    try {
      DeepSeekLyricsResponse response = deepSeekLyricsClient.generate(deepSeekRequest);
      recordAgentRun(request, prompt, deepSeekRequest, response, startedAt, null);
      return response;
    } catch (RuntimeException exception) {
      recordAgentRun(request, prompt, deepSeekRequest, null, startedAt, exception);
      throw exception;
    }
  }

  private void recordAgentRun(
      LyricsGenerationRequest request,
      PromptRenderResult prompt,
      DeepSeekLyricsRequest deepSeekRequest,
      DeepSeekLyricsResponse response,
      long startedAt,
      RuntimeException exception) {
    agentRunRecorder.record(
        new AgentRunRecord(
            request.workId(),
            null,
            "LyricsAgent",
            "v0.1",
            request.operation().name(),
            deepSeekLyricsClient.modelName(),
            prompt.templateKey(),
            prompt.version(),
            AgentRunHashing.sha256(inputFingerprint(deepSeekRequest)),
            response == null ? null : AgentRunHashing.sha256(outputFingerprint(response)),
            exception == null ? AgentRunStatus.SUCCEEDED : AgentRunStatus.FAILED,
            elapsedMs(startedAt),
            null,
            null,
            null,
            exception == null ? null : "DEEPSEEK_LYRICS_FAILED",
            exception == null ? null : exception.getMessage()));
  }

  private int elapsedMs(long startedAt) {
    long elapsed = (System.nanoTime() - startedAt) / 1_000_000L;
    return elapsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, elapsed);
  }

  private KnowledgeRetrievalResult retrieveKnowledge(LyricsGenerationRequest request) {
    KnowledgeRetrievalRequest retrievalRequest =
        new KnowledgeRetrievalRequest(query(request), List.of("yanyun", "lyrics"), 6);
    long startedAt = System.nanoTime();
    try {
      KnowledgeRetrievalResult result = knowledgeService.retrieve(retrievalRequest);
      recordKnowledgeRun(request, retrievalRequest, result, startedAt, null);
      return result;
    } catch (RuntimeException exception) {
      KnowledgeRetrievalResult fallback = new KnowledgeRetrievalResult("unavailable", List.of());
      recordKnowledgeRun(request, retrievalRequest, fallback, startedAt, exception);
      return fallback;
    }
  }

  private void recordKnowledgeRun(
      LyricsGenerationRequest request,
      KnowledgeRetrievalRequest retrievalRequest,
      KnowledgeRetrievalResult result,
      long startedAt,
      RuntimeException exception) {
    agentRunRecorder.record(
        new AgentRunRecord(
            request.workId(),
            null,
            "KnowledgeRetrieve",
            "v0.3",
            request.operation().name(),
            result == null ? "knowledge-unavailable" : result.kbVersion(),
            "knowledge.retrieve.v1",
            1,
            AgentRunHashing.sha256(knowledgeInputFingerprint(retrievalRequest)),
            result == null ? null : AgentRunHashing.sha256(knowledgeOutputFingerprint(result)),
            exception == null ? AgentRunStatus.SUCCEEDED : AgentRunStatus.FAILED,
            elapsedMs(startedAt),
            null,
            null,
            null,
            exception == null ? null : "KNOWLEDGE_RETRIEVAL_FAILED",
            exception == null ? null : exception.getMessage()));
  }

  private String knowledgeInputFingerprint(KnowledgeRetrievalRequest request) {
    return String.join(
        "\n",
        firstNonBlank(request.query(), ""),
        request.tags().toString(),
        Integer.toString(request.limit()));
  }

  private String knowledgeOutputFingerprint(KnowledgeRetrievalResult result) {
    return String.join(
        "\n",
        result.kbVersion(),
        result.references().stream().map(KnowledgeReference::chunkId).toList().toString());
  }

  private String inputFingerprint(DeepSeekLyricsRequest request) {
    return String.join(
        "\n",
        nullToEmpty(request.operation()),
        nullToEmpty(request.prompt()),
        nullToEmpty(request.userInput()),
        nullToEmpty(request.currentLyrics()),
        nullToEmpty(request.instruction()),
        nullToEmpty(request.requestedTitle()),
        nullToEmpty(request.musicStyle()),
        nullToEmpty(request.vocalPreference()),
        String.join(",", request.yanyunReferences()));
  }

  private String outputFingerprint(DeepSeekLyricsResponse response) {
    return String.join(
        "\n",
        nullToEmpty(response.songTitle()),
        nullToEmpty(response.songSummary()),
        nullToEmpty(response.lyricsText()),
        nullToEmpty(response.musicPrompt()),
        nullToEmpty(response.coverPromptSeed()),
        response.riskNotes().toString(),
        response.qualityScore() == null ? "" : response.qualityScore().toPlainString());
  }

  private LyricsGenerationRequest rewriteRequest(LyricsGenerationRequest request) {
    return new LyricsGenerationRequest(
        request.userId(),
        request.workId(),
        request.operation(),
        request.userInput(),
        request.currentLyrics(),
        appendInstruction(
            request.instruction(),
            "Rewrite once because the previous lyrics did not pass the quality gate. Preserve the user's emotion, music preference, and story core. Make the final lyrics feel like they belong inside the Yanyun Sixteen Sounds world through character choices, place atmosphere, wuxia actions, player experience, or emotional texture. Do not force official names or the words Yanyun/Sixteen Sounds. Do not over-heroize ordinary characters. Avoid generic wuxia phrasing and plot-summary writing."),
        request.requestedTitle(),
        request.musicStyle(),
        request.vocalPreference());
  }

  private LyricsGenerationResult toResult(
      DeepSeekLyricsResponse response,
      CreativeBriefResult creativeBrief,
      KnowledgeRetrievalResult knowledge,
      PromptRenderResult prompt) {
    return new LyricsGenerationResult(
        response.songTitle(),
        response.songSummary(),
        response.lyricsText(),
        response.musicPrompt(),
        response.coverPromptSeed(),
        response.riskNotes(),
        yanyunReferences(creativeBrief, knowledge),
        knowledge.kbVersion(),
        Map.of(
            prompt.templateKey(),
            prompt.version(),
            CREATIVE_BRIEF_TEMPLATE_KEY,
            CREATIVE_BRIEF_TEMPLATE_VERSION),
        response.qualityScore());
  }

  private List<String> yanyunReferences(
      CreativeBriefResult creativeBrief, KnowledgeRetrievalResult knowledge) {
    if (creativeBrief != null && !creativeBrief.yanyunReferences().isEmpty()) {
      return creativeBrief.yanyunReferences();
    }
    return knowledge.references().stream().map(KnowledgeReference::displayName).toList();
  }

  private boolean isLowQuality(DeepSeekLyricsResponse response) {
    return response.qualityScore() != null
        && response.qualityScore().compareTo(QUALITY_REWRITE_THRESHOLD) < 0;
  }

  private String query(LyricsGenerationRequest request) {
    return firstNonBlank(
        request.userInput(),
        firstNonBlank(request.instruction(), firstNonBlank(request.currentLyrics(), "yanyun")));
  }

  private String templateKey(LyricsOperation operation) {
    return switch (operation) {
      case INSPIRATION -> "lyrics.inspiration.v1";
      case LYRICS -> "lyrics.user_lyrics.v1";
      case POLISH -> "lyrics.polish.v1";
      case CONTINUE -> "lyrics.continue.v1";
    };
  }

  private String appendInstruction(String value, String addition) {
    return value == null || value.isBlank() ? addition : value.trim() + "\n" + addition;
  }

  private String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
