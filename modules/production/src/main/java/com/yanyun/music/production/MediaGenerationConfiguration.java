package com.yanyun.music.production;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.image2.CoverGenerationService;
import com.yanyun.music.image2.MockCoverGenerationService;
import com.yanyun.music.media.MockVideoRenderService;
import com.yanyun.music.media.VideoRenderService;
import com.yanyun.music.storage.ObjectStorageClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MediaGenerationConfiguration {

  @Bean
  public CoverGenerationService coverGenerationService() {
    return new MockCoverGenerationService();
  }

  @Bean
  @ConfigurationProperties(prefix = "yanyun.render-worker")
  public RenderWorkerProperties renderWorkerProperties() {
    return new RenderWorkerProperties();
  }

  @Bean
  public VideoRenderService videoRenderService(
      RenderWorkerProperties renderWorkerProperties,
      ObjectStorageClient objectStorageClient,
      ObjectMapper objectMapper) {
    if ("local-process".equals(renderWorkerProperties.normalizedMode())) {
      return new LocalProcessVideoRenderService(
          renderWorkerProperties, objectStorageClient, objectMapper);
    }
    return new MockVideoRenderService();
  }
}
