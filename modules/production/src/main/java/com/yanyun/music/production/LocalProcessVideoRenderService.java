package com.yanyun.music.production;

import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class LocalProcessVideoRenderService implements VideoRenderService {

  private static final int LOG_TAIL_LIMIT = 1_600;
  private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(10);

  private final RenderWorkerProperties properties;
  private final ObjectStorageClient objectStorageClient;
  private final ObjectMapper objectMapper;

  public LocalProcessVideoRenderService(
      RenderWorkerProperties properties,
      ObjectStorageClient objectStorageClient,
      ObjectMapper objectMapper) {
    this.properties = properties == null ? new RenderWorkerProperties() : properties;
    if (objectStorageClient == null) {
      throw new IllegalArgumentException("objectStorageClient is required");
    }
    this.objectStorageClient = objectStorageClient;
    this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
  }

  @Override
  public VideoRenderResult renderVideo(VideoRenderRequest request) {
    Path tempDirectory = createTempDirectory(request.workId());
    try {
      Path inputPath = tempDirectory.resolve("render-input.json");
      Path outputPath = tempDirectory.resolve("render-output.json");
      Path outputDirectory = tempDirectory.resolve("assets");
      Path logPath = tempDirectory.resolve("render-worker.log");
      Files.createDirectories(outputDirectory);
      objectMapper.writeValue(inputPath.toFile(), jobInput(request));

      ProcessBuilder processBuilder =
          new ProcessBuilder(command(inputPath, outputPath, outputDirectory));
      Path workingDirectory = resolveWorkingDirectory();
      if (workingDirectory != null) {
        processBuilder.directory(workingDirectory.toFile());
      }
      processBuilder.redirectErrorStream(true);
      processBuilder.redirectOutput(logPath.toFile());

      Process process = processBuilder.start();
      boolean completed = process.waitFor(timeout().toMillis(), TimeUnit.MILLISECONDS);
      if (!completed) {
        process.destroyForcibly();
        throw new IllegalStateException("Render worker timed out. " + logTail(logPath));
      }
      if (process.exitValue() != 0) {
        throw new IllegalStateException(
            "Render worker exited with code " + process.exitValue() + ". " + logTail(logPath));
      }

      RenderWorkerJobOutput output =
          objectMapper.readValue(outputPath.toFile(), RenderWorkerJobOutput.class);
      Path videoPath = resolveOutputFile(tempDirectory, output.videoFilePath());
      Path timelinePath = resolveOutputFile(tempDirectory, output.timelineFilePath());
      int width = requiredPositive(output.width(), "width");
      int height = requiredPositive(output.height(), "height");
      int fps = requiredPositive(output.fps(), "fps");
      int durationMs = requiredPositive(output.durationMs(), "duration_ms");
      int durationInFrames = requiredPositive(output.durationInFrames(), "duration_in_frames");
      String renderer = firstNonBlank(output.renderer(), "remotion");
      String compositionId = firstNonBlank(output.compositionId(), properties.getCompositionId());
      byte[] videoBytes = Files.readAllBytes(videoPath);
      byte[] timelineBytes = Files.readAllBytes(timelinePath);
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
              width,
              height,
              durationMs,
              metadata(request, renderer, compositionId, fps, durationInFrames, "local-process")),
          new MediaAssetDescriptor(
              "TIMELINE",
              storedTimeline.objectKey(),
              "application/json",
              storedTimeline.sizeBytes(),
              sha256(timelineBytes),
              null,
              null,
              durationMs,
              metadata(request, renderer, compositionId, fps, durationInFrames, "local-process")));
    } catch (IOException exception) {
      throw new IllegalStateException("Render worker local process failed", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Render worker local process was interrupted", exception);
    } finally {
      deleteRecursively(tempDirectory);
    }
  }

  private RenderWorkerJobInput jobInput(VideoRenderRequest request) {
    return new RenderWorkerJobInput(
        request.workId(),
        firstNonBlank(request.songTitle(), "燕云新曲"),
        firstNonBlank(request.songSummary(), "一首来自燕云乐坊的原创曲。"),
        firstNonBlank(request.lyricsText(), "这一曲，把江湖唱给你听"),
        request.audioObjectKey(),
        firstNonBlank(request.audioMimeType(), "audio/mpeg"),
        request.coverObjectKey(),
        request.durationMs(),
        firstNonBlank(properties.getCompositionId(), "LyricVideo16x9"));
  }

  private List<String> command(Path inputPath, Path outputPath, Path outputDirectory) {
    if (properties.getCommand() == null || properties.getCommand().isBlank()) {
      throw new IllegalArgumentException("render worker command is required");
    }
    List<String> command = new ArrayList<>();
    command.add(properties.getCommand());
    command.addAll(properties.getArguments() == null ? List.of() : properties.getArguments());
    command.add("--input");
    command.add(inputPath.toAbsolutePath().toString());
    command.add("--output");
    command.add(outputPath.toAbsolutePath().toString());
    command.add("--out-dir");
    command.add(outputDirectory.toAbsolutePath().toString());
    return command;
  }

  private Path resolveWorkingDirectory() {
    if (properties.getWorkingDirectory() == null || properties.getWorkingDirectory().isBlank()) {
      return null;
    }
    Path configured = Path.of(properties.getWorkingDirectory());
    if (configured.isAbsolute()) {
      return requireExistingDirectory(configured.normalize());
    }
    Path currentDirectory = Path.of("").toAbsolutePath().normalize();
    Path currentRelative = currentDirectory.resolve(configured).normalize();
    if (Files.isDirectory(currentRelative)) {
      return currentRelative;
    }
    for (Path cursor = currentDirectory.getParent(); cursor != null; cursor = cursor.getParent()) {
      Path candidate = cursor.resolve(configured).normalize();
      if (Files.isDirectory(candidate)) {
        return candidate;
      }
    }
    return requireExistingDirectory(currentRelative);
  }

  private Path requireExistingDirectory(Path directory) {
    if (!Files.isDirectory(directory)) {
      throw new IllegalStateException(
          "Render worker working directory does not exist: " + directory);
    }
    return directory;
  }

  private Path createTempDirectory(String workId) {
    try {
      return Files.createTempDirectory("yanyun-render-" + safeFileSegment(workId) + "-");
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to create render temp directory", exception);
    }
  }

  private Path resolveOutputFile(Path tempDirectory, String outputPath) {
    if (outputPath == null || outputPath.isBlank()) {
      throw new IllegalStateException("Render worker output path is missing");
    }
    Path path = Path.of(outputPath);
    Path resolved = path.isAbsolute() ? path.normalize() : tempDirectory.resolve(path).normalize();
    if (!resolved.startsWith(tempDirectory.normalize())) {
      throw new IllegalStateException("Render worker output path escapes temp directory");
    }
    if (!Files.isRegularFile(resolved)) {
      throw new IllegalStateException("Render worker output file does not exist: " + resolved);
    }
    return resolved;
  }

  private Map<String, Object> metadata(
      VideoRenderRequest request,
      String renderer,
      String compositionId,
      int fps,
      int durationInFrames,
      String mode) {
    return Map.of(
        "renderer",
        renderer,
        "source_mode",
        mode,
        "composition_id",
        compositionId,
        "fps",
        fps,
        "duration_in_frames",
        durationInFrames,
        "audio_object_key",
        request.audioObjectKey(),
        "cover_object_key",
        request.coverObjectKey());
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

  private Duration timeout() {
    Duration timeout = properties.getTimeout();
    return timeout == null || timeout.isZero() || timeout.isNegative() ? DEFAULT_TIMEOUT : timeout;
  }

  private int requiredPositive(Integer value, String fieldName) {
    if (value == null || value <= 0) {
      throw new IllegalStateException("Render worker output field is invalid: " + fieldName);
    }
    return value;
  }

  private String logTail(Path logPath) {
    try {
      if (!Files.isRegularFile(logPath)) {
        return "No render worker log was written.";
      }
      String log = Files.readString(logPath, StandardCharsets.UTF_8).trim();
      if (log.length() <= LOG_TAIL_LIMIT) {
        return log;
      }
      return log.substring(log.length() - LOG_TAIL_LIMIT);
    } catch (IOException exception) {
      return "Failed to read render worker log.";
    }
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

  private record RenderWorkerJobInput(
      @JsonProperty("work_id") String workId,
      @JsonProperty("song_title") String songTitle,
      @JsonProperty("song_summary") String songSummary,
      @JsonProperty("lyrics_text") String lyricsText,
      @JsonProperty("audio_object_key") String audioObjectKey,
      @JsonProperty("audio_mime_type") String audioMimeType,
      @JsonProperty("cover_object_key") String coverObjectKey,
      @JsonProperty("duration_ms") Integer durationMs,
      @JsonProperty("composition_id") String compositionId) {}

  private record RenderWorkerJobOutput(
      @JsonProperty("work_id") String workId,
      @JsonProperty("video_file_path") String videoFilePath,
      @JsonProperty("timeline_file_path") String timelineFilePath,
      @JsonProperty("width") Integer width,
      @JsonProperty("height") Integer height,
      @JsonProperty("fps") Integer fps,
      @JsonProperty("duration_ms") Integer durationMs,
      @JsonProperty("duration_in_frames") Integer durationInFrames,
      @JsonProperty("renderer") String renderer,
      @JsonProperty("composition_id") String compositionId) {}
}
