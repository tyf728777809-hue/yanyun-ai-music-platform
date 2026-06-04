package com.yanyun.music.musicprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MusicProviderSelectionTest {

  @Test
  void defaultsToMockWhenConfigIsBlank() {
    assertEquals(MusicProviderType.MOCK, MusicProviderSelection.fromConfig(null).providerType());
    assertEquals(MusicProviderType.MOCK, MusicProviderSelection.fromConfig("").providerType());
  }

  @Test
  void parsesSupportedProviderNames() {
    assertEquals(MusicProviderType.MOCK, MusicProviderSelection.fromConfig("mock").providerType());
    assertEquals(MusicProviderType.SUNO, MusicProviderSelection.fromConfig("suno").providerType());
    assertEquals(
        MusicProviderType.MINIMAX, MusicProviderSelection.fromConfig("MINIMAX").providerType());
  }

  @Test
  void rejectsUnsupportedProviderName() {
    assertThrows(
        IllegalArgumentException.class, () -> MusicProviderSelection.fromConfig("unknown"));
  }
}
