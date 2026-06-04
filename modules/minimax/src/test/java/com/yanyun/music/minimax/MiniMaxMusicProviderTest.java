package com.yanyun.music.minimax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yanyun.music.musicprovider.MusicGenerationRequest;
import com.yanyun.music.musicprovider.MusicProviderType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MiniMaxMusicProviderTest {

  @Test
  void exposesMiniMaxProviderTypeWithoutCallingRealApi() {
    MiniMaxMusicProvider provider = new MiniMaxMusicProvider();

    assertEquals(MusicProviderType.MINIMAX, provider.providerType());
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            provider.submit(
                new MusicGenerationRequest("work-1", "lyrics", "prompt", "AUTO", Map.of())));
  }
}
