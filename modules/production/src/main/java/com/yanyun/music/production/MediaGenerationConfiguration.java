package com.yanyun.music.production;

import com.yanyun.music.image2.CoverGenerationService;
import com.yanyun.music.image2.MockCoverGenerationService;
import com.yanyun.music.media.MockVideoRenderService;
import com.yanyun.music.media.VideoRenderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MediaGenerationConfiguration {

  @Bean
  public CoverGenerationService coverGenerationService() {
    return new MockCoverGenerationService();
  }

  @Bean
  public VideoRenderService videoRenderService() {
    return new MockVideoRenderService();
  }
}
