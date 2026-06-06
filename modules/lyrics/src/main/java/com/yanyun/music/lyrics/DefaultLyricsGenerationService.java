package com.yanyun.music.lyrics;

import com.yanyun.music.agentruntime.AgentRunHashing;
import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunRecorder;
import com.yanyun.music.agentruntime.AgentRunStatus;
import com.yanyun.music.agentruntime.NoopAgentRunRecorder;
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

  private final KnowledgeService knowledgeService;
  private final PromptTemplateService promptTemplateService;
  private final DeepSeekLyricsClient deepSeekLyricsClient;
  private final AgentRunRecorder agentRunRecorder;

  public DefaultLyricsGenerationService(
      KnowledgeService knowledgeService,
      PromptTemplateService promptTemplateService,
      DeepSeekLyricsClient deepSeekLyricsClient) {
    this(
        knowledgeService,
        promptTemplateService,
        deepSeekLyricsClient,
        NoopAgentRunRecorder.INSTANCE);
  }

  public DefaultLyricsGenerationService(
      KnowledgeService knowledgeService,
      PromptTemplateService promptTemplateService,
      DeepSeekLyricsClient deepSeekLyricsClient,
      AgentRunRecorder agentRunRecorder) {
    this.knowledgeService = knowledgeService;
    this.promptTemplateService = promptTemplateService;
    this.deepSeekLyricsClient = deepSeekLyricsClient;
    this.agentRunRecorder =
        agentRunRecorder == null ? NoopAgentRunRecorder.INSTANCE : agentRunRecorder;
  }

  @Override
  public LyricsGenerationResult generate(LyricsGenerationRequest request) {
    KnowledgeRetrievalResult knowledge =
        knowledgeService.retrieve(
            new KnowledgeRetrievalRequest(query(request), List.of("yanyun", "lyrics"), 3));
    PromptRenderResult prompt = renderPrompt(request, knowledge);
    DeepSeekLyricsResponse response = generateWithDeepSeek(request, prompt, knowledge);
    if (isLowQuality(response)) {
      LyricsGenerationRequest rewriteRequest = rewriteRequest(request);
      prompt = renderPrompt(rewriteRequest, knowledge);
      response = generateWithDeepSeek(rewriteRequest, prompt, knowledge);
    }
    return toResult(response, knowledge, prompt);
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
        appendInstruction(request.instruction(), "Improve structure, imagery, and singability."),
        request.requestedTitle(),
        request.musicStyle(),
        request.vocalPreference());
  }

  private LyricsGenerationResult toResult(
      DeepSeekLyricsResponse response,
      KnowledgeRetrievalResult knowledge,
      PromptRenderResult prompt) {
    return new LyricsGenerationResult(
        response.songTitle(),
        response.songSummary(),
        response.lyricsText(),
        response.musicPrompt(),
        response.coverPromptSeed(),
        response.riskNotes(),
        knowledge.references().stream().map(KnowledgeReference::displayName).toList(),
        knowledge.kbVersion(),
        Map.of(prompt.templateKey(), prompt.version()),
        response.qualityScore());
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
