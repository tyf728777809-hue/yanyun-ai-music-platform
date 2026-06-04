package com.yanyun.music.musicprovider;

import java.util.Locale;

public record MusicProviderSelection(MusicProviderType providerType) {

  public MusicProviderSelection {
    if (providerType == null) {
      providerType = MusicProviderType.MOCK;
    }
  }

  public static MusicProviderSelection fromConfig(String value) {
    if (value == null || value.isBlank()) {
      return new MusicProviderSelection(MusicProviderType.MOCK);
    }
    String normalized = value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    try {
      return new MusicProviderSelection(MusicProviderType.valueOf(normalized));
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Unsupported music provider: " + value, exception);
    }
  }
}
