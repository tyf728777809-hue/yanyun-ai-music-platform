package com.yanyun.music.worker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TemporalWorkerPropertiesTest {

  @Test
  void storesTemporalConnectionSettings() {
    TemporalWorkerProperties properties =
        new TemporalWorkerProperties("localhost:7233", "default", "song-production-local");

    assertThat(properties.target()).isEqualTo("localhost:7233");
    assertThat(properties.namespace()).isEqualTo("default");
    assertThat(properties.taskQueue()).isEqualTo("song-production-local");
  }
}
