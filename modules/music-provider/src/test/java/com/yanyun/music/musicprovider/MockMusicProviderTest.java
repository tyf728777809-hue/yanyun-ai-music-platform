package com.yanyun.music.musicprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MockMusicProviderTest {

  @Test
  void mockProviderReturnsSucceededAudioResult() {
    MockMusicProvider provider = new MockMusicProvider();

    MusicGenerationResult result =
        provider.submit(new MusicGenerationRequest("work-1", "lyrics", "prompt", "AUTO", Map.of()));

    assertEquals(MusicProviderType.MOCK, result.providerType());
    assertEquals(MusicGenerationStatus.SUCCEEDED, result.status());
    assertEquals("audio/work-1.mp3", result.audioObjectKey());
    assertEquals(180_000, result.durationMs());
  }

  @Test
  void registrySelectsRegisteredProvider() {
    MusicProviderRegistry registry = new MusicProviderRegistry(List.of(new MockMusicProvider()));

    assertTrue(registry.find(MusicProviderType.MOCK).isPresent());
    assertEquals(MusicProviderType.MOCK, registry.require(MusicProviderType.MOCK).providerType());
  }
}
