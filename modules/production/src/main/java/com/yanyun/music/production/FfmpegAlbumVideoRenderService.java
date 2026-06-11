package com.yanyun.music.production;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.media.MediaAssetDescriptor;
import com.yanyun.music.media.VideoRenderRequest;
import com.yanyun.music.media.VideoRenderResult;
import com.yanyun.music.media.VideoRenderService;
import com.yanyun.music.storage.ObjectStorageClient;
import com.yanyun.music.storage.ObjectStoragePutRequest;
import com.yanyun.music.storage.StoredObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class FfmpegAlbumVideoRenderService implements VideoRenderService {

  static final String TEMPLATE_ID = "album-cover-mp4-v5";
  private static final int WIDTH = 1920;
  private static final int HEIGHT = 1080;
  private static final int FPS = 30;
  private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(3);
  private static final int LOG_TAIL_LIMIT = 1_600;

  private final RenderWorkerProperties properties;
  private final ObjectStorageClient objectStorageClient;
  private final ObjectMapper objectMapper;
  private final AlbumVideoEncoder encoder;

  public FfmpegAlbumVideoRenderService(
      RenderWorkerProperties properties,
      ObjectStorageClient objectStorageClient,
      ObjectMapper objectMapper) {
    this(properties, objectStorageClient, objectMapper, null);
  }

  FfmpegAlbumVideoRenderService(
      RenderWorkerProperties properties,
      ObjectStorageClient objectStorageClient,
      ObjectMapper objectMapper,
      AlbumVideoEncoder encoder) {
    this.properties = properties == null ? new RenderWorkerProperties() : properties;
    if (objectStorageClient == null) {
      throw new IllegalArgumentException("objectStorageClient is required");
    }
    this.objectStorageClient = objectStorageClient;
    this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    this.encoder =
        encoder == null
            ? new ProcessAlbumVideoEncoder(this.properties, this.objectMapper, timeout())
            : encoder;
  }

  @Override
  public VideoRenderResult renderVideo(VideoRenderRequest request) {
    Path tempDirectory = createTempDirectory(request.workId());
    try {
      Path audioPath =
          stageObject(
              tempDirectory,
              request.audioObjectKey(),
              stagedFileName("audio", request.audioObjectKey(), ".mp3"));
      Path coverPath =
          stageObject(
              tempDirectory,
              request.coverObjectKey(),
              stagedFileName("cover", request.coverObjectKey(), ".png"));
      Path videoPath = tempDirectory.resolve("album-video.mp4");

      AlbumVideoEncodingResult encoding =
          encoder.encode(coverPath, audioPath, videoPath, request.durationMs());
      if (!Files.isRegularFile(videoPath)) {
        throw new IllegalStateException("FFmpeg album video output file does not exist");
      }
      int durationMs = requiredPositive(encoding.durationMs(), "duration_ms");
      int durationInFrames =
          encoding.durationInFrames() > 0
              ? encoding.durationInFrames()
              : Math.max(1, Math.round((durationMs / 1000f) * FPS));
      byte[] videoBytes = Files.readAllBytes(videoPath);
      byte[] timelineBytes = timelineJson(request, durationMs, encoding, durationInFrames);
      StoredObject storedVideo =
          objectStorageClient.putObject(
              new ObjectStoragePutRequest(videoObjectKey(request), "video/mp4", videoBytes));
      StoredObject storedTimeline =
          objectStorageClient.putObject(
              new ObjectStoragePutRequest(
                  timelineObjectKey(request), "application/json", timelineBytes));

      return new VideoRenderResult(
          new MediaAssetDescriptor(
              "VIDEO",
              storedVideo.objectKey(),
              "video/mp4",
              storedVideo.sizeBytes(),
              sha256(videoBytes),
              WIDTH,
              HEIGHT,
              durationMs,
              metadata(request, durationInFrames, encoding)),
          new MediaAssetDescriptor(
              "TIMELINE",
              storedTimeline.objectKey(),
              "application/json",
              storedTimeline.sizeBytes(),
              sha256(timelineBytes),
              null,
              null,
              durationMs,
              timelineMetadata(request, durationInFrames, encoding)));
    } catch (IOException exception) {
      throw new IllegalStateException("FFmpeg album video render failed", exception);
    } finally {
      deleteRecursively(tempDirectory);
    }
  }

  private byte[] timelineJson(
      VideoRenderRequest request,
      int durationMs,
      AlbumVideoEncodingResult encoding,
      int durationInFrames)
      throws IOException {
    Map<String, Object> timeline = new LinkedHashMap<>();
    timeline.put("work_id", request.workId());
    timeline.put("template_id", TEMPLATE_ID);
    timeline.put("lyrics_timing_source", "none");
    timeline.put("subtitle_strategy", "none");
    timeline.put("lines", List.of());
    timeline.put(
        "video",
        Map.of(
            "renderer",
            "ffmpeg",
            "width",
            WIDTH,
            "height",
            HEIGHT,
            "fps",
            FPS,
            "duration_ms",
            durationMs,
            "duration_in_frames",
            durationInFrames,
            "render_duration_ms",
            encoding.renderDurationMs()));
    return objectMapper
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(timeline)
        .getBytes(StandardCharsets.UTF_8);
  }

  private Map<String, Object> metadata(
      VideoRenderRequest request, int durationInFrames, AlbumVideoEncodingResult encoding) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("renderer", "ffmpeg");
    metadata.put("source_mode", "album-ffmpeg");
    metadata.put("template_id", TEMPLATE_ID);
    metadata.put("composition_id", "AlbumCoverMp4V5");
    metadata.put("fps", FPS);
    metadata.put("duration_in_frames", durationInFrames);
    metadata.put("audio_object_key", request.audioObjectKey());
    metadata.put("cover_object_key", request.coverObjectKey());
    metadata.put("video_codec", encoding.videoCodec());
    metadata.put("audio_codec", encoding.audioCodec());
    metadata.put("render_duration_ms", encoding.renderDurationMs());
    metadata.put("subtitle_strategy", "none");
    return metadata;
  }

  private Map<String, Object> timelineMetadata(
      VideoRenderRequest request, int durationInFrames, AlbumVideoEncodingResult encoding) {
    Map<String, Object> metadata =
        new LinkedHashMap<>(metadata(request, durationInFrames, encoding));
    metadata.put("timeline_kind", "no_subtitle_album_video");
    return metadata;
  }

  private Path stageObject(Path tempDirectory, String objectKey, String fileName) {
    try {
      Path assetDirectory = tempDirectory.resolve("input-assets");
      Files.createDirectories(assetDirectory);
      Path target = assetDirectory.resolve(fileName).normalize();
      if (!target.startsWith(assetDirectory.normalize())) {
        throw new IllegalStateException("Album video input asset path escapes temp directory");
      }
      Files.write(target, objectStorageClient.getObject(objectKey));
      return target;
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Failed to stage album video input asset: " + objectKey, exception);
    }
  }

  private String stagedFileName(String baseName, String objectKey, String fallbackExtension) {
    String safeBaseName = safeFileSegment(baseName);
    String extension = extensionFromObjectKey(objectKey);
    return safeBaseName + (extension.isBlank() ? fallbackExtension : extension);
  }

  private String extensionFromObjectKey(String objectKey) {
    if (objectKey == null || objectKey.isBlank()) {
      return "";
    }
    String fileName = Path.of(objectKey).getFileName().toString();
    int dotIndex = fileName.lastIndexOf('.');
    if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
      return "";
    }
    String extension = fileName.substring(dotIndex).toLowerCase(java.util.Locale.ROOT);
    return extension.matches("\\.[a-z0-9]{1,8}") ? extension : "";
  }

  private String videoObjectKey(VideoRenderRequest request) {
    return objectKey(properties.getVideoObjectKeyPrefix(), request.workId(), ".mp4");
  }

  private String timelineObjectKey(VideoRenderRequest request) {
    return objectKey(properties.getTimelineObjectKeyPrefix(), request.workId(), ".json");
  }

  private String objectKey(String prefix, String workId, String extension) {
    String safePrefix = trimSlashes(firstNonBlank(prefix, "media"));
    return safePrefix + "/" + safeFileSegment(workId) + extension;
  }

  private Path createTempDirectory(String workId) {
    try {
      return Files.createTempDirectory("yanyun-album-video-" + safeFileSegment(workId) + "-");
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to create album video temp directory", exception);
    }
  }

  private Duration timeout() {
    Duration timeout = properties.getTimeout();
    return timeout == null || timeout.isZero() || timeout.isNegative() ? DEFAULT_TIMEOUT : timeout;
  }

  private int requiredPositive(Integer value, String fieldName) {
    if (value == null || value <= 0) {
      throw new IllegalStateException("FFmpeg album video output field is invalid: " + fieldName);
    }
    return value;
  }

  private void deleteRecursively(Path directory) {
    if (directory == null || !Files.exists(directory)) {
      return;
    }
    try (var paths = Files.walk(directory)) {
      paths.sorted(Comparator.reverseOrder()).forEach(this::deleteQuietly);
    } catch (IOException ignored) {
      // Temporary render files are best-effort cleanup.
    }
  }

  private void deleteQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException ignored) {
      // Temporary render files are best-effort cleanup.
    }
  }

  private String sha256(byte[] value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 digest is unavailable", exception);
    }
  }

  private String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private String trimSlashes(String value) {
    String trimmed = value.trim();
    while (trimmed.startsWith("/")) {
      trimmed = trimmed.substring(1);
    }
    while (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed.isBlank() ? "media" : trimmed;
  }

  private String safeFileSegment(String value) {
    String sanitized = (value == null ? "work" : value).replaceAll("[^a-zA-Z0-9._-]", "-");
    return sanitized.isBlank() ? "work" : sanitized;
  }

  @FunctionalInterface
  interface AlbumVideoEncoder {
    AlbumVideoEncodingResult encode(
        Path coverPath, Path audioPath, Path videoPath, Integer requestedDurationMs)
        throws IOException;
  }

  record AlbumVideoEncodingResult(
      int width,
      int height,
      int fps,
      int durationMs,
      int durationInFrames,
      String videoCodec,
      String audioCodec,
      long renderDurationMs) {}

  private static final class ProcessAlbumVideoEncoder implements AlbumVideoEncoder {

    private final RenderWorkerProperties properties;
    private final ObjectMapper objectMapper;
    private final Duration timeout;

    private ProcessAlbumVideoEncoder(
        RenderWorkerProperties properties, ObjectMapper objectMapper, Duration timeout) {
      this.properties = properties;
      this.objectMapper = objectMapper;
      this.timeout = timeout;
    }

    @Override
    public AlbumVideoEncodingResult encode(
        Path coverPath, Path audioPath, Path videoPath, Integer requestedDurationMs)
        throws IOException {
      Path framePath = videoPath.resolveSibling("album-frame.jpg");
      double audioDurationSeconds = audioDurationSeconds(audioPath, requestedDurationMs);
      long startedAt = System.nanoTime();
      runProcess(
          List.of(
              properties.getFfmpegCommand(),
              "-hide_banner",
              "-loglevel",
              "error",
              "-y",
              "-i",
              coverPath.toAbsolutePath().toString(),
              "-vf",
              "scale=1920:1080:force_original_aspect_ratio=decrease,"
                  + "pad=1920:1080:(ow-iw)/2:(oh-ih)/2:black,"
                  + "eq=contrast=1.03:saturation=0.96:brightness=-0.018,"
                  + "vignette=PI/7,format=yuvj420p",
              "-frames:v",
              "1",
              "-q:v",
              "2",
              framePath.toAbsolutePath().toString()),
          "ffmpeg cover preprocessing");
      runProcess(
          List.of(
              properties.getFfmpegCommand(),
              "-hide_banner",
              "-loglevel",
              "error",
              "-y",
              "-loop",
              "1",
              "-framerate",
              "1",
              "-i",
              framePath.toAbsolutePath().toString(),
              "-i",
              audioPath.toAbsolutePath().toString(),
              "-t",
              String.format(java.util.Locale.ROOT, "%.6f", audioDurationSeconds),
              "-map",
              "0:v:0",
              "-map",
              "1:a:0",
              "-c:v",
              "libx264",
              "-preset",
              "veryfast",
              "-tune",
              "stillimage",
              "-crf",
              "20",
              "-r",
              Integer.toString(FPS),
              "-pix_fmt",
              "yuv420p",
              "-c:a",
              "aac",
              "-b:a",
              "192k",
              "-movflags",
              "+faststart",
              videoPath.toAbsolutePath().toString()),
          "ffmpeg album video encoding");
      long renderDurationMs = Math.round((System.nanoTime() - startedAt) / 1_000_000d);
      ProbeResult probe = inspectVideo(videoPath);
      validateProbe(probe, Math.round(audioDurationSeconds * 1000));
      return new AlbumVideoEncodingResult(
          probe.width(),
          probe.height(),
          FPS,
          Math.toIntExact(probe.durationMs()),
          Math.max(1, Math.round((probe.durationMs() / 1000f) * FPS)),
          probe.videoCodec(),
          probe.audioCodec(),
          renderDurationMs);
    }

    private double audioDurationSeconds(Path audioPath, Integer requestedDurationMs) {
      try {
        String output =
            runProcess(
                    List.of(
                        properties.getFfprobeCommand(),
                        "-v",
                        "error",
                        "-select_streams",
                        "a:0",
                        "-show_entries",
                        "stream=duration:format=duration",
                        "-of",
                        "json",
                        audioPath.toAbsolutePath().toString()),
                    "ffprobe audio duration")
                .trim();
        JsonNode root = objectMapper.readTree(output);
        double streamDuration = root.path("streams").path(0).path("duration").asDouble(0);
        double formatDuration = root.path("format").path("duration").asDouble(0);
        double duration = streamDuration > 0 ? streamDuration : formatDuration;
        if (duration > 0) {
          return duration;
        }
      } catch (IOException exception) {
        throw new IllegalStateException("Failed to inspect album video audio duration", exception);
      }
      if (requestedDurationMs != null && requestedDurationMs > 0) {
        return requestedDurationMs / 1000d;
      }
      throw new IllegalStateException("Album video audio duration is unavailable");
    }

    private ProbeResult inspectVideo(Path videoPath) {
      try {
        String output =
            runProcess(
                    List.of(
                        properties.getFfprobeCommand(),
                        "-v",
                        "error",
                        "-show_entries",
                        "stream=codec_type,codec_name,width,height,duration:format=duration",
                        "-of",
                        "json",
                        videoPath.toAbsolutePath().toString()),
                    "ffprobe album video")
                .trim();
        JsonNode root = objectMapper.readTree(output);
        JsonNode streams = root.path("streams");
        boolean hasVideo = false;
        boolean hasAudio = false;
        String videoCodec = "";
        String audioCodec = "";
        int width = 0;
        int height = 0;
        double audioDuration = 0;
        if (streams.isArray()) {
          for (JsonNode stream : streams) {
            String codecType = stream.path("codec_type").asText("");
            if ("video".equals(codecType) && !hasVideo) {
              hasVideo = true;
              videoCodec = stream.path("codec_name").asText("");
              width = stream.path("width").asInt(0);
              height = stream.path("height").asInt(0);
            } else if ("audio".equals(codecType) && !hasAudio) {
              hasAudio = true;
              audioCodec = stream.path("codec_name").asText("");
              audioDuration = stream.path("duration").asDouble(0);
            }
          }
        }
        long durationMs = Math.round(root.path("format").path("duration").asDouble(0) * 1000);
        long audioDurationMs = Math.round(audioDuration * 1000);
        return new ProbeResult(
            hasVideo, hasAudio, videoCodec, audioCodec, width, height, durationMs, audioDurationMs);
      } catch (IOException exception) {
        throw new IllegalStateException("Failed to inspect album video MP4", exception);
      }
    }

    private void validateProbe(ProbeResult probe, long expectedAudioDurationMs) {
      if (!probe.hasVideo()) {
        throw new IllegalStateException("Album video MP4 is missing video stream");
      }
      if (!probe.hasAudio()) {
        throw new IllegalStateException("Album video MP4 is missing audio stream");
      }
      if (!"h264".equalsIgnoreCase(probe.videoCodec())) {
        throw new IllegalStateException("Album video MP4 video codec is not h264");
      }
      if (!"aac".equalsIgnoreCase(probe.audioCodec())) {
        throw new IllegalStateException("Album video MP4 audio codec is not aac");
      }
      if (probe.width() != WIDTH || probe.height() != HEIGHT) {
        throw new IllegalStateException("Album video MP4 dimensions are not 1920x1080");
      }
      if (probe.durationMs() <= 0) {
        throw new IllegalStateException("Album video MP4 duration is unavailable");
      }
      long driftMs = Math.abs(probe.durationMs() - expectedAudioDurationMs);
      if (driftMs > 1_000L) {
        throw new IllegalStateException("Album video MP4 duration drift is too high");
      }
    }

    private String runProcess(List<String> command, String operation) throws IOException {
      Path logPath = Files.createTempFile("yanyun-" + safeOperation(operation) + "-", ".log");
      try {
        Process process =
            new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(logPath.toFile())
                .start();
        boolean completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!completed) {
          process.destroyForcibly();
          throw new IllegalStateException(operation + " timed out. " + logTail(logPath));
        }
        String output = Files.readString(logPath, StandardCharsets.UTF_8);
        if (process.exitValue() != 0) {
          throw new IllegalStateException(
              operation + " exited with code " + process.exitValue() + ". " + tail(output));
        }
        return output;
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(operation + " was interrupted", exception);
      } finally {
        Files.deleteIfExists(logPath);
      }
    }

    private static String logTail(Path logPath) {
      try {
        return tail(Files.readString(logPath, StandardCharsets.UTF_8));
      } catch (IOException exception) {
        return "Failed to read process log.";
      }
    }

    private static String tail(String value) {
      String trimmed = value == null ? "" : value.trim();
      if (trimmed.length() <= LOG_TAIL_LIMIT) {
        return trimmed;
      }
      return trimmed.substring(trimmed.length() - LOG_TAIL_LIMIT);
    }

    private static String safeOperation(String operation) {
      return operation.replaceAll("[^a-zA-Z0-9._-]", "-");
    }

    private record ProbeResult(
        boolean hasVideo,
        boolean hasAudio,
        String videoCodec,
        String audioCodec,
        int width,
        int height,
        long durationMs,
        long audioDurationMs) {}
  }
}
