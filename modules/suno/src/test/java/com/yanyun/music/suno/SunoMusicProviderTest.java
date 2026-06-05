package com.yanyun.music.suno;

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
                            "https://provider.example/cover.png",
                            "audio",
                            181_000)))));
    SunoMusicProvider provider =
        new SunoMusicProvider(client, new SunoMusicProviderOptions("chirp-crow", 1, Duration.ZERO));

    MusicGenerationResult result =
        provider.submit(
            new MusicGenerationRequest(
                "work-1", "[Verse]\nlyrics", "cinematic folk", "FEMALE", Map.of()));

    assertEquals(MusicGenerationStatus.SUCCEEDED, result.status());
    assertEquals("task-1", result.providerTaskId());
    assertEquals("https://provider.example/song.mp3", result.audioSourceUrl());
    assertEquals("audio/mpeg", result.audioContentType());
    assertEquals(181_000, result.durationMs());
    assertEquals("suno", client.lastRunRequest.appName());
    assertEquals("music-gen", client.lastRunRequest.subAppName());
    assertEquals("[Verse]\nlyrics", client.lastRunRequest.params().get("prompt"));
    assertEquals("cinematic folk", client.lastRunRequest.params().get("tags"));
    assertEquals("chirp-crow", client.lastRunRequest.params().get("mv"));
    assertEquals(Map.of("vocal_gender", "f"), client.lastRunRequest.params().get("metadata"));
  }

  @Test
  void returnsTimeoutWhenTaskNeverFinishes() {
    FakeDreamMakerClient client =
        new FakeDreamMakerClient(
            new DreamMakerSubmitResponse(0, "ok", "task-1"),
            List.of(
                new DreamMakerStatusResponse(
                    0, "ok", DreamMakerTaskStatus.RUNNING, "running", List.of())));
    SunoMusicProvider provider =
        new SunoMusicProvider(client, new SunoMusicProviderOptions("chirp-crow", 1, Duration.ZERO));

    MusicGenerationResult result =
        provider.submit(new MusicGenerationRequest("work-1", "lyrics", "prompt", "AUTO", Map.of()));

    assertEquals(MusicGenerationStatus.FAILED, result.status());
    assertEquals("PROVIDER_TIMEOUT", result.failureCode());
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
