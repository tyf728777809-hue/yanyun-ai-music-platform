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
import org.junit.jupiter.api.io.TempDir;

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
    CapturingStorageClient storageClient = storageClientWithRenderInputs();
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
    assertEquals("lyric-video-16x9-v2", result.videoAsset().metadata().get("template_id"));
    assertEquals("lyric-video-16x9-v2", result.timelineAsset().metadata().get("template_id"));
  }

  @Test
  void resolvesRelativeWorkingDirectoryFromAncestor(@TempDir Path repoRoot) throws IOException {
    Path apiDirectory = repoRoot.resolve("apps/music-api");
    Path renderWorkerDirectory = repoRoot.resolve("apps/render-worker");
    Files.createDirectories(apiDirectory);
    Files.createDirectories(renderWorkerDirectory);
    String originalUserDirectory = System.getProperty("user.dir");
    CapturingStorageClient storageClient = storageClientWithRenderInputs();
    RenderWorkerProperties properties = successfulProperties();
    properties.setWorkingDirectory("apps/render-worker");
    LocalProcessVideoRenderService service =
        new LocalProcessVideoRenderService(properties, storageClient, new ObjectMapper());

    try {
      System.setProperty("user.dir", apiDirectory.toString());

      VideoRenderResult result = service.renderVideo(videoRenderRequest());

      assertEquals("VIDEO", result.videoAsset().assetType());
      assertTrue(storageClient.objects.containsKey("videos/work-123.mp4"));
    } finally {
      System.setProperty("user.dir", originalUserDirectory);
    }
  }

  @Test
  void failsWhenRenderWorkerProcessExitsNonZero() {
    CapturingStorageClient storageClient = storageClientWithRenderInputs();
    RenderWorkerProperties properties = successfulProperties();
    properties.setArguments(
        List.of("-cp", System.getProperty("java.class.path"), FailingRenderWorker.class.getName()));
    LocalProcessVideoRenderService service =
        new LocalProcessVideoRenderService(properties, storageClient, new ObjectMapper());

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> service.renderVideo(videoRenderRequest()));

    assertTrue(exception.getMessage().contains("exited with code"));
    assertTrue(!storageClient.objects.containsKey("videos/work-123.mp4"));
    assertTrue(!storageClient.objects.containsKey("timelines/work-123.json"));
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

  private CapturingStorageClient storageClientWithRenderInputs() {
    CapturingStorageClient storageClient = new CapturingStorageClient();
    storageClient.objects.put("audio/work-123.mp3", "fake-audio".getBytes(StandardCharsets.UTF_8));
    storageClient.objects.put("covers/work-123.png", "fake-cover".getBytes(StandardCharsets.UTF_8));
    return storageClient;
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

    @Override
    public byte[] getObject(String objectKey) {
      byte[] content = objects.get(objectKey);
      if (content == null) {
        throw new IllegalStateException("Missing object: " + objectKey);
      }
      return content;
    }
  }

  public static final class FakeRenderWorker {

    public static void main(String[] args) throws IOException {
      Path inputPath = Path.of(requiredValue(args, "--input"));
      Path outputPath = Path.of(requiredValue(args, "--output"));
      Path outputDirectory = Path.of(requiredValue(args, "--out-dir"));
      validateRenderInput(inputPath);
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
            "composition_id": "LyricVideo16x9V2"
          }
          """
              .formatted(jsonPath(videoPath), jsonPath(timelinePath)),
          StandardCharsets.UTF_8);
    }

    private static void validateRenderInput(Path inputPath) throws IOException {
      Map<?, ?> input = new ObjectMapper().readValue(inputPath.toFile(), Map.class);
      Path audioSourcePath = Path.of(requiredString(input, "audio_source_path"));
      Path coverSourcePath = Path.of(requiredString(input, "cover_source_path"));
      if (!Files.isRegularFile(audioSourcePath) || !Files.isReadable(audioSourcePath)) {
        throw new IllegalStateException("Staged audio source is not readable");
      }
      if (!Files.isRegularFile(coverSourcePath) || !Files.isReadable(coverSourcePath)) {
        throw new IllegalStateException("Staged cover source is not readable");
      }
      String audioContent = Files.readString(audioSourcePath, StandardCharsets.UTF_8);
      String coverContent = Files.readString(coverSourcePath, StandardCharsets.UTF_8);
      if (!"fake-audio".equals(audioContent)) {
        throw new IllegalStateException("Unexpected staged audio content");
      }
      if (!"fake-cover".equals(coverContent)) {
        throw new IllegalStateException("Unexpected staged cover content");
      }
      if (!"lyric-video-16x9-v2".equals(input.get("template_id"))) {
        throw new IllegalStateException("Render input template_id is not lyric-video-16x9-v2");
      }
    }

    private static String requiredString(Map<?, ?> input, String name) {
      Object value = input.get(name);
      if (!(value instanceof String text) || text.isBlank()) {
        throw new IllegalStateException("Missing render input field: " + name);
      }
      return text;
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
