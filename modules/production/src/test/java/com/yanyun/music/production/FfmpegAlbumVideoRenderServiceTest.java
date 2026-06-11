package com.yanyun.music.production;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FfmpegAlbumVideoRenderServiceTest {

  @Test
  void mediaConfigurationUsesAlbumFfmpegRenderer() {
    RenderWorkerProperties properties = new RenderWorkerProperties();
    properties.setMode("album-ffmpeg");

    VideoRenderService service =
        new MediaGenerationConfiguration()
            .videoRenderService(properties, new CapturingStorageClient(), new ObjectMapper());

    assertInstanceOf(FfmpegAlbumVideoRenderService.class, service);
  }

  @Test
  void mediaConfigurationRejectsLocalProcessRenderer() {
    RenderWorkerProperties properties = new RenderWorkerProperties();
    properties.setMode("local-process");

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new MediaGenerationConfiguration()
                    .videoRenderService(
                        properties, new CapturingStorageClient(), new ObjectMapper()));

    assertTrue(exception.getMessage().contains("Unsupported render worker mode"));
  }

  @Test
  void uploadsAlbumVideoAndNoSubtitleTimeline() throws IOException {
    CapturingStorageClient storageClient = storageClientWithInputs();
    ObjectMapper objectMapper = new ObjectMapper();
    FfmpegAlbumVideoRenderService service =
        new FfmpegAlbumVideoRenderService(
            new RenderWorkerProperties(), storageClient, objectMapper, successfulEncoder());

    VideoRenderResult result = service.renderVideo(videoRenderRequest());

    assertEquals("VIDEO", result.videoAsset().assetType());
    assertEquals("videos/work-123.mp4", result.videoAsset().objectKey());
    assertEquals("video/mp4", result.videoAsset().mimeType());
    assertEquals(1920, result.videoAsset().width());
    assertEquals(1080, result.videoAsset().height());
    assertEquals(180_000, result.videoAsset().durationMs());
    assertEquals("ffmpeg", result.videoAsset().metadata().get("renderer"));
    assertEquals("album-ffmpeg", result.videoAsset().metadata().get("source_mode"));
    assertEquals(
        FfmpegAlbumVideoRenderService.TEMPLATE_ID,
        result.videoAsset().metadata().get("template_id"));
    assertEquals("none", result.videoAsset().metadata().get("subtitle_strategy"));
    assertNotNull(result.videoAsset().checksum());

    assertEquals("TIMELINE", result.timelineAsset().assetType());
    assertEquals("timelines/work-123.json", result.timelineAsset().objectKey());
    assertEquals("application/json", result.timelineAsset().mimeType());
    assertEquals(180_000, result.timelineAsset().durationMs());
    assertTrue(storageClient.objects.containsKey("videos/work-123.mp4"));
    assertTrue(storageClient.objects.containsKey("timelines/work-123.json"));

    var timeline = objectMapper.readTree(storageClient.objects.get("timelines/work-123.json"));
    assertEquals(FfmpegAlbumVideoRenderService.TEMPLATE_ID, timeline.path("template_id").asText());
    assertEquals("none", timeline.path("lyrics_timing_source").asText());
    assertEquals("none", timeline.path("subtitle_strategy").asText());
    assertEquals(0, timeline.path("lines").size());
  }

  private FfmpegAlbumVideoRenderService.AlbumVideoEncoder successfulEncoder() {
    return (coverPath, audioPath, videoPath, requestedDurationMs) -> {
      assertTrue(Files.isRegularFile(coverPath));
      assertTrue(Files.isRegularFile(audioPath));
      Files.writeString(videoPath, "fake-album-mp4", StandardCharsets.UTF_8);
      return new FfmpegAlbumVideoRenderService.AlbumVideoEncodingResult(
          1920, 1080, 30, requestedDurationMs, 5_400, "h264", "aac", 12_000);
    };
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
        180_000);
  }

  private CapturingStorageClient storageClientWithInputs() {
    CapturingStorageClient storageClient = new CapturingStorageClient();
    storageClient.objects.put("audio/work-123.mp3", "fake-audio".getBytes(StandardCharsets.UTF_8));
    storageClient.objects.put("covers/work-123.png", "fake-cover".getBytes(StandardCharsets.UTF_8));
    return storageClient;
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
}
