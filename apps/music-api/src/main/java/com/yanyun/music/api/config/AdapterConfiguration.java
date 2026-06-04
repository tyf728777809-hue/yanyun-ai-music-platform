package com.yanyun.music.api.config;

import com.yanyun.music.auth.AccountAdapter;
import com.yanyun.music.auth.MockAccountAdapter;
import com.yanyun.music.moderation.MockModerationAdapter;
import com.yanyun.music.moderation.ModerationAdapter;
import com.yanyun.music.musicprovider.MockMusicProvider;
import com.yanyun.music.musicprovider.MusicProvider;
import com.yanyun.music.musicprovider.MusicProviderRegistry;
import com.yanyun.music.publish.MockPublishAdapter;
import com.yanyun.music.publish.PublishAdapter;
import com.yanyun.music.quota.MockQuotaAdapter;
import com.yanyun.music.quota.QuotaAdapter;
import java.util.List;
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
  MusicProvider mockMusicProvider() {
    return new MockMusicProvider();
  }

  @Bean
  MusicProviderRegistry musicProviderRegistry(List<MusicProvider> providers) {
    return new MusicProviderRegistry(providers);
  }
}
