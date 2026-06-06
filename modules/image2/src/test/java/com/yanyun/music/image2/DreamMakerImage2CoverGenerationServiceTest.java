package com.yanyun.music.image2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yanyun.music.dreammaker.DreamMakerClient;
import com.yanyun.music.dreammaker.DreamMakerOutputFile;
import com.yanyun.music.dreammaker.DreamMakerRunRequest;
import com.yanyun.music.dreammaker.DreamMakerStatusRequest;
import com.yanyun.music.dreammaker.DreamMakerStatusResponse;
import com.yanyun.music.dreammaker.DreamMakerSubmitResponse;
import com.yanyun.music.dreammaker.DreamMakerTaskStatus;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DreamMakerImage2CoverGenerationServiceTest {

  @Test
  void submitsGptImage2TaskAndReturnsRemoteCoverForImport() {
    CapturingDreamMakerClient dreamMakerClient =
        new CapturingDreamMakerClient(
            new DreamMakerSubmitResponse(0, "ok", "task-123"),
            new DreamMakerStatusResponse(
                0,
                "ok",
                DreamMakerTaskStatus.SUCCEEDED,
                "succeeded",
                List.of(
                    new DreamMakerOutputFile(
                        "cover.png", "https://cdn.example.test/cover.png", null, "image", null))));
    DreamMakerImage2CoverGenerationService service =
        new DreamMakerImage2CoverGenerationService(dreamMakerClient, realProperties());

    CoverGenerationResult result = service.generateCover(request());

    assertEquals("gpt-image-2", dreamMakerClient.submittedRequest.subAppName());
    assertEquals("gpt-image-2", dreamMakerClient.submittedRequest.params().get("model"));
    assertEquals("2048x1152", dreamMakerClient.submittedRequest.params().get("size"));
    assertEquals("medium", dreamMakerClient.submittedRequest.params().get("quality"));
    assertEquals("COVER", result.asset().assetType());
    assertEquals("covers/work-123.png", result.asset().objectKey());
    assertEquals("image/png", result.asset().mimeType());
    assertEquals("dreammaker-image2", result.asset().checksum());
    assertEquals(2048, result.asset().width());
    assertEquals(1152, result.asset().height());
    assertEquals(
        "https://cdn.example.test/cover.png",
        result
            .asset()
            .metadata()
            .get(DreamMakerImage2CoverGenerationService.SOURCE_URL_METADATA_KEY));
    assertEquals(1, dreamMakerClient.submitCount.get());
  }

  @Test
  void refusesToSubmitWhenRealSwitchIsDisabled() {
    CapturingDreamMakerClient dreamMakerClient =
        new CapturingDreamMakerClient(
            new DreamMakerSubmitResponse(0, "ok", "task-123"),
            new DreamMakerStatusResponse(0, "ok", DreamMakerTaskStatus.SUCCEEDED, "ok", List.of()));
    Image2Properties properties = realProperties();
    properties.setRealCallsEnabled(false);
    DreamMakerImage2CoverGenerationService service =
        new DreamMakerImage2CoverGenerationService(dreamMakerClient, properties);

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> service.generateCover(request()));

    assertTrue(exception.getMessage().contains("IMAGE_REAL_CALLS_ENABLED"));
    assertEquals(0, dreamMakerClient.submitCount.get());
  }

  @Test
  void failsWhenProviderSucceedsWithoutImageOutput() {
    CapturingDreamMakerClient dreamMakerClient =
        new CapturingDreamMakerClient(
            new DreamMakerSubmitResponse(0, "ok", "task-123"),
            new DreamMakerStatusResponse(
                0,
                "ok",
                DreamMakerTaskStatus.SUCCEEDED,
                "succeeded",
                List.of(
                    new DreamMakerOutputFile(
                        "audio.mp3", "https://cdn.example.test/audio.mp3", null, "audio", null))));
    DreamMakerImage2CoverGenerationService service =
        new DreamMakerImage2CoverGenerationService(dreamMakerClient, realProperties());

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> service.generateCover(request()));

    assertTrue(exception.getMessage().contains("without image output"));
  }

  private Image2Properties realProperties() {
    Image2Properties properties = new Image2Properties();
    properties.setRealCallsEnabled(true);
    properties.setPollInterval(Duration.ZERO);
    properties.setMaxPollAttempts(1);
    return properties;
  }

  private CoverGenerationRequest request() {
    return new CoverGenerationRequest(
        "work-123",
        "边城旧梦",
        "燕云边城里的原创歌曲",
        "[Verse]\n雁门风起过长街",
        "国风民谣",
        "燕云边城夜色，远山，灯火，国风写实封面",
        "现代城市，真实人物肖像",
        1920,
        1080,
        Map.of("prompt_source", "cover-prompt-agent"));
  }

  private static final class CapturingDreamMakerClient implements DreamMakerClient {

    private final DreamMakerSubmitResponse submitResponse;
    private final DreamMakerStatusResponse statusResponse;
    private final AtomicInteger submitCount = new AtomicInteger();
    private DreamMakerRunRequest submittedRequest;

    private CapturingDreamMakerClient(
        DreamMakerSubmitResponse submitResponse, DreamMakerStatusResponse statusResponse) {
      this.submitResponse = submitResponse;
      this.statusResponse = statusResponse;
    }

    @Override
    public DreamMakerSubmitResponse submit(DreamMakerRunRequest request) {
      submitCount.incrementAndGet();
      submittedRequest = request;
      return submitResponse;
    }

    @Override
    public DreamMakerStatusResponse status(DreamMakerStatusRequest request) {
      return statusResponse;
    }
  }
}
