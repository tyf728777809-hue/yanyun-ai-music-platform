package com.yanyun.music.production;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.media.MockVideoRenderService;
import com.yanyun.music.media.VideoRenderRequest;
import com.yanyun.music.media.VideoRenderResult;
import com.yanyun.music.media.VideoRenderService;
import com.yanyun.music.storage.ObjectStorageClient;
import com.yanyun.music.storage.ObjectStorageDownloadUrl;
import com.yanyun.music.storage.ObjectStoragePutRequest;
import com.yanyun.music.storage.StoredObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LocalProcessVideoRenderServiceTest {

  @Test
  void mediaConfigurationDefaultsToMockRenderer() {
    VideoRenderService service =
        new MediaGenerationConfiguration()
            .videoRenderService(
                new RenderWorkerProperties(), new CapturingStorageClient(), new ObjectMapper());

    assertInstanceOf(MockVideoRenderService.class, service);
  }

  @Test
  void uploadsVideoAndTimelineFromSuccessfulLocalProcess() {
    CapturingStorageClient storageClient = new CapturingStorageClient();
    LocalProcessVideoRenderService service =
        new LocalProcessVideoRenderService(
            successfulProperties(), storageClient, new ObjectMapper());

    VideoRenderResult result = service.renderVideo(videoRenderRequest());

    assertEquals("VIDEO", result.videoAsset().assetType());
    assertEquals("videos/work-123.mp4", result.videoAsset().objectKey());
    assertEquals("video/mp4", result.videoAsset().mimeType());
    assertEquals(1920, result.videoAsset().width());
    assertEquals(1080, result.videoAsset().height());
    assertEquals(180000, result.videoAsset().durationMs());
    assertNotNull(result.videoAsset().checksum());
    assertEquals("TIMELINE", result.timelineAsset().assetType());
    assertEquals("timelines/work-123.json", result.timelineAsset().objectKey());
    assertEquals("application/json", result.timelineAsset().mimeType());
    assertEquals(180000, result.timelineAsset().durationMs());
    assertTrue(storageClient.objects.containsKey("videos/work-123.mp4"));
    assertTrue(storageClient.objects.containsKey("timelines/work-123.json"));
  }

  @Test
  void failsWhenRenderWorkerProcessExitsNonZero() {
    CapturingStorageClient storageClient = new CapturingStorageClient();
    RenderWorkerProperties properties = successfulProperties();
    properties.setArguments(
        List.of("-cp", System.getProperty("java.class.path"), FailingRenderWorker.class.getName()));
    LocalProcessVideoRenderService service =
        new LocalProcessVideoRenderService(properties, storageClient, new ObjectMapper());

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> service.renderVideo(videoRenderRequest()));

    assertTrue(exception.getMessage().contains("exited with code"));
    assertTrue(storageClient.objects.isEmpty());
  }

  private RenderWorkerProperties successfulProperties() {
    RenderWorkerProperties properties = new RenderWorkerProperties();
    properties.setCommand(javaBinary());
    properties.setArguments(
        List.of("-cp", System.getProperty("java.class.path"), FakeRenderWorker.class.getName()));
    properties.setWorkingDirectory("");
    properties.setTimeout(Duration.ofSeconds(10));
    return properties;
  }

  private VideoRenderRequest videoRenderRequest() {
    return new VideoRenderRequest(
        "work-123",
        "边城旧梦",
        "旧梦重回燕云。",
        "第一句\n第二句",
        "audio/work-123.mp3",
        "audio/mpeg",
        "covers/work-123.png",
        180000);
  }

  private String javaBinary() {
    return Path.of(System.getProperty("java.home"), "bin", "java").toString();
  }

  private static final class CapturingStorageClient implements ObjectStorageClient {

    private final Map<String, byte[]> objects = new HashMap<>();

    @Override
    public StoredObject putObject(ObjectStoragePutRequest request) {
      objects.put(request.objectKey(), request.content());
      return new StoredObject(
          request.objectKey(),
          "http://localhost/" + request.objectKey(),
          request.contentType(),
          request.content().length);
    }

    @Override
    public ObjectStorageDownloadUrl createDownloadUrl(String objectKey) {
      return new ObjectStorageDownloadUrl(
          objectKey, "http://localhost/" + objectKey, OffsetDateTime.now().plusHours(1));
    }
  }

  public static final class FakeRenderWorker {

    public static void main(String[] args) throws IOException {
      Path outputPath = Path.of(requiredValue(args, "--output"));
      Path outputDirectory = Path.of(requiredValue(args, "--out-dir"));
      Files.createDirectories(outputDirectory);
      Path videoPath = outputDirectory.resolve("work-123.mp4");
      Path timelinePath = outputDirectory.resolve("work-123.timeline.json");
      Files.writeString(videoPath, "fake-mp4", StandardCharsets.UTF_8);
      Files.writeString(timelinePath, "{\"lines\":[]}", StandardCharsets.UTF_8);
      Files.writeString(
          outputPath,
          """
          {
            "work_id": "work-123",
            "video_file_path": "%s",
            "timeline_file_path": "%s",
            "width": 1920,
            "height": 1080,
            "fps": 30,
            "duration_ms": 180000,
            "duration_in_frames": 5400,
            "renderer": "remotion",
            "composition_id": "LyricVideo16x9"
          }
          """
              .formatted(jsonPath(videoPath), jsonPath(timelinePath)),
          StandardCharsets.UTF_8);
    }

    private static String requiredValue(String[] args, String name) {
      for (int index = 0; index < args.length - 1; index++) {
        if (name.equals(args[index])) {
          return args[index + 1];
        }
      }
      throw new IllegalArgumentException("Missing " + name);
    }

    private static String jsonPath(Path path) {
      return path.toAbsolutePath().toString().replace("\\", "\\\\");
    }
  }

  public static final class FailingRenderWorker {

    public static void main(String[] args) {
      System.out.println("fake render failure");
      System.exit(7);
    }
  }
}
