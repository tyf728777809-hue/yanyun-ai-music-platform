package com.yanyun.music.lyrics;

import com.yanyun.music.agentruntime.AgentRunHashing;
import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunRecorder;
import com.yanyun.music.agentruntime.AgentRunStatus;
import com.yanyun.music.agentruntime.NoopAgentRunRecorder;
import com.yanyun.music.creativeagent.CreativeBoundaryTerms;
import com.yanyun.music.creativeagent.CreativeBriefAgent;
import com.yanyun.music.creativeagent.CreativeBriefRequest;
import com.yanyun.music.creativeagent.CreativeBriefResult;
import com.yanyun.music.creativeagent.CreativeDomainDecision;
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
import com.yanyun.music.knowledge.ResolvedKnowledgeEntity;
import com.yanyun.music.prompt.PromptRenderRequest;
import com.yanyun.music.prompt.PromptRenderResult;
import com.yanyun.music.prompt.PromptTemplateService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DefaultLyricsGenerationService implements LyricsGenerationService {

  private static final BigDecimal QUALITY_REWRITE_THRESHOLD = BigDecimal.valueOf(0.80);
  private static final String CREATIVE_BRIEF_TEMPLATE_KEY = "creative.brief.v8";
  private static final int CREATIVE_BRIEF_TEMPLATE_VERSION = 8;

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
    ensureEntityResolutionAllowed(request, knowledge);
    if (lightweightEditOperation(request.operation())) {
      return generateLightweightEdit(request, knowledge);
    }
    CreativeBriefResult creativeBrief = generateCreativeBrief(request, knowledge);
    ensureCreativeDomainAllowed(creativeBrief);
    LyricsGenerationRequest briefedRequest = withCreativeBrief(request, creativeBrief, knowledge);
    PromptRenderResult prompt = renderPrompt(briefedRequest, knowledge);
    DeepSeekLyricsResponse response = generateWithDeepSeek(briefedRequest, prompt, knowledge);
    QualityEvaluationResult quality =
        evaluateLyricsQuality(briefedRequest, response, creativeBrief, knowledge);
    if (isLowQuality(response) || shouldRewrite(quality)) {
      LyricsGenerationRequest rewriteRequest = rewriteRequest(briefedRequest, quality);
      prompt = renderPrompt(rewriteRequest, knowledge);
      response = generateWithDeepSeek(rewriteRequest, prompt, knowledge);
      quality = evaluateLyricsQuality(rewriteRequest, response, creativeBrief, knowledge);
    }
    ensureLyricsQualityAllowed(response, quality);
    return toResult(response, creativeBrief, knowledge, prompt, true);
  }

  private LyricsGenerationResult generateLightweightEdit(
      LyricsGenerationRequest request, KnowledgeRetrievalResult knowledge) {
    CreativeBriefResult lightweightBrief = fallbackCreativeBrief(request, knowledge);
    ensureCreativeDomainAllowed(lightweightBrief);
    LyricsGenerationRequest editRequest = withLightweightEditInstruction(request, knowledge);
    PromptRenderResult prompt = renderPrompt(editRequest, knowledge);
    DeepSeekLyricsResponse response = generateWithDeepSeek(editRequest, prompt, knowledge);
    ensureLightweightEditQualityAllowed(editRequest, response);
    return toResult(response, lightweightBrief, knowledge, prompt, false);
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
            request.mood(),
            request.musicStyle(),
            request.vocalPreference(),
            knowledge.references().stream().map(KnowledgeReference::content).toList()));
  }

  private CreativeBriefResult generateCreativeBrief(
      LyricsGenerationRequest request, KnowledgeRetrievalResult knowledge) {
    CreativeBriefRequest briefRequest =
        new CreativeBriefRequest(
            request.userId(),
            request.workId(),
            request.operation().name(),
            request.userInput(),
            request.currentLyrics(),
            request.instruction(),
            request.requestedTitle(),
            request.mood(),
            request.musicStyle(),
            request.vocalPreference(),
            yanyunReferenceLabels(knowledge));
    try {
      return creativeBriefAgent.generate(briefRequest);
    } catch (RuntimeException exception) {
      return fallbackCreativeBrief(request, knowledge);
    }
  }

  private CreativeBriefResult fallbackCreativeBrief(
      LyricsGenerationRequest request, KnowledgeRetrievalResult knowledge) {
    List<String> references = yanyunReferenceLabels(knowledge);
    String boundaryText =
        String.join(
            " ",
            firstNonBlank(request.userInput(), ""),
            firstNonBlank(request.currentLyrics(), ""),
            firstNonBlank(request.instruction(), ""),
            firstNonBlank(request.requestedTitle(), ""),
            references.toString());
    CreativeDomainDecision fallbackDecision = CreativeDomainDecision.PASS;
    String userFacingMessage = null;
    String rewriteSuggestion = null;
    if (CreativeBoundaryTerms.containsOtherIpTerm(boundaryText)) {
      if (CreativeBoundaryTerms.containsYanyunTerm(boundaryText)) {
        fallbackDecision = CreativeDomainDecision.REWRITE_TO_YANYUN;
        rewriteSuggestion = "删除其他 IP 专属名词，只保留用户想要的情绪、结构或音乐能量，并转成燕云十六声语境。";
      } else {
        fallbackDecision = CreativeDomainDecision.REJECT;
        userFacingMessage = "当前只支持燕云十六声相关创作。可以改成燕云里的江湖、武学、奇术、乱世同行或寻声记忆方向。";
        rewriteSuggestion = "请把主题改为燕云十六声相关内容。";
      }
    }
    String theme =
        firstNonBlank(
            request.requestedTitle(),
            firstNonBlank(request.userInput(), firstNonBlank(request.instruction(), "燕云玩家故事")));
    return new CreativeBriefResult(
        fallbackDecision,
        "Creative brief agent unavailable; continue from user request and retrieved Yanyun context.",
        trimToLength(theme, 80),
        List.of("user-directed"),
        "preserve the user's viewpoint and keep the language singable",
        firstNonBlank(request.musicStyle(), "open music style"),
        references.isEmpty() ? List.of("燕云十六声创作域") : references,
        List.of(
            "creative_brief_unavailable_fallback",
            "preserve user instruction",
            "ground named entities in retrieved knowledge when available"),
        List.of("creative_brief_agent_fallback"),
        userFacingMessage,
        rewriteSuggestion,
        List.of("use current lyrics and user edit instruction as the primary source"),
        "Use the user's current lyric or story core as the creative core.",
        "Enter through the user's requested change instead of inventing a new plot.",
        List.of("voice-led rewrite", "image-led rewrite"),
        "Avoid generic wuxia phrasing; keep the requested edit concrete and singable.",
        "user-directed, natural, singable",
        references.isEmpty() ? List.of() : references,
        "focused rewrite",
        "Keep one clear song argument from the user's request.",
        "the user's existing lyric voice",
        "what the user wants to change versus what must be preserved",
        "make the requested edit deepen the same song rather than become a new song",
        "make the chorus clarify and repeat the song's main emotional argument",
        "one repeatable phrase, image loop, or rhythm hook from the original song");
  }

  private LyricsGenerationRequest withCreativeBrief(
      LyricsGenerationRequest request,
      CreativeBriefResult creativeBrief,
      KnowledgeRetrievalResult knowledge) {
    return new LyricsGenerationRequest(
        request.userId(),
        request.workId(),
        request.operation(),
        request.userInput(),
        request.currentLyrics(),
        appendInstruction(
            request.instruction(), creativeBriefInstruction(creativeBrief, knowledge)),
        request.requestedTitle(),
        request.mood(),
        firstNonBlank(request.musicStyle(), creativeBrief.musicDirection()),
        request.vocalPreference());
  }

  private LyricsGenerationRequest withLightweightEditInstruction(
      LyricsGenerationRequest request, KnowledgeRetrievalResult knowledge) {
    return new LyricsGenerationRequest(
        request.userId(),
        request.workId(),
        request.operation(),
        request.userInput(),
        request.currentLyrics(),
        appendInstruction(request.instruction(), lightweightEditInstruction(request, knowledge)),
        request.requestedTitle(),
        request.mood(),
        request.musicStyle(),
        request.vocalPreference());
  }

  private String lightweightEditInstruction(
      LyricsGenerationRequest request, KnowledgeRetrievalResult knowledge) {
    String operationCopy =
        request.operation() == LyricsOperation.CONTINUE
            ? "This is a continuation task, not a new full-song rewrite."
            : "This is a lightweight polish task, not a new full-song rewrite.";
    return """
        Lightweight edit brief:
        %s
        edit_goal=%s
        resolved_entities=%s
        yanyun_references=%s
        edit_policy=Preserve the current song title, point of view, song thesis, emotional arc, section structure, and most usable lines. Apply only the user's requested change. Keep the result singable and coherent. Do not run away into a different story, a different protagonist, or a new song unless the user explicitly asked for that.
        polish_policy=For POLISH, repair diction, rhyme, clarity, sentence length, chorus memory, and weak lines while retaining the original core.
        continue_policy=For CONTINUE, extend the existing voice and structure naturally, then close the song if appropriate.
        grounding_policy=Use retrieved Yanyun entities and references as grounding material. Do not output user typos as canonical names. Do not force official names or the words Yanyun/Sixteen Sounds unless they fit naturally.
        speed_policy=Return one clean JSON object. Avoid over-explaining. Do not include Markdown.
        """
        .formatted(
            operationCopy,
            firstNonBlank(request.instruction(), "keep the current lyric's direction"),
            entityLabels(knowledge),
            yanyunReferenceLabels(knowledge))
        .trim();
  }

  private String creativeBriefInstruction(
      CreativeBriefResult creativeBrief, KnowledgeRetrievalResult knowledge) {
    return """
        Creative brief:
        domain_decision=%s
        intent=%s
        theme=%s
        mood_tags=%s
        narrative_viewpoint=%s
        music_direction=%s
        yanyun_references=%s
        resolved_entities=%s
        constraints=%s
        song_core=%s
        singer_voice=%s
        listener_target=%s
        emotional_engine=%s
        chorus_job=%s
        avoid_direction=%s
        creative_core=%s
        chosen_angle=%s
        alternative_angles=%s
        anti_cliche_strategy=%s
        voice_texture=%s
        image_pool=%s
        song_energy=%s
        song_thesis=%s
        pov=%s
        central_tension=%s
        emotional_turn=%s
        chorus_function=%s
        memory_device=%s
        v08_policy=First complete song_core, singer_voice, emotional_engine, and chorus_job. If older creative fields conflict with these v0.8 fields, follow the v0.8 fields. The song must be understandable after one listen and must not become a checklist of references.
        knowledge_policy=Knowledge references are optional creative material. Preserve the user's story core. Use only the facts, relationships, scenes, and emotional texture that serve song_core. Do not force official names or the words Yanyun/Sixteen Sounds unless they fit naturally. Do not turn lyrics into plot summary.
        entity_grounding_policy=If the user names a character, storyline, place, faction, or gameplay concept and resolved_entities maps it to a canonical Yanyun entity, use that canonical entity as the grounding source. Do not preserve user typos as official names. Character or storyline songs must use the core relationships, life events, conflicts, and scenes from the matched knowledge references instead of generic wuxia atmosphere.
        songcraft_policy=Use creative_core, chosen_angle, anti_cliche_strategy, voice_texture, image_pool, song_energy, song_thesis, pov, central_tension, emotional_turn, chorus_function, and memory_device only as open supporting guidance. Do not treat them as a rigid template. Before writing, decide one clear song thesis and keep every verse, chorus, and bridge serving it. Choose the best writing path for this song: narrative, image-led, colloquial, dialogue, monologue, group portrait, contrast, repetition, irony, silence, or anti-cliche. Music style is only a voice/rhythm/energy reference, not a creative cage.
        polish_policy=For POLISH, keep the original song title, point of view, thesis, emotional arc, and most usable lines unless the user explicitly asks to replace them. Improve diction, rhyme, singability, clarity, and requested weak spots; do not silently write a different song.
        direct_write_policy=Use this brief directly. Do not wait for or assume an extra craft plan. If the brief is imperfect, choose the clearest song thesis from the user input and retrieved Yanyun context, then write one coherent song around it.
        """
        .formatted(
            creativeBrief.domainDecision(),
            creativeBrief.userIntentSummary(),
            creativeBrief.theme(),
            creativeBrief.moodTags(),
            creativeBrief.narrativeViewpoint(),
            creativeBrief.musicDirection(),
            creativeBrief.yanyunReferences(),
            entityLabels(knowledge),
            creativeBrief.constraints()
                + (creativeBrief.yanyunRewriteSuggestion() == null
                    ? ""
                    : "\nyanyun_rewrite_suggestion=" + creativeBrief.yanyunRewriteSuggestion()),
            creativeBrief.songCore(),
            creativeBrief.singerVoice(),
            creativeBrief.listenerTarget(),
            creativeBrief.emotionalEngine(),
            creativeBrief.chorusJob(),
            creativeBrief.avoidDirection(),
            creativeBrief.creativeCore(),
            creativeBrief.chosenAngle(),
            creativeBrief.alternativeAngles(),
            creativeBrief.antiClicheStrategy(),
            creativeBrief.voiceTexture(),
            creativeBrief.imagePool(),
            creativeBrief.songEnergy(),
            creativeBrief.songThesis(),
            creativeBrief.pov(),
            creativeBrief.centralTension(),
            creativeBrief.emotionalTurn(),
            creativeBrief.chorusFunction(),
            creativeBrief.memoryDevice())
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
                Map.entry(
                    "song_core",
                    creativeBrief == null ? "" : firstNonBlank(creativeBrief.songCore(), "")),
                Map.entry(
                    "singer_voice",
                    creativeBrief == null ? "" : firstNonBlank(creativeBrief.singerVoice(), "")),
                Map.entry(
                    "listener_target",
                    creativeBrief == null ? "" : firstNonBlank(creativeBrief.listenerTarget(), "")),
                Map.entry(
                    "emotional_engine",
                    creativeBrief == null
                        ? ""
                        : firstNonBlank(creativeBrief.emotionalEngine(), "")),
                Map.entry(
                    "chorus_job",
                    creativeBrief == null ? "" : firstNonBlank(creativeBrief.chorusJob(), "")),
                Map.entry(
                    "avoid_direction",
                    creativeBrief == null ? "" : firstNonBlank(creativeBrief.avoidDirection(), "")),
                Map.entry(
                    "creative_core",
                    creativeBrief == null ? "" : firstNonBlank(creativeBrief.creativeCore(), "")),
                Map.entry(
                    "chosen_angle",
                    creativeBrief == null ? "" : firstNonBlank(creativeBrief.chosenAngle(), "")),
                Map.entry(
                    "alternative_angles",
                    creativeBrief == null ? List.of() : creativeBrief.alternativeAngles()),
                Map.entry(
                    "anti_cliche_strategy",
                    creativeBrief == null
                        ? ""
                        : firstNonBlank(creativeBrief.antiClicheStrategy(), "")),
                Map.entry(
                    "voice_texture",
                    creativeBrief == null ? "" : firstNonBlank(creativeBrief.voiceTexture(), "")),
                Map.entry(
                    "image_pool", creativeBrief == null ? List.of() : creativeBrief.imagePool()),
                Map.entry(
                    "song_energy",
                    creativeBrief == null ? "" : firstNonBlank(creativeBrief.songEnergy(), "")),
                Map.entry(
                    "song_thesis",
                    creativeBrief == null ? "" : firstNonBlank(creativeBrief.songThesis(), "")),
                Map.entry(
                    "pov", creativeBrief == null ? "" : firstNonBlank(creativeBrief.pov(), "")),
                Map.entry(
                    "central_tension",
                    creativeBrief == null ? "" : firstNonBlank(creativeBrief.centralTension(), "")),
                Map.entry(
                    "emotional_turn",
                    creativeBrief == null ? "" : firstNonBlank(creativeBrief.emotionalTurn(), "")),
                Map.entry(
                    "chorus_function",
                    creativeBrief == null ? "" : firstNonBlank(creativeBrief.chorusFunction(), "")),
                Map.entry(
                    "memory_device",
                    creativeBrief == null ? "" : firstNonBlank(creativeBrief.memoryDevice(), "")),
                Map.entry("knowledge_base_version", knowledge == null ? "" : knowledge.kbVersion()),
                Map.entry("knowledge_resolved_entities", entityLabels(knowledge)),
                Map.entry("knowledge_reference_names", referenceNames(knowledge)),
                Map.entry("knowledge_reference_summaries", referenceSummaries(knowledge)),
                Map.entry("knowledge_reference_fact_levels", referenceFactLevels(knowledge)),
                Map.entry("knowledge_reference_source_classes", referenceSourceClasses(knowledge)),
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

  private void ensureLightweightEditQualityAllowed(
      LyricsGenerationRequest request, DeepSeekLyricsResponse response) {
    if (response == null || firstNonBlank(response.lyricsText(), "").isBlank()) {
      throw new LyricsQualityException("AI 改词没有返回有效歌词，请稍后重试。");
    }
    String outputText =
        String.join(
            " ",
            firstNonBlank(response.songTitle(), ""),
            firstNonBlank(response.songSummary(), ""),
            firstNonBlank(response.lyricsText(), ""),
            firstNonBlank(response.musicPrompt(), ""),
            firstNonBlank(response.coverPromptSeed(), ""));
    String inputText =
        String.join(
            " ",
            firstNonBlank(request.userInput(), ""),
            firstNonBlank(request.currentLyrics(), ""),
            firstNonBlank(request.instruction(), ""),
            firstNonBlank(request.requestedTitle(), ""));
    if (CreativeBoundaryTerms.containsOtherIpTerm(outputText)
        && !CreativeBoundaryTerms.containsOtherIpTerm(inputText)) {
      throw new LyricsQualityException("AI 改词偏离了燕云十六声创作域，请换一个更明确的润色方向后重试。");
    }
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
            request.mood(),
            request.musicStyle(),
            request.vocalPreference(),
            yanyunReferenceLabels(knowledge));
    long startedAt = System.nanoTime();
    try {
      DeepSeekLyricsResponse response = deepSeekLyricsClient.generate(deepSeekRequest);
      recordAgentRun(request, prompt, deepSeekRequest, response, startedAt, null);
      return response;
    } catch (RuntimeException exception) {
      recordAgentRun(request, prompt, deepSeekRequest, null, startedAt, exception);
      throw new LyricsGenerationTransientException(transientFailureMessage(request), exception);
    }
  }

  private String transientFailureMessage(LyricsGenerationRequest request) {
    return switch (request.operation()) {
      case POLISH -> "AI 润色暂时失败，本次未消耗改词次数，请稍后重试。";
      case CONTINUE -> "AI 续写暂时失败，本次未消耗改词次数，请稍后重试。";
      case LYRICS, INSPIRATION -> "AI 写词暂时失败，本次未创建作品，请稍后重试。";
    };
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
            "v0.11",
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
        result.references().stream().map(KnowledgeReference::chunkId).toList().toString(),
        result.resolvedEntities().stream()
            .map(ResolvedKnowledgeEntity::promptLabel)
            .toList()
            .toString());
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
        nullToEmpty(request.mood()),
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

  private LyricsGenerationRequest rewriteRequest(
      LyricsGenerationRequest request, QualityEvaluationResult quality) {
    return new LyricsGenerationRequest(
        request.userId(),
        request.workId(),
        request.operation(),
        request.userInput(),
        request.currentLyrics(),
        appendInstruction(request.instruction(), rewriteInstruction(quality)),
        request.requestedTitle(),
        request.mood(),
        request.musicStyle(),
        request.vocalPreference());
  }

  private String rewriteInstruction(QualityEvaluationResult quality) {
    String action = recommendedRewriteAction(quality == null ? null : quality.recommendedAction());
    String base =
        "Rewrite once because the previous lyrics did not pass the quality gate. Preserve the user's emotion, music preference, and story core. Keep one clear song thesis: the listener should understand what this song is singing about after one listen. Keep the point of view stable, make each section serve the same central tension, and make the chorus perform a real function rather than simply stacking pretty lines. Make the final lyrics feel like they belong inside the Yanyun Sixteen Sounds world through character choices, place atmosphere, wuxia actions, player experience, or emotional texture. If the request names a resolved Yanyun character, storyline, place, or faction, ground the rewrite in that canonical entity and do not output user typos as official names. Do not force official names or the words Yanyun/Sixteen Sounds. Do not over-heroize ordinary characters. Avoid generic wuxia phrasing and plot-summary writing. For POLISH, do not create a different song: keep the original thesis, POV, emotional arc, and most usable lines unless the user explicitly asked for a major rewrite.";
    String targeted =
        switch (action) {
          case "rewrite_angle" ->
              "Change to a more distinctive creative entry angle. Avoid the first obvious interpretation and choose an angle with stronger song identity.";
          case "rewrite_cliche" ->
              "Remove first-response cliches and generic wuxia phrasing. Replace them with unexpected but truthful actions, objects, voices, or scene details.";
          case "rewrite_voice" ->
              "Unify the singing voice. Make the lyrics sound like a real person or group inside the song is singing, not like an outside narrator summarizing a plot.";
          case "rewrite_memory_point" ->
              "Strengthen the song's memory point. It can be a repeatable line, repeated sentence shape, spoken habit, sound action, image loop, or rhythmic refrain; do not force a slogan.";
          case "rewrite_singability" ->
              "Repair singability: shorter natural lines, clearer rhythmic repetition, more natural rhyme or near-rhyme, and fewer prose-like explanations.";
          case "rewrite_grounding" ->
              "Strengthen Yanyun grounding and user-story fidelity. Use matched knowledge as creative material without turning the lyric into encyclopedia or plot summary.";
          default ->
              "Use the quality gate feedback to make the rewrite less ordinary, more singable, more concrete, and more memorable without locking into a fixed formula.";
        };
    return base + " Targeted rewrite action=" + action + ". " + targeted;
  }

  private String recommendedRewriteAction(String recommendedAction) {
    String normalized = nullToEmpty(recommendedAction);
    for (String action :
        List.of(
            "rewrite_angle",
            "rewrite_cliche",
            "rewrite_voice",
            "rewrite_memory_point",
            "rewrite_singability",
            "rewrite_grounding")) {
      if (normalized.contains(action)) {
        return action;
      }
    }
    return "rewrite_open_quality";
  }

  private LyricsGenerationResult toResult(
      DeepSeekLyricsResponse response,
      CreativeBriefResult creativeBrief,
      KnowledgeRetrievalResult knowledge,
      PromptRenderResult prompt,
      boolean includeCreativeBriefVersion) {
    Map<String, Integer> promptTemplateVersions;
    if (includeCreativeBriefVersion) {
      promptTemplateVersions =
          Map.of(
              prompt.templateKey(),
              prompt.version(),
              CREATIVE_BRIEF_TEMPLATE_KEY,
              CREATIVE_BRIEF_TEMPLATE_VERSION);
    } else {
      promptTemplateVersions = Map.of(prompt.templateKey(), prompt.version());
    }
    return new LyricsGenerationResult(
        response.songTitle(),
        response.songSummary(),
        response.lyricsText(),
        response.musicPrompt(),
        response.coverPromptSeed(),
        response.riskNotes(),
        yanyunReferences(creativeBrief, knowledge),
        knowledge.kbVersion(),
        promptTemplateVersions,
        response.qualityScore());
  }

  private List<String> yanyunReferences(
      CreativeBriefResult creativeBrief, KnowledgeRetrievalResult knowledge) {
    if (creativeBrief != null && !creativeBrief.yanyunReferences().isEmpty()) {
      return creativeBrief.yanyunReferences();
    }
    return knowledge.references().stream().map(KnowledgeReference::displayName).toList();
  }

  private void ensureEntityResolutionAllowed(
      LyricsGenerationRequest request, KnowledgeRetrievalResult knowledge) {
    if (knowledge == null || knowledge.resolvedEntities().isEmpty()) {
      return;
    }
    boolean hasAmbiguousEntity =
        knowledge.resolvedEntities().stream().anyMatch(ResolvedKnowledgeEntity::ambiguous);
    if (hasAmbiguousEntity && looksLikeExplicitEntityRequest(request)) {
      throw new LyricsCreativeDomainException("没能准确识别你提到的燕云角色、剧情、地点或门派。请检查名称，或补充一句描述后重试。", null);
    }
  }

  private boolean looksLikeExplicitEntityRequest(LyricsGenerationRequest request) {
    String query = query(request);
    return query.contains("燕云十六声中")
        || query.contains("游戏中")
        || query.contains("游戏里")
        || query.contains("角色歌")
        || query.contains("人生经历")
        || query.contains("剧情歌")
        || query.contains("任务线")
        || query.contains("门派")
        || query.contains("势力");
  }

  private List<String> yanyunReferenceLabels(KnowledgeRetrievalResult knowledge) {
    Set<String> labels = new LinkedHashSet<>();
    labels.addAll(entityLabels(knowledge));
    labels.addAll(referenceNames(knowledge));
    return new ArrayList<>(labels).stream().limit(12).toList();
  }

  private List<String> entityLabels(KnowledgeRetrievalResult knowledge) {
    if (knowledge == null) {
      return List.of();
    }
    return knowledge.resolvedEntities().stream().map(ResolvedKnowledgeEntity::promptLabel).toList();
  }

  private List<String> referenceNames(KnowledgeRetrievalResult knowledge) {
    if (knowledge == null) {
      return List.of();
    }
    return knowledge.references().stream().map(KnowledgeReference::displayName).toList();
  }

  private List<String> referenceSummaries(KnowledgeRetrievalResult knowledge) {
    if (knowledge == null) {
      return List.of();
    }
    return knowledge.references().stream()
        .map(reference -> reference.displayName() + ": " + trimToLength(reference.content(), 320))
        .toList();
  }

  private List<String> referenceFactLevels(KnowledgeRetrievalResult knowledge) {
    if (knowledge == null) {
      return List.of();
    }
    return knowledge.references().stream().map(KnowledgeReference::factLevel).toList();
  }

  private List<String> referenceSourceClasses(KnowledgeRetrievalResult knowledge) {
    if (knowledge == null) {
      return List.of();
    }
    return knowledge.references().stream()
        .map(reference -> reference.sourceClass().name())
        .toList();
  }

  private boolean isLowQuality(DeepSeekLyricsResponse response) {
    return response.qualityScore() != null
        && response.qualityScore().compareTo(QUALITY_REWRITE_THRESHOLD) < 0;
  }

  private boolean lightweightEditOperation(LyricsOperation operation) {
    return operation == LyricsOperation.POLISH || operation == LyricsOperation.CONTINUE;
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

  private String trimToLength(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value == null ? "" : value;
    }
    return value.substring(0, Math.max(0, maxLength)) + "...";
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
