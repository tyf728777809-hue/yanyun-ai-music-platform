package com.yanyun.music.production;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.dreammaker.DreamMakerClient;
import com.yanyun.music.image2.CoverGenerationService;
import com.yanyun.music.image2.DreamMakerImage2CoverGenerationService;
import com.yanyun.music.image2.Image2Properties;
import com.yanyun.music.image2.MockCoverGenerationService;
import com.yanyun.music.image2.WellApiImage2CoverGenerationService;
import com.yanyun.music.media.MockVideoRenderService;
import com.yanyun.music.media.VideoRenderService;
import com.yanyun.music.storage.ObjectStorageClient;
import java.util.Locale;
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
      @Value("${yanyun.image2.backend:dreammaker}") String image2Backend,
      DreamMakerClient dreamMakerClient,
      Image2Properties image2Properties,
      ObjectMapper objectMapper) {
    if ("image2".equalsIgnoreCase(imageProvider) || image2Properties.isRealCallsEnabled()) {
      return switch (normalize(image2Backend)) {
        case "dreammaker" ->
            new DreamMakerImage2CoverGenerationService(dreamMakerClient, image2Properties);
        case "wellapi" -> new WellApiImage2CoverGenerationService(image2Properties, objectMapper);
        default ->
            throw new IllegalArgumentException("Unsupported image2 backend: " + image2Backend);
      };
    }
    return new MockCoverGenerationService();
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? "" : value.trim().toLowerCase(Locale.ROOT);
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
    String mode = renderWorkerProperties.normalizedMode();
    if ("album-ffmpeg".equals(mode) || "ffmpeg-album".equals(mode)) {
      return new FfmpegAlbumVideoRenderService(
          renderWorkerProperties, objectStorageClient, objectMapper);
    }
    if ("mock".equals(mode)) {
      return new MockVideoRenderService();
    }
    throw new IllegalArgumentException("Unsupported render worker mode: " + mode);
  }
}
