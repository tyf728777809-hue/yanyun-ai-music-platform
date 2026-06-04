package com.yanyun.music.suno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yanyun.music.musicprovider.MusicGenerationRequest;
import com.yanyun.music.musicprovider.MusicProviderType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SunoMusicProviderTest {

  @Test
  void exposesSunoProviderTypeWithoutCallingRealApi() {
    SunoMusicProvider provider = new SunoMusicProvider();

    assertEquals(MusicProviderType.SUNO, provider.providerType());
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            provider.submit(
                new MusicGenerationRequest("work-1", "lyrics", "prompt", "AUTO", Map.of())));
  }
}
