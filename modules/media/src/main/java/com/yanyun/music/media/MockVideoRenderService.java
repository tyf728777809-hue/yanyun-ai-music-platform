package com.yanyun.music.media;

import java.util.Map;

public final class MockVideoRenderService implements VideoRenderService {

  private static final int DEFAULT_DURATION_MS = 180_000;

  @Override
  public VideoRenderResult renderVideo(VideoRenderRequest request) {
    int durationMs = request.durationMs() == null ? DEFAULT_DURATION_MS : request.durationMs();
    return new VideoRenderResult(
        new MediaAssetDescriptor(
            "VIDEO",
            "videos/" + request.workId() + ".mp4",
            "video/mp4",
            12_000_000L,
            "mock-video",
            1920,
            1080,
            durationMs,
            Map.of(
                "renderer",
                "mock-remotion-ffmpeg",
                "profile",
                "video-16x9-1920x1080",
                "audio_object_key",
                request.audioObjectKey(),
                "cover_object_key",
                request.coverObjectKey())),
        new MediaAssetDescriptor(
            "TIMELINE",
            "timelines/" + request.workId() + ".json",
            "application/json",
            64_000L,
            "mock-timeline",
            null,
            null,
            durationMs,
            Map.of(
                "renderer",
                "mock-timeline",
                "lyrics_source",
                "workflow-input",
                "safe_area",
                "mobile-first")));
  }
}
