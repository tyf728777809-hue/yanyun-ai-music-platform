package com.yanyun.music.dreammaker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DreamMakerTaskStatusTest {

  @Test
  void mapsProviderStatuses() {
    assertEquals(DreamMakerTaskStatus.QUEUED, DreamMakerTaskStatus.fromProviderStatus("queued"));
    assertEquals(DreamMakerTaskStatus.RUNNING, DreamMakerTaskStatus.fromProviderStatus("running"));
    assertEquals(
        DreamMakerTaskStatus.SUCCEEDED, DreamMakerTaskStatus.fromProviderStatus("success"));
    assertEquals(DreamMakerTaskStatus.FAILED, DreamMakerTaskStatus.fromProviderStatus("failed"));
    assertEquals(DreamMakerTaskStatus.FAILED, DreamMakerTaskStatus.fromProviderStatus("unknown"));
  }
}
