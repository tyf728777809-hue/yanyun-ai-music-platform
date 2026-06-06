package com.yanyun.music.production;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.dreammaker.DreamMakerClient;
import com.yanyun.music.image2.CoverGenerationService;
import com.yanyun.music.image2.DreamMakerImage2CoverGenerationService;
import com.yanyun.music.image2.Image2Properties;
import com.yanyun.music.image2.MockCoverGenerationService;
import com.yanyun.music.media.MockVideoRenderService;
import com.yanyun.music.media.VideoRenderService;
import com.yanyun.music.storage.ObjectStorageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MediaGenerationConfiguration {

  @Bean
  @ConfigurationProperties(prefix = "yanyun.image2")
  public Image2Properties image2Properties() {
    return new Image2Properties();
  }

  @Bean
  public CoverGenerationService coverGenerationService(
      @Value("${yanyun.image.provider:mock}") String imageProvider,
      DreamMakerClient dreamMakerClient,
      Image2Properties image2Properties) {
    if ("image2".equalsIgnoreCase(imageProvider) || image2Properties.isRealCallsEnabled()) {
      return new DreamMakerImage2CoverGenerationService(dreamMakerClient, image2Properties);
    }
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
