package com.yanyun.music.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class HealthControllerTest {

  @Test
  void healthReturnsOkStatus() {
    Map<String, Object> response = new HealthController().health();

    assertThat(response).containsEntry("status", "OK");
    assertThat(response).containsEntry("service", "music-api");
  }
}
