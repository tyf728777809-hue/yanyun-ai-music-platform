package com.yanyun.music.minimax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yanyun.music.dreammaker.DreamMakerClient;
import com.yanyun.music.dreammaker.DreamMakerOutputFile;
import com.yanyun.music.dreammaker.DreamMakerRunRequest;
import com.yanyun.music.dreammaker.DreamMakerStatusRequest;
import com.yanyun.music.dreammaker.DreamMakerStatusResponse;
import com.yanyun.music.dreammaker.DreamMakerSubmitResponse;
import com.yanyun.music.dreammaker.DreamMakerTaskStatus;
import com.yanyun.music.musicprovider.MusicGenerationRequest;
import com.yanyun.music.musicprovider.MusicGenerationResult;
import com.yanyun.music.musicprovider.MusicGenerationStatus;
import com.yanyun.music.musicprovider.MusicProviderType;
import java.time.Duration;
import java.util.List;
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

  @Test
  void submitsDreamMakerTaskAndReturnsAudioSourceUrl() {
    FakeDreamMakerClient client =
        new FakeDreamMakerClient(
            new DreamMakerSubmitResponse(0, "ok", "task-1"),
            List.of(
                new DreamMakerStatusResponse(
                    0,
                    "ok",
                    DreamMakerTaskStatus.SUCCEEDED,
                    "success",
                    List.of(
                        new DreamMakerOutputFile(
                            "song.mp3",
                            "https://provider.example/song.mp3",
                            null,
                            "audio",
                            182_000)))));
    MiniMaxMusicProvider provider =
        new MiniMaxMusicProvider(
            client,
            new MiniMaxMusicProviderOptions(
                "minimax-music-2.6", 1, Duration.ZERO, "mp3", 44100, 256000));

    MusicGenerationResult result =
        provider.submit(
            new MusicGenerationRequest(
                "work-1", "[Verse]\nlyrics", "cinematic folk", "AUTO", Map.of()));

    assertEquals(MusicGenerationStatus.SUCCEEDED, result.status());
    assertEquals("task-1", result.providerTaskId());
    assertEquals("music-minimax:text-to-music:minimax-music-2.6", result.modelName());
    assertEquals("https://provider.example/song.mp3", result.audioSourceUrl());
    assertEquals("audio/mpeg", result.audioContentType());
    assertEquals(182_000, result.durationMs());
    assertEquals("music-minimax", client.lastRunRequest.appName());
    assertEquals("text-to-music", client.lastRunRequest.subAppName());
    assertEquals("minimax-music-2.6", client.lastRunRequest.params().get("model"));
    assertEquals("cinematic folk", client.lastRunRequest.params().get("prompt"));
    assertEquals("[Verse]\nlyrics", client.lastRunRequest.params().get("lyrics"));
    assertEquals(false, client.lastRunRequest.params().get("is_instrumental"));
    assertEquals(false, client.lastRunRequest.params().get("lyrics_optimizer"));
    assertEquals("mp3", client.lastRunRequest.params().get("audio_format"));
    assertEquals(44100, client.lastRunRequest.params().get("sample_rate"));
    assertEquals(256000, client.lastRunRequest.params().get("bitrate"));
  }

  @Test
  void mapsFailedProviderStatusToGenerationFailure() {
    FakeDreamMakerClient client =
        new FakeDreamMakerClient(
            new DreamMakerSubmitResponse(0, "ok", "task-1"),
            List.of(
                new DreamMakerStatusResponse(
                    0, "audio quality failed", DreamMakerTaskStatus.FAILED, "failed", List.of())));
    MiniMaxMusicProvider provider =
        new MiniMaxMusicProvider(
            client,
            new MiniMaxMusicProviderOptions(
                "minimax-music-2.6", 1, Duration.ZERO, "mp3", 44100, 256000));

    MusicGenerationResult result =
        provider.submit(new MusicGenerationRequest("work-1", "lyrics", "prompt", "AUTO", Map.of()));

    assertEquals(MusicGenerationStatus.FAILED, result.status());
    assertEquals("MUSIC_QUALITY_FAILED", result.failureCode());
    assertEquals("task-1", result.providerTaskId());
  }

  private static final class FakeDreamMakerClient implements DreamMakerClient {

    private final DreamMakerSubmitResponse submitResponse;
    private final List<DreamMakerStatusResponse> statusResponses;
    private int statusIndex;
    private DreamMakerRunRequest lastRunRequest;

    private FakeDreamMakerClient(
        DreamMakerSubmitResponse submitResponse, List<DreamMakerStatusResponse> statusResponses) {
      this.submitResponse = submitResponse;
      this.statusResponses = statusResponses;
    }

    @Override
    public DreamMakerSubmitResponse submit(DreamMakerRunRequest request) {
      this.lastRunRequest = request;
      return submitResponse;
    }

    @Override
    public DreamMakerStatusResponse status(DreamMakerStatusRequest request) {
      int index = Math.min(statusIndex, statusResponses.size() - 1);
      statusIndex++;
      return statusResponses.get(index);
    }
  }
}
