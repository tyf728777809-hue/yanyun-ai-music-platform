package com.yanyun.music.lyrics;

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

  public DefaultLyricsGenerationService(
      KnowledgeService knowledgeService,
      PromptTemplateService promptTemplateService,
      DeepSeekLyricsClient deepSeekLyricsClient) {
    this.knowledgeService = knowledgeService;
    this.promptTemplateService = promptTemplateService;
    this.deepSeekLyricsClient = deepSeekLyricsClient;
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
    return deepSeekLyricsClient.generate(
        new DeepSeekLyricsRequest(
            request.operation().name(),
            prompt.prompt(),
            request.userInput(),
            request.currentLyrics(),
            request.instruction(),
            request.requestedTitle(),
            request.musicStyle(),
            request.vocalPreference(),
            knowledge.references().stream().map(KnowledgeReference::displayName).toList()));
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
}
