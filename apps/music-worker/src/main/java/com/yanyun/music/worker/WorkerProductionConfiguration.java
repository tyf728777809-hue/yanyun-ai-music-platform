package com.yanyun.music.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.agentruntime.AgentRunRecorder;
import com.yanyun.music.creativeagent.CoverPromptAgent;
import com.yanyun.music.creativeagent.MockCoverPromptAgent;
import com.yanyun.music.creativeagent.MockModerationAgent;
import com.yanyun.music.creativeagent.MockMusicPromptAgent;
import com.yanyun.music.creativeagent.MockQualityEvaluationAgent;
import com.yanyun.music.creativeagent.ModerationAgent;
import com.yanyun.music.creativeagent.MusicPromptAgent;
import com.yanyun.music.creativeagent.QualityEvaluationAgent;
import com.yanyun.music.dreammaker.DreamMakerClient;
import com.yanyun.music.dreammaker.DreamMakerHttpClient;
import com.yanyun.music.dreammaker.DreamMakerProperties;
import com.yanyun.music.minimax.MiniMaxMusicProvider;
import com.yanyun.music.minimax.MiniMaxMusicProviderOptions;
import com.yanyun.music.moderation.MockModerationAdapter;
import com.yanyun.music.moderation.ModerationAdapter;
import com.yanyun.music.musicprovider.MockMusicProvider;
import com.yanyun.music.musicprovider.MusicProvider;
import com.yanyun.music.musicprovider.MusicProviderRegistry;
import com.yanyun.music.musicprovider.MusicProviderSelection;
import com.yanyun.music.production.JdbcAgentRunRecorder;
import com.yanyun.music.production.RecordingSongProductionStepActivities;
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
import com.yanyun.music.workflow.SongProductionStepActivities;
import com.yanyun.music.workpersistence.WorkRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class WorkerProductionConfiguration {

  @Bean
  QuotaAdapter quotaAdapter() {
    return new MockQuotaAdapter();
  }

  @Bean
  ModerationAdapter moderationAdapter() {
    return new MockModerationAdapter();
  }

  @Bean
  PublishAdapter publishAdapter(@Value("${yanyun.storage.environment:local}") String environment) {
    return new MockPublishAdapter(environment);
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
  AgentRunRecorder agentRunRecorder(JdbcTemplate jdbcTemplate) {
    return new JdbcAgentRunRecorder(jdbcTemplate);
  }

  @Bean
  MusicPromptAgent musicPromptAgent(AgentRunRecorder agentRunRecorder) {
    return new MockMusicPromptAgent(agentRunRecorder);
  }

  @Bean
  ModerationAgent moderationAgent(AgentRunRecorder agentRunRecorder) {
    return new MockModerationAgent(agentRunRecorder);
  }

  @Bean
  CoverPromptAgent coverPromptAgent(AgentRunRecorder agentRunRecorder) {
    return new MockCoverPromptAgent(agentRunRecorder);
  }

  @Bean
  QualityEvaluationAgent qualityEvaluationAgent(AgentRunRecorder agentRunRecorder) {
    return new MockQualityEvaluationAgent(agentRunRecorder);
  }

  @Bean
  MusicProvider mockMusicProvider(
      @Value("${yanyun.music.mock-duration-ms:180000}") int mockDurationMs) {
    return new MockMusicProvider(mockDurationMs);
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
  MusicProvider sunoMusicProvider(
      DreamMakerClient dreamMakerClient, DreamMakerProperties dreamMakerProperties) {
    return new SunoMusicProvider(
        dreamMakerClient,
        new SunoMusicProviderOptions(
            dreamMakerProperties.getSunoModel(),
            dreamMakerProperties.getMaxPollAttempts(),
            dreamMakerProperties.getPollInterval()));
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
  SongProductionStepActivities songProductionStepActivities(WorkRepository workRepository) {
    return new RecordingSongProductionStepActivities(workRepository);
  }
}
