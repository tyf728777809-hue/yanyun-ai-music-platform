package com.yanyun.music.musicprovider;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MusicProviderRegistry {

  private final Map<MusicProviderType, MusicProvider> providers;

  public MusicProviderRegistry(List<MusicProvider> providers) {
    this.providers = new EnumMap<>(MusicProviderType.class);
    for (MusicProvider provider : providers) {
      this.providers.put(provider.providerType(), provider);
    }
  }

  public Optional<MusicProvider> find(MusicProviderType providerType) {
    return Optional.ofNullable(providers.get(providerType));
  }

  public MusicProvider require(MusicProviderType providerType) {
    return find(providerType)
        .orElseThrow(
            () ->
                new IllegalArgumentException("Music provider is not registered: " + providerType));
  }
}
