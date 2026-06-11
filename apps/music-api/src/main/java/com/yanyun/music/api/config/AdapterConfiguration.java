package com.yanyun.music.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.agentruntime.AgentRunRecorder;
import com.yanyun.music.auth.AccountAdapter;
import com.yanyun.music.auth.MockAccountAdapter;
import com.yanyun.music.configcenter.CompanyIntegrationProperties;
import com.yanyun.music.configcenter.IntegrationReadinessService;
import com.yanyun.music.creativeagent.CoverPromptAgent;
import com.yanyun.music.creativeagent.CreativeBriefAgent;
import com.yanyun.music.creativeagent.MockCoverPromptAgent;
import com.yanyun.music.creativeagent.MockCreativeBriefAgent;
import com.yanyun.music.creativeagent.MockModerationAgent;
import com.yanyun.music.creativeagent.MockMusicPromptAgent;
import com.yanyun.music.creativeagent.MockQualityEvaluationAgent;
import com.yanyun.music.creativeagent.ModerationAgent;
import com.yanyun.music.creativeagent.MusicPromptAgent;
import com.yanyun.music.creativeagent.QualityEvaluationAgent;
import com.yanyun.music.deepseek.DeepSeekLyricsClient;
import com.yanyun.music.deepseek.DeepSeekProperties;
import com.yanyun.music.deepseek.MockDeepSeekLyricsClient;
import com.yanyun.music.deepseek.RealDeepSeekCoverPromptAgent;
import com.yanyun.music.deepseek.RealDeepSeekCreativeBriefAgent;
import com.yanyun.music.deepseek.RealDeepSeekLyricsClient;
import com.yanyun.music.deepseek.RealDeepSeekMusicPromptAgent;
import com.yanyun.music.deepseek.RealDeepSeekQualityEvaluationAgent;
import com.yanyun.music.dreammaker.DreamMakerClient;
import com.yanyun.music.dreammaker.DreamMakerHttpClient;
import com.yanyun.music.dreammaker.DreamMakerProperties;
import com.yanyun.music.knowledge.KnowledgeService;
import com.yanyun.music.knowledge.NoopKnowledgeService;
import com.yanyun.music.lyrics.DefaultLyricsGenerationService;
import com.yanyun.music.lyrics.LyricsGenerationService;
import com.yanyun.music.minimax.MiniMaxMusicProvider;
import com.yanyun.music.minimax.MiniMaxMusicProviderOptions;
import com.yanyun.music.moderation.MockModerationAdapter;
import com.yanyun.music.moderation.ModerationAdapter;
import com.yanyun.music.musicprovider.MockMusicProvider;
import com.yanyun.music.musicprovider.MusicProvider;
import com.yanyun.music.musicprovider.MusicProviderRegistry;
import com.yanyun.music.musicprovider.MusicProviderSelection;
import com.yanyun.music.production.JdbcAgentRunRecorder;
import com.yanyun.music.prompt.MockPromptTemplateService;
import com.yanyun.music.prompt.PromptTemplateService;
import com.yanyun.music.publish.MockPublishAdapter;
import com.yanyun.music.publish.PublishAdapter;
import com.yanyun.music.quota.MockQuotaAdapter;
import com.yanyun.music.quota.QuotaAdapter;
import com.yanyun.music.storage.HttpRemoteObjectImporter;
import com.yanyun.music.storage.ObjectStorageClient;
import com.yanyun.music.storage.ObjectStorageClientFactory;
import com.yanyun.music.storage.ObjectStorageProperties;
import com.yanyun.music.storage.RemoteObjectImporter;
import com.yanyun.music.suno.SunoMusicProvider;
import com.yanyun.music.suno.SunoMusicProviderOptions;
import com.yanyun.music.suno.YunwuProperties;
import com.yanyun.music.suno.YunwuSunoMusicProvider;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AdapterConfiguration {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  AccountAdapter accountAdapter() {
    return new MockAccountAdapter();
  }

  @Bean
  QuotaAdapter quotaAdapter(
      @Value("${yanyun.quota.mock-max-generate-locks:999}") int mockMaxGenerateLocks) {
    return new MockQuotaAdapter(mockMaxGenerateLocks);
  }

  @Bean
  ModerationAdapter moderationAdapter(
      @Value("${yanyun.moderation.mock-publish-package-blocked-user-ids:}")
          String publishPackageBlockedUserIds,
      @Value("${yanyun.moderation.mock-publish-package-blocked-work-ids:}")
          String publishPackageBlockedWorkIds) {
    return new MockModerationAdapter(
        splitCsv(publishPackageBlockedUserIds), splitCsv(publishPackageBlockedWorkIds));
  }

  @Bean
  PublishAdapter publishAdapter(@Value("${yanyun.storage.environment:local}") String environment) {
    return new MockPublishAdapter(environment);
  }

  private Set<String> splitCsv(String value) {
    if (value == null || value.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(value.split(","))
        .filter(item -> item != null && !item.isBlank())
        .map(String::trim)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Bean
  KnowledgeService knowledgeService() {
    return new NoopKnowledgeService();
  }

  @Bean
  PromptTemplateService promptTemplateService() {
    return new MockPromptTemplateService();
  }

  @Bean
  @ConfigurationProperties(prefix = "yanyun.deepseek")
  DeepSeekProperties deepSeekProperties() {
    return new DeepSeekProperties();
  }

  @Bean
  DeepSeekLyricsClient deepSeekLyricsClient(
      DeepSeekProperties deepSeekProperties, ObjectMapper objectMapper) {
    if (deepSeekProperties.isRealCallsEnabled()) {
      return new RealDeepSeekLyricsClient(deepSeekProperties, objectMapper);
    }
    return new MockDeepSeekLyricsClient();
  }

  @Bean
  AgentRunRecorder agentRunRecorder(JdbcTemplate jdbcTemplate) {
    return new JdbcAgentRunRecorder(jdbcTemplate);
  }

  @Bean
  MusicPromptAgent musicPromptAgent(
      DeepSeekProperties deepSeekProperties,
      ObjectMapper objectMapper,
      AgentRunRecorder agentRunRecorder) {
    if (deepSeekProperties.isRealCallsEnabled()) {
      return new RealDeepSeekMusicPromptAgent(deepSeekProperties, objectMapper, agentRunRecorder);
    }
    return new MockMusicPromptAgent(agentRunRecorder);
  }

  @Bean
  ModerationAgent moderationAgent(AgentRunRecorder agentRunRecorder) {
    return new MockModerationAgent(agentRunRecorder);
  }

  @Bean
  CreativeBriefAgent creativeBriefAgent(
      DeepSeekProperties deepSeekProperties,
      ObjectMapper objectMapper,
      AgentRunRecorder agentRunRecorder) {
    if (deepSeekProperties.isRealCallsEnabled()) {
      return new RealDeepSeekCreativeBriefAgent(deepSeekProperties, objectMapper, agentRunRecorder);
    }
    return new MockCreativeBriefAgent(agentRunRecorder);
  }

  @Bean
  CoverPromptAgent coverPromptAgent(
      DeepSeekProperties deepSeekProperties,
      ObjectMapper objectMapper,
      AgentRunRecorder agentRunRecorder) {
    if (deepSeekProperties.isRealCallsEnabled()) {
      return new RealDeepSeekCoverPromptAgent(deepSeekProperties, objectMapper, agentRunRecorder);
    }
    return new MockCoverPromptAgent(agentRunRecorder);
  }

  @Bean
  QualityEvaluationAgent qualityEvaluationAgent(
      DeepSeekProperties deepSeekProperties,
      ObjectMapper objectMapper,
      AgentRunRecorder agentRunRecorder) {
    if (deepSeekProperties.isRealCallsEnabled()) {
      return new RealDeepSeekQualityEvaluationAgent(
          deepSeekProperties, objectMapper, agentRunRecorder);
    }
    return new MockQualityEvaluationAgent(agentRunRecorder);
  }

  @Bean
  LyricsGenerationService lyricsGenerationService(
      KnowledgeService knowledgeService,
      PromptTemplateService promptTemplateService,
      CreativeBriefAgent creativeBriefAgent,
      DeepSeekLyricsClient deepSeekLyricsClient,
      QualityEvaluationAgent qualityEvaluationAgent,
      AgentRunRecorder agentRunRecorder) {
    return new DefaultLyricsGenerationService(
        knowledgeService,
        promptTemplateService,
        creativeBriefAgent,
        deepSeekLyricsClient,
        qualityEvaluationAgent,
        agentRunRecorder);
  }

  @Bean
  @ConfigurationProperties(prefix = "yanyun.storage")
  ObjectStorageProperties objectStorageProperties() {
    return new ObjectStorageProperties();
  }

  @Bean
  ObjectStorageClient objectStorageClient(ObjectStorageProperties objectStorageProperties) {
    return ObjectStorageClientFactory.create(objectStorageProperties);
  }

  @Bean
  RemoteObjectImporter remoteObjectImporter(ObjectStorageClient objectStorageClient) {
    return new HttpRemoteObjectImporter(objectStorageClient);
  }

  @Bean
  @ConfigurationProperties(prefix = "yanyun.dreammaker")
  DreamMakerProperties dreamMakerProperties() {
    return new DreamMakerProperties();
  }

  @Bean
  DreamMakerClient dreamMakerClient(
      DreamMakerProperties dreamMakerProperties, ObjectMapper objectMapper) {
    return new DreamMakerHttpClient(dreamMakerProperties, objectMapper);
  }

  @Bean
  @ConfigurationProperties(prefix = "yanyun.yunwu")
  YunwuProperties yunwuProperties() {
    return new YunwuProperties();
  }

  @Bean
  MusicProvider mockMusicProvider(
      @Value("${yanyun.music.mock-duration-ms:180000}") int mockDurationMs) {
    return new MockMusicProvider(mockDurationMs);
  }

  @Bean
  MusicProvider sunoMusicProvider(
      @Value("${yanyun.suno.backend:dreammaker}") String sunoBackend,
      DreamMakerClient dreamMakerClient,
      DreamMakerProperties dreamMakerProperties,
      YunwuProperties yunwuProperties,
      ObjectMapper objectMapper) {
    return switch (normalize(sunoBackend)) {
      case "dreammaker" ->
          new SunoMusicProvider(
              dreamMakerClient,
              new SunoMusicProviderOptions(
                  dreamMakerProperties.getSunoModel(),
                  dreamMakerProperties.getMaxPollAttempts(),
                  dreamMakerProperties.getPollInterval()));
      case "yunwu" -> new YunwuSunoMusicProvider(yunwuProperties, objectMapper);
      default -> throw new IllegalArgumentException("Unsupported Suno backend: " + sunoBackend);
    };
  }

  @Bean
  MusicProvider miniMaxMusicProvider(
      DreamMakerClient dreamMakerClient, DreamMakerProperties dreamMakerProperties) {
    return new MiniMaxMusicProvider(
        dreamMakerClient,
        new MiniMaxMusicProviderOptions(
            dreamMakerProperties.getMinimaxModel(),
            dreamMakerProperties.getMaxPollAttempts(),
            dreamMakerProperties.getPollInterval(),
            "mp3",
            44100,
            256000));
  }

  @Bean
  MusicProviderRegistry musicProviderRegistry(List<MusicProvider> providers) {
    return new MusicProviderRegistry(providers);
  }

  @Bean
  MusicProviderSelection musicProviderSelection(
      @Value("${yanyun.music.provider:mock}") String provider) {
    return MusicProviderSelection.fromConfig(provider);
  }

  @Bean
  @ConfigurationProperties(prefix = "yanyun.integration")
  CompanyIntegrationProperties companyIntegrationProperties() {
    return new CompanyIntegrationProperties();
  }

  @Bean
  IntegrationReadinessService integrationReadinessService(
      CompanyIntegrationProperties companyIntegrationProperties,
      DreamMakerProperties dreamMakerProperties,
      YunwuProperties yunwuProperties,
      DeepSeekProperties deepSeekProperties,
      com.yanyun.music.image2.Image2Properties image2Properties,
      Clock clock) {
    return new IntegrationReadinessService(
        companyIntegrationProperties,
        clock,
        !dreamMakerProperties.getAccessKey().isBlank(),
        !dreamMakerProperties.getSecretKey().isBlank(),
        !deepSeekProperties.getApiKey().isBlank(),
        !yunwuProperties.getApiKey().isBlank(),
        !image2Properties.getApiKey().isBlank());
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? "" : value.trim().toLowerCase(Locale.ROOT);
  }
}
