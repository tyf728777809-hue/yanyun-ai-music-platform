package com.yanyun.music.api.config;

import com.yanyun.music.auth.AccountAdapter;
import com.yanyun.music.auth.MockAccountAdapter;
import com.yanyun.music.minimax.MiniMaxMusicProvider;
import com.yanyun.music.moderation.MockModerationAdapter;
import com.yanyun.music.moderation.ModerationAdapter;
import com.yanyun.music.musicprovider.MockMusicProvider;
import com.yanyun.music.musicprovider.MusicProvider;
import com.yanyun.music.musicprovider.MusicProviderRegistry;
import com.yanyun.music.musicprovider.MusicProviderSelection;
import com.yanyun.music.publish.MockPublishAdapter;
import com.yanyun.music.publish.PublishAdapter;
import com.yanyun.music.quota.MockQuotaAdapter;
import com.yanyun.music.quota.QuotaAdapter;
import com.yanyun.music.storage.LocalObjectStorageClient;
import com.yanyun.music.storage.ObjectStorageClient;
import com.yanyun.music.suno.SunoMusicProvider;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
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
  ObjectStorageClient objectStorageClient() {
    return new LocalObjectStorageClient(
        Path.of("build/local-object-storage/yanyun-works-local"),
        "http://localhost:9000/yanyun-works-local");
  }

  @Bean
  MusicProvider mockMusicProvider() {
    return new MockMusicProvider();
  }

  @Bean
  MusicProvider sunoMusicProvider() {
    return new SunoMusicProvider();
  }

  @Bean
  MusicProvider miniMaxMusicProvider() {
    return new MiniMaxMusicProvider();
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
