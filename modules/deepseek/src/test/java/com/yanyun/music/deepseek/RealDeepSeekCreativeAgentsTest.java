package com.yanyun.music.deepseek;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.creativeagent.CoverPromptRequest;
import com.yanyun.music.creativeagent.CoverPromptResult;
import com.yanyun.music.creativeagent.CreativeBriefRequest;
import com.yanyun.music.creativeagent.CreativeBriefResult;
import com.yanyun.music.creativeagent.CreativeDomainDecision;
import com.yanyun.music.creativeagent.MusicPromptRequest;
import com.yanyun.music.creativeagent.MusicPromptResult;
import com.yanyun.music.creativeagent.QualityDecision;
import com.yanyun.music.creativeagent.QualityEvaluationRequest;
import com.yanyun.music.creativeagent.QualityEvaluationResult;
import com.yanyun.music.creativeagent.QualityGate;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RealDeepSeekCreativeAgentsTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private HttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void creativeBriefRejectsUnrelatedIpBeforeCallingDeepSeek() throws IOException {
    AtomicInteger requestCount = new AtomicInteger();
    server =
        startServer(
            exchange -> {
              requestCount.incrementAndGet();
              respondJson(exchange, 200, chatResponse(Map.of("domain_decision", "PASS")));
            });
    List<AgentRunRecord> records = new ArrayList<>();
    RealDeepSeekCreativeBriefAgent agent =
        new RealDeepSeekCreativeBriefAgent(client(), records::add);

    CreativeBriefResult result =
        agent.generate(
            new CreativeBriefRequest(
                "user-1",
                "work-1",
                "INSPIRATION",
                "写一首高达歌",
                null,
                null,
                null,
                null,
                null,
                List.of()));

    assertEquals(CreativeDomainDecision.REJECT, result.domainDecision());
    assertEquals(0, requestCount.get());
    assertEquals("CreativeBriefAgent", records.getFirst().agentName());
    assertEquals("v0.7", records.getFirst().agentVersion());
  }

  @Test
  void creativeBriefAddsFallbackYanyunReferencesWhenModelLeavesThemEmpty() throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        mapOf(
                            "domain_decision",
                            "PASS",
                            "creative_intent",
                            "玩家想写雁门风雪里的江湖归来。",
                            "theme",
                            "雁门埋刀",
                            "mood_tags",
                            List.of("苍凉", "释然"),
                            "narrative_viewpoint",
                            "player-facing",
                            "music_direction",
                            "cinematic Chinese folk ballad",
                            "yanyun_references",
                            List.of(),
                            "constraints",
                            List.of("must remain Yanyun-related"),
                            "risk_notes",
                            List.of(),
                            "creative_core",
                            "一个无名游侠在风雪里选择把刀埋下。",
                            "chosen_angle",
                            "不写胜利，写放下武器后的余温。",
                            "alternative_angles",
                            List.of("边关旧友", "雪夜归路"),
                            "anti_cliche_strategy",
                            "避开风雪孤刀的第一反应，用埋刀动作写人。",
                            "voice_texture",
                            "克制、苍凉、低声",
                            "image_pool",
                            List.of("埋刀", "雪线", "旧马蹄"),
                            "song_energy",
                            "收束后回响",
                            "freeform_opportunities",
                            List.of()))));
    RealDeepSeekCreativeBriefAgent agent =
        new RealDeepSeekCreativeBriefAgent(client(), new ArrayList<AgentRunRecord>()::add);

    CreativeBriefResult result =
        agent.generate(
            new CreativeBriefRequest(
                "user-1",
                "work-1",
                "INSPIRATION",
                "我是燕云十六声玩家，想写一首雁门风雪里无名游侠埋刀归来的歌。",
                null,
                null,
                null,
                null,
                null,
                List.of()));

    assertTrue(result.yanyunReferences().contains("雁门关外风雪"));
    assertTrue(result.yanyunReferences().contains("燕云十六声玩家故事"));
    assertTrue(result.yanyunReferences().contains("江湖游侠心境"));
    assertTrue(result.creativeCore().contains("无名游侠"));
    assertTrue(result.alternativeAngles().contains("边关旧友"));
    assertTrue(result.imagePool().contains("埋刀"));
  }

  @Test
  void creativeBriefPromptUsesOpenSongcraftInsteadOfFixedTemplate() throws IOException {
    AtomicReference<JsonNode> capturedBody = new AtomicReference<>();
    server =
        startServer(
            exchange -> {
              capturedBody.set(
                  objectMapper.readTree(
                      new String(
                          exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
              respondJson(
                  exchange,
                  200,
                  chatResponse(
                      mapOf(
                          "domain_decision",
                          "PASS",
                          "creative_intent",
                          "普通玩家想写市井里仍愿出手的小人物。",
                          "theme",
                          "市井小侠",
                          "mood_tags",
                          List.of("克制", "热血"),
                          "narrative_viewpoint",
                          "市井小人物第一人称",
                          "music_direction",
                          "国风流行",
                          "yanyun_references",
                          List.of("开封夜市"),
                          "constraints",
                          List.of("preserve user story"),
                          "risk_notes",
                          List.of(),
                          "creative_core",
                          "普通人不是成为英雄，而是在摊灯下仍然愿意伸手。",
                          "chosen_angle",
                          "从摊贩手上的油烟和迟疑进入。",
                          "alternative_angles",
                          List.of("夜市群像", "旧门派余波"),
                          "anti_cliche_strategy",
                          "不写大侠出场，写他放下葱油饼后还是回头。",
                          "voice_texture",
                          "市井、低声、带一点倔",
                          "image_pool",
                          List.of("摊灯", "葱油饼", "袖口油渍"),
                          "song_energy",
                          "低烧后抬头",
                          "freeform_opportunities",
                          List.of())));
            });
    RealDeepSeekCreativeBriefAgent agent =
        new RealDeepSeekCreativeBriefAgent(client(), new ArrayList<AgentRunRecord>()::add);

    agent.generate(
        new CreativeBriefRequest(
            "user-1",
            "work-1",
            "INSPIRATION",
            "一个普通小摊贩遇到不平事还是会出手",
            null,
            null,
            null,
            "国风流行",
            null,
            List.of("开封夜市")));

    String systemPrompt = capturedBody.get().path("messages").get(0).path("content").asText();
    assertTrue(systemPrompt.contains("不按曲风套写法"));
    assertTrue(systemPrompt.contains("不强制每首歌都有同一种结构"));
    assertTrue(systemPrompt.contains("不强制一定有金句式 hook"));
    assertTrue(systemPrompt.contains("creative_core"));
    assertTrue(systemPrompt.contains("chosen_angle"));
    assertTrue(systemPrompt.contains("alternative_angles"));
    assertTrue(systemPrompt.contains("anti_cliche_strategy"));
    assertTrue(systemPrompt.contains("voice_texture"));
    assertTrue(systemPrompt.contains("image_pool"));
    assertTrue(systemPrompt.contains("song_energy"));
    assertTrue(systemPrompt.contains("侠=自由、离别=月光、江湖=风雨、少年=逍遥"));
    assertTrue(systemPrompt.contains("不强行改成官方角色歌"));
  }

  @Test
  void musicPromptSanitizesRealSingerReferencesFromModelOutput() throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "title",
                            "燕云行",
                            "lyrics_with_structure_tags",
                            "[Verse]\n燕云路远",
                            "style_prompt",
                            "周杰伦 仿唱 国风 R&B 轻说唱",
                            "exclude_prompt",
                            "no Jay Chou vocal clone",
                            "provider_options",
                            Map.of("provider_profile", "SUNO")))));
    RealDeepSeekMusicPromptAgent agent =
        new RealDeepSeekMusicPromptAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    MusicPromptResult result =
        agent.generate(
            new MusicPromptRequest(
                "work-1", "燕云行", "summary", "[Verse]\n燕云路远", "周杰伦风格", null, "SUNO"));

    assertTrue(result.musicPrompt().contains("Chinese pop R&B"));
    assertFalse(result.musicPrompt().contains("周杰伦"));
    assertFalse(result.musicPrompt().contains("仿唱"));
    assertFalse(result.excludePrompt().toLowerCase().contains("jay chou"));
  }

  @Test
  void musicPromptParsesJsonObjectWrappedInMarkdownFence() throws IOException {
    String content =
        """
        ```json
        {
          "title": "燕云行",
          "lyrics_with_structure_tags": "[Verse]\\n燕云路远",
          "style_prompt": "cinematic Chinese folk ballad with warm female vocal",
          "exclude_prompt": "no vocal clone",
          "provider_options": {"provider_profile": "SUNO"}
        }
        ```
        """;
    server = startServer(exchange -> respondJson(exchange, 200, chatResponseContent(content)));
    RealDeepSeekMusicPromptAgent agent =
        new RealDeepSeekMusicPromptAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    MusicPromptResult result =
        agent.generate(
            new MusicPromptRequest(
                "work-1", "燕云行", "summary", "[Verse]\n燕云路远", "国风民谣", null, "SUNO"));

    assertEquals("燕云行", result.title());
    assertTrue(result.musicPrompt().contains("Chinese folk ballad"));
  }

  @Test
  void musicPromptRetriesOnceWhenDeepSeekReturnsEmptyContent() throws IOException {
    AtomicInteger requestCount = new AtomicInteger();
    server =
        startServer(
            exchange -> {
              if (requestCount.incrementAndGet() == 1) {
                respondJson(exchange, 200, chatResponseContent(""));
                return;
              }
              respondJson(
                  exchange,
                  200,
                  chatResponse(
                      Map.of(
                          "title",
                          "燕云行",
                          "lyrics_with_structure_tags",
                          "[Verse]\\n燕云路远",
                          "style_prompt",
                          "cinematic Chinese folk ballad with warm female vocal",
                          "exclude_prompt",
                          "no vocal clone",
                          "provider_options",
                          Map.of("provider_profile", "SUNO"))));
            });
    RealDeepSeekMusicPromptAgent agent =
        new RealDeepSeekMusicPromptAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    MusicPromptResult result =
        agent.generate(
            new MusicPromptRequest(
                "work-1", "燕云行", "summary", "[Verse]\n燕云路远", "国风民谣", null, "SUNO"));

    assertEquals(2, requestCount.get());
    assertEquals("燕云行", result.title());
    assertTrue(result.musicPrompt().contains("Chinese folk ballad"));
  }

  @Test
  void jsonChatClientExtractsFirstJsonObjectFromWrappedContent() {
    JsonNode root =
        new DeepSeekJsonChatClient(
                properties(URI.create("http://127.0.0.1"), true, true), objectMapper)
            .parseContentJson(
                "好的，下面是 JSON： {\"title\":\"燕云行\",\"nested\":{\"brace\":\"{ok}\"}} 完成。");

    assertEquals("燕云行", root.path("title").asText());
    assertEquals("{ok}", root.path("nested").path("brace").asText());
  }

  @Test
  void coverPromptAllowsOnlyControlledSongTitleText() throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "visual_prompt",
                            "premium 16:9 album cover with clear Chinese title typography",
                            "text_prompt",
                            "Use only the song title as main title",
                            "negative_prompt",
                            "fake singer, fake copyright, watermark",
                            "width",
                            1920,
                            "height",
                            1080,
                            "typography_requirements",
                            List.of("clear readable Chinese title")))));
    RealDeepSeekCoverPromptAgent agent =
        new RealDeepSeekCoverPromptAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    CoverPromptResult result =
        agent.generate(
            new CoverPromptRequest(
                "work-1", "燕云行", "summary", "lyrics", "style", "seed", 1920, 1080));

    assertTrue(result.visualPrompt().contains("燕云行"));
    assertTrue(result.visualPrompt().contains("exactly one text element"));
    assertTrue(result.negativePrompt().contains("extra text beyond the exact song title"));
    assertEquals(
        "CONTROLLED_TITLE_TEXT: render only the exact song title \"燕云行\". No other text.",
        result.textPrompt());
    assertEquals("CONTROLLED_TITLE_TEXT", result.providerOptions().get("text_policy"));
    assertEquals("燕云行", result.providerOptions().get("allowed_title"));
    assertEquals(1920, result.width());
    assertEquals(1080, result.height());
  }

  @Test
  void coverPromptUsesConfiguredMaxTokensAndTrimsLongLyricsInput() throws IOException {
    AtomicReference<JsonNode> capturedBody = new AtomicReference<>();
    server =
        startServer(
            exchange -> {
              capturedBody.set(
                  objectMapper.readTree(
                      new String(
                          exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
              respondJson(
                  exchange,
                  200,
                  chatResponse(
                      Map.of(
                          "visual_prompt",
                          "premium 16:9 album cover with clear Chinese title typography",
                          "text_prompt",
                          "Use only the song title as main title",
                          "negative_prompt",
                          "fake singer, fake copyright, watermark",
                          "width",
                          1920,
                          "height",
                          1080)));
            });
    DeepSeekProperties properties = properties(serverBaseUri(), true, true);
    properties.setResponseMaxTokens(4096);
    RealDeepSeekCoverPromptAgent agent =
        new RealDeepSeekCoverPromptAgent(
            new DeepSeekJsonChatClient(properties, objectMapper),
            objectMapper,
            new ArrayList<AgentRunRecord>()::add);

    CoverPromptResult result =
        agent.generate(
            new CoverPromptRequest(
                "work-1", "燕云行", "summary", "燕云".repeat(3000), "style", "seed", 1920, 1080));

    String userPrompt = capturedBody.get().path("messages").get(1).path("content").asText();
    assertEquals(4096, capturedBody.get().path("max_tokens").asInt());
    assertTrue(userPrompt.contains("lyrics_excerpt="));
    assertFalse(userPrompt.contains("lyrics_text="));
    assertTrue(userPrompt.length() < 3800);
    assertEquals(1920, result.width());
  }

  @Test
  void qualityAgentLocalSafetyRejectsSingerImitationEvenWhenModelPasses() throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "gate",
                            "MUSIC",
                            "decision",
                            "PASS",
                            "score",
                            95,
                            "reasons",
                            List.of(),
                            "recommended_action",
                            "PASS",
                            "retryable",
                            false))));
    RealDeepSeekQualityEvaluationAgent agent =
        new RealDeepSeekQualityEvaluationAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    QualityEvaluationResult result =
        agent.evaluate(
            new QualityEvaluationRequest(
                "work-1",
                QualityGate.MUSIC,
                "燕云行",
                "燕云路远，十六州风雪未歇",
                "SUNO",
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
                Map.of("music_prompt", "周杰伦 仿唱")));

    assertEquals(QualityDecision.REWRITE, result.decision());
  }

  @Test
  void qualityAgentDoesNotRejectLyricsOnlyBecauseTheyLackLiteralYanyunAnchor() throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "gate",
                            "LYRICS",
                            "decision",
                            "PASS",
                            "score",
                            92,
                            "reasons",
                            List.of(),
                            "recommended_action",
                            "PASS",
                            "retryable",
                            false))));
    RealDeepSeekQualityEvaluationAgent agent =
        new RealDeepSeekQualityEvaluationAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    QualityEvaluationResult result =
        agent.evaluate(
            new QualityEvaluationRequest(
                "work-1",
                QualityGate.LYRICS,
                "江湖月",
                "[Verse]\n江湖夜雨十年灯\n孤舟一叶过长风",
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
                Map.of(
                    "song_summary", "一个游侠在雨夜告别旧梦。",
                    "music_prompt", "cinematic Chinese folk ballad",
                    "cover_prompt_seed", "moonlit river and lone boat")));

    assertEquals(QualityDecision.PASS, result.decision());
  }

  @Test
  void qualityAgentLyricsPromptChecksOpenCraftAndMemoryPoint() throws IOException {
    AtomicReference<JsonNode> capturedBody = new AtomicReference<>();
    server =
        startServer(
            exchange -> {
              capturedBody.set(
                  objectMapper.readTree(
                      new String(
                          exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
              respondJson(
                  exchange,
                  200,
                  chatResponse(
                      Map.of(
                          "gate",
                          "LYRICS",
                          "decision",
                          "PASS",
                          "score",
                          86,
                          "reasons",
                          List.of(),
                          "recommended_action",
                          "PASS",
                          "retryable",
                          false)));
            });
    RealDeepSeekQualityEvaluationAgent agent =
        new RealDeepSeekQualityEvaluationAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    agent.evaluate(
        new QualityEvaluationRequest(
            "work-1",
            QualityGate.LYRICS,
            "我还会出手",
            "[Chorus]\n别问我为何还不肯收手\n锈剑在腰间 也算旧友",
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
            Map.of("context", "lyrics quality smoke")));

    String systemPrompt = capturedBody.get().path("messages").get(0).path("content").asText();
    assertTrue(systemPrompt.contains("不要用固定模板审稿"));
    assertTrue(systemPrompt.contains("是否值得被唱"));
    assertTrue(systemPrompt.contains("摆脱第一反应俗套"));
    assertTrue(systemPrompt.contains("声音记忆点不限定为金句"));
    assertTrue(systemPrompt.contains("完整但普通"));
    assertTrue(systemPrompt.contains("每句都对但没有一处让人想再听"));
    assertTrue(systemPrompt.contains("副歌主韵脚或节奏回环"));
    assertTrue(systemPrompt.contains("连续堆叠成填充词"));
    assertTrue(systemPrompt.contains("rewrite_angle"));
    assertTrue(systemPrompt.contains("rewrite_cliche"));
    assertTrue(systemPrompt.contains("rewrite_voice"));
    assertTrue(systemPrompt.contains("rewrite_memory_point"));
    assertTrue(systemPrompt.contains("rewrite_singability"));
    assertTrue(systemPrompt.contains("rewrite_grounding"));
    assertTrue(systemPrompt.contains("knowledge_resolved_entities"));
    assertTrue(systemPrompt.contains("点名角色但歌词只是泛江湖"));
    assertTrue(systemPrompt.contains("错字/非标准称呼"));
  }

  @Test
  void qualityAgentCanRewriteCompleteButOrdinaryLyricsWithMemoryPointAction() throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "gate",
                            "LYRICS",
                            "decision",
                            "REWRITE",
                            "score",
                            76,
                            "reasons",
                            List.of("题材正确但入口普通，副歌没有能留下来的声音记忆点。"),
                            "recommended_action",
                            "rewrite_memory_point",
                            "retryable",
                            true))));
    RealDeepSeekQualityEvaluationAgent agent =
        new RealDeepSeekQualityEvaluationAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    QualityEvaluationResult result =
        agent.evaluate(
            new QualityEvaluationRequest(
                "work-1",
                QualityGate.LYRICS,
                "天地任逍遥",
                "[Chorus]\n你看那天高地也阔\n云自飘来水自流\n我就这么快乐地游",
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
                Map.of("song_summary", "一个市井少年选择成为游侠。")));

    assertEquals(QualityDecision.REWRITE, result.decision());
    assertEquals("rewrite_memory_point", result.recommendedAction());
    assertEquals(76, result.score());
  }

  @Test
  void qualityAgentLocalSafetyRejectsCorrectedEntityTypoInLyrics() throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "gate",
                            "LYRICS",
                            "decision",
                            "PASS",
                            "score",
                            90,
                            "reasons",
                            List.of(),
                            "recommended_action",
                            "PASS",
                            "retryable",
                            false))));
    RealDeepSeekQualityEvaluationAgent agent =
        new RealDeepSeekQualityEvaluationAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    QualityEvaluationResult result =
        agent.evaluate(
            new QualityEvaluationRequest(
                "work-1",
                QualityGate.LYRICS,
                "姜无浪",
                "[Chorus]\n姜无浪把门留在竹林旁",
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
                Map.of(
                    "song_summary",
                    "姜无浪的人生经历。",
                    "knowledge_resolved_entities",
                    List.of("character/江无浪 matched=姜无浪 kind=fuzzy confidence=0.92"))));

    assertEquals(QualityDecision.REWRITE, result.decision());
    assertTrue(result.reasons().getFirst().contains("错字"));
  }

  @Test
  void qualityAgentLocalSafetyAllowsControlledCoverTitleTypography() throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "gate",
                            "COVER",
                            "decision",
                            "BLOCK",
                            "score",
                            40,
                            "reasons",
                            List.of(
                                "Model noticed title typography and conservative policy marked it risky."),
                            "recommended_action",
                            "REWRITE_AGENT_OUTPUT",
                            "retryable",
                            false))));
    RealDeepSeekQualityEvaluationAgent agent =
        new RealDeepSeekQualityEvaluationAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    QualityEvaluationResult result =
        agent.evaluate(
            new QualityEvaluationRequest(
                "work-1",
                QualityGate.COVER,
                "燕云行",
                "lyrics",
                null,
                null,
                null,
                null,
                1920,
                1080,
                null,
                null,
                null,
                null,
                null,
                Map.of(
                    "visual_prompt",
                    "premium 16:9 album cover with exactly one text element: only the exact song title 燕云行 as clear Chinese title typography",
                    "text_prompt",
                    "CONTROLLED_TITLE_TEXT: render only the exact song title \"燕云行\". No other text.",
                    "negative_prompt",
                    "low quality, fake singer name, fake label, fake copyright, watermark, UI, garbled text",
                    "provider_options",
                    Map.of("text_policy", "CONTROLLED_TITLE_TEXT", "allowed_title", "燕云行"),
                    "typography_requirements",
                    List.of("clear readable Chinese title", "no fake singer credits"))));

    assertEquals(QualityDecision.PASS, result.decision());
    assertFalse(result.retryable());
  }

  @Test
  void qualityAgentLocalSafetyAllowsControlledCoverTitleReasonWhenModelRewrites()
      throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "gate",
                            "COVER",
                            "decision",
                            "REWRITE",
                            "score",
                            65,
                            "reasons",
                            List.of("封面提示词仅允许作品歌名主标题，无违规文本元素，符合规则"),
                            "recommended_action",
                            "REWRITE_AGENT_OUTPUT",
                            "retryable",
                            true))));
    RealDeepSeekQualityEvaluationAgent agent =
        new RealDeepSeekQualityEvaluationAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    QualityEvaluationResult result =
        agent.evaluate(
            new QualityEvaluationRequest(
                "work-1",
                QualityGate.COVER,
                "宫灯看剑",
                "lyrics",
                null,
                null,
                null,
                null,
                1920,
                1080,
                null,
                null,
                null,
                null,
                null,
                Map.of(
                    "visual_prompt",
                    "premium cinematic album cover with clear Chinese title typography",
                    "negative_prompt",
                    "low quality, fake singer name, fake label, fake copyright, watermark, UI, garbled text")));

    assertEquals(QualityDecision.PASS, result.decision());
    assertEquals(80, result.score());
    assertFalse(result.retryable());
  }

  @Test
  void qualityAgentLocalSafetyAllowsNoTextCoverPolicyWhenModelFlagsItAsRewrite()
      throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "gate",
                            "COVER",
                            "decision",
                            "REWRITE",
                            "score",
                            65,
                            "reasons",
                            List.of(
                                "Cover prompt explicitly disallows any text, signatures, watermarks, or fake artist/label info, matching guidelines."),
                            "recommended_action",
                            "REWRITE_AGENT_OUTPUT",
                            "retryable",
                            true))));
    RealDeepSeekQualityEvaluationAgent agent =
        new RealDeepSeekQualityEvaluationAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    QualityEvaluationResult result =
        agent.evaluate(
            new QualityEvaluationRequest(
                "work-1",
                QualityGate.COVER,
                "燕云行",
                "lyrics",
                null,
                null,
                null,
                null,
                1920,
                1080,
                null,
                null,
                null,
                null,
                null,
                Map.of(
                    "visual_prompt",
                    "premium 16:9 album cover, no text in the image, no Chinese characters",
                    "negative_prompt",
                    "low quality, text, Chinese characters, fake singer name",
                    "text_policy",
                    "NO_TEXT_IN_IMAGE")));

    assertEquals(QualityDecision.PASS, result.decision());
    assertEquals(80, result.score());
    assertFalse(result.retryable());
  }

  @Test
  void qualityAgentLocalSafetyAllowsChineseNoTextCoverPolicyReason() throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "gate",
                            "COVER",
                            "decision",
                            "REWRITE",
                            "score",
                            65,
                            "reasons",
                            List.of("封面 prompt 明确禁止任何文字、歌手名、版权信息，无违规元素；画面构图预留了后期文字叠加空间，符合排版设计要求"),
                            "recommended_action",
                            "REWRITE_AGENT_OUTPUT",
                            "retryable",
                            true))));
    RealDeepSeekQualityEvaluationAgent agent =
        new RealDeepSeekQualityEvaluationAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    QualityEvaluationResult result =
        agent.evaluate(
            new QualityEvaluationRequest(
                "work-1",
                QualityGate.COVER,
                "燕云行",
                "lyrics",
                null,
                null,
                null,
                null,
                1920,
                1080,
                null,
                null,
                null,
                null,
                null,
                Map.of(
                    "visual_prompt",
                    "premium 16:9 album cover, no text in the image, no Chinese characters",
                    "negative_prompt",
                    "low quality, text, Chinese characters, fake singer name",
                    "text_policy",
                    "NO_TEXT_IN_IMAGE")));

    assertEquals(QualityDecision.PASS, result.decision());
    assertFalse(result.retryable());
  }

  @Test
  void qualityAgentLocalSafetyAllowsNoTextCoverPolicyWithNonStandardModelDecision()
      throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "gate",
                            "COVER",
                            "decision",
                            "REWRITE_AGENT_OUTPUT",
                            "score",
                            65,
                            "reasons",
                            List.of("封面prompt禁止文字，未要求假歌手/版权/水印等违规内容"),
                            "recommended_action",
                            "REWRITE_AGENT_OUTPUT",
                            "retryable",
                            true))));
    RealDeepSeekQualityEvaluationAgent agent =
        new RealDeepSeekQualityEvaluationAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    QualityEvaluationResult result =
        agent.evaluate(
            new QualityEvaluationRequest(
                "work-1",
                QualityGate.COVER,
                "燕云行",
                "lyrics",
                null,
                null,
                null,
                null,
                1920,
                1080,
                null,
                null,
                null,
                null,
                null,
                Map.of(
                    "visual_prompt",
                    "premium 16:9 album cover, no text in the image, no Chinese characters",
                    "negative_prompt",
                    "low quality, text, Chinese characters, fake singer name",
                    "text_policy",
                    "NO_TEXT_IN_IMAGE")));

    assertEquals(QualityDecision.PASS, result.decision());
    assertFalse(result.retryable());
  }

  @Test
  void qualityAgentLocalSafetyRejectsPositiveFakeCoverCreditsEvenWhenModelPasses()
      throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "gate",
                            "COVER",
                            "decision",
                            "PASS",
                            "score",
                            92,
                            "reasons",
                            List.of(),
                            "recommended_action",
                            "PASS",
                            "retryable",
                            false))));
    RealDeepSeekQualityEvaluationAgent agent =
        new RealDeepSeekQualityEvaluationAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    QualityEvaluationResult result =
        agent.evaluate(
            new QualityEvaluationRequest(
                "work-1",
                QualityGate.COVER,
                "燕云行",
                "lyrics",
                null,
                null,
                null,
                null,
                1920,
                1080,
                null,
                null,
                null,
                null,
                null,
                Map.of("visual_prompt", "premium album cover with fake singer credits")));

    assertEquals(QualityDecision.BLOCK, result.decision());
    assertFalse(result.retryable());
  }

  private DeepSeekJsonChatClient client() {
    return new DeepSeekJsonChatClient(properties(serverBaseUri(), true, true), objectMapper);
  }

  private DeepSeekProperties properties(
      URI baseUrl, boolean realCallsEnabled, boolean agentEnabled) {
    DeepSeekProperties properties = new DeepSeekProperties();
    properties.setBaseUrl(baseUrl);
    properties.setApiKey("test-api-key");
    properties.setModelName("deepseek-v4-pro");
    properties.setRealCallsEnabled(realCallsEnabled);
    properties.setAgentRealCallsEnabled(agentEnabled);
    properties.setRequestTimeout(Duration.ofSeconds(5));
    return properties;
  }

  private HttpServer startServer(ExchangeHandler handler) throws IOException {
    HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    httpServer.createContext(
        "/chat/completions",
        exchange -> {
          try {
            handler.handle(exchange);
          } finally {
            exchange.close();
          }
        });
    httpServer.start();
    return httpServer;
  }

  private URI serverBaseUri() {
    return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
  }

  private String chatResponse(Map<String, Object> contentFields) throws IOException {
    String content = objectMapper.writeValueAsString(contentFields);
    return chatResponseContent(content);
  }

  private Map<String, Object> mapOf(Object... keysAndValues) {
    if (keysAndValues.length % 2 != 0) {
      throw new IllegalArgumentException("Expected key/value pairs");
    }
    Map<String, Object> result = new LinkedHashMap<>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      result.put((String) keysAndValues[i], keysAndValues[i + 1]);
    }
    return result;
  }

  private String chatResponseContent(String content) throws IOException {
    return objectMapper.writeValueAsString(
        Map.of(
            "id",
            "chatcmpl-test",
            "choices",
            List.of(Map.of("message", Map.of("content", content)))));
  }

  private void respondJson(
      com.sun.net.httpserver.HttpExchange exchange, int statusCode, String body)
      throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(statusCode, bytes.length);
    exchange.getResponseBody().write(bytes);
  }

  @FunctionalInterface
  private interface ExchangeHandler {
    void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException;
  }
}
