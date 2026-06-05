package com.yanyun.music.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.auth.AccountAdapter;
import com.yanyun.music.auth.MockAccountAdapter;
import com.yanyun.music.deepseek.DeepSeekLyricsClient;
import com.yanyun.music.deepseek.MockDeepSeekLyricsClient;
import com.yanyun.music.dreammaker.DreamMakerClient;
import com.yanyun.music.dreammaker.DreamMakerHttpClient;
import com.yanyun.music.dreammaker.DreamMakerProperties;
import com.yanyun.music.knowledge.KnowledgeService;
import com.yanyun.music.knowledge.MockKnowledgeService;
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
import com.yanyun.music.prompt.MockPromptTemplateService;
import com.yanyun.music.prompt.PromptTemplateService;
import com.yanyun.music.publish.MockPublishAdapter;
import com.yanyun.music.publish.PublishAdapter;
import com.yanyun.music.quota.MockQuotaAdapter;
import com.yanyun.music.quota.QuotaAdapter;
import com.yanyun.music.storage.HttpRemoteObjectImporter;
import com.yanyun.music.storage.LocalObjectStorageClient;
import com.yanyun.music.storage.ObjectStorageClient;
import com.yanyun.music.storage.RemoteObjectImporter;
import com.yanyun.music.suno.SunoMusicProvider;
import com.yanyun.music.suno.SunoMusicProviderOptions;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdapterConfiguration {

  @Bean
  AccountAdapter accountAdapter() {
    return new MockAccountAdapter();
  }

  @Bean
  QuotaAdapter quotaAdapter() {
    return new MockQuotaAdapter();
  }

  @Bean
  ModerationAdapter moderationAdapter() {
    return new MockModerationAdapter();
  }

  @Bean
  PublishAdapter publishAdapter() {
    return new MockPublishAdapter();
  }

  @Bean
  KnowledgeService knowledgeService() {
    return new MockKnowledgeService();
  }

  @Bean
  PromptTemplateService promptTemplateService() {
    return new MockPromptTemplateService();
  }

  @Bean
  DeepSeekLyricsClient deepSeekLyricsClient() {
    return new MockDeepSeekLyricsClient();
  }

  @Bean
  LyricsGenerationService lyricsGenerationService(
      KnowledgeService knowledgeService,
      PromptTemplateService promptTemplateService,
      DeepSeekLyricsClient deepSeekLyricsClient) {
    return new DefaultLyricsGenerationService(
        knowledgeService, promptTemplateService, deepSeekLyricsClient);
  }

  @Bean
  ObjectStorageClient objectStorageClient() {
    return new LocalObjectStorageClient(
        Path.of("build/local-object-storage/yanyun-works-local"),
        "http://localhost:9000/yanyun-works-local");
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
  MusicProvider mockMusicProvider() {
    return new MockMusicProvider();
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
}
