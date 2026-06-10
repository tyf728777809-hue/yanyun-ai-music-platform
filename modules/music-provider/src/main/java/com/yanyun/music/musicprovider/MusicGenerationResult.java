package com.yanyun.music.musicprovider;

import java.util.LinkedHashMap;
import java.util.Map;

public record MusicGenerationResult(
    MusicProviderType providerType,
    String providerTaskId,
    String modelName,
    MusicGenerationStatus status,
    String audioObjectKey,
    String audioSourceUrl,
    String audioContentType,
    Integer durationMs,
    String failureCode,
    String failureMessage,
    String message,
    Map<String, Object> metadata) {

  public MusicGenerationResult {
    if (metadata == null || metadata.isEmpty()) {
      metadata = Map.of();
    } else {
      Map<String, Object> sanitizedMetadata = new LinkedHashMap<>();
      metadata.forEach(
          (key, value) -> {
            if (key != null && value != null) {
              sanitizedMetadata.put(key, value);
            }
          });
      metadata = Map.copyOf(sanitizedMetadata);
    }
  }

  public static MusicGenerationResult accepted(
      MusicProviderType providerType, String providerTaskId) {
    return new MusicGenerationResult(
        providerType,
        providerTaskId,
        null,
        MusicGenerationStatus.QUEUED,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public static MusicGenerationResult succeeded(
      MusicProviderType providerType,
      String providerTaskId,
      String audioObjectKey,
      Integer durationMs,
      String message) {
    return succeeded(providerType, providerTaskId, null, audioObjectKey, durationMs, message);
  }

  public static MusicGenerationResult succeeded(
      MusicProviderType providerType,
      String providerTaskId,
      String modelName,
      String audioObjectKey,
      Integer durationMs,
      String message) {
    return new MusicGenerationResult(
        providerType,
        providerTaskId,
        modelName,
        MusicGenerationStatus.SUCCEEDED,
        audioObjectKey,
        null,
        null,
        durationMs,
        null,
        null,
        message,
        Map.of());
  }

  public static MusicGenerationResult succeededFromSource(
      MusicProviderType providerType,
      String providerTaskId,
      String audioSourceUrl,
      String audioContentType,
      Integer durationMs,
      String message) {
    return succeededFromSource(
        providerType, providerTaskId, null, audioSourceUrl, audioContentType, durationMs, message);
  }

  public static MusicGenerationResult succeededFromSource(
      MusicProviderType providerType,
      String providerTaskId,
      String modelName,
      String audioSourceUrl,
      String audioContentType,
      Integer durationMs,
      String message) {
    return succeededFromSource(
        providerType,
        providerTaskId,
        modelName,
        audioSourceUrl,
        audioContentType,
        durationMs,
        message,
        Map.of());
  }

  public static MusicGenerationResult succeededFromSource(
      MusicProviderType providerType,
      String providerTaskId,
      String modelName,
      String audioSourceUrl,
      String audioContentType,
      Integer durationMs,
      String message,
      Map<String, Object> metadata) {
    return new MusicGenerationResult(
        providerType,
        providerTaskId,
        modelName,
        MusicGenerationStatus.SUCCEEDED,
        null,
        audioSourceUrl,
        audioContentType,
        durationMs,
        null,
        null,
        message,
        metadata);
  }

  public static MusicGenerationResult failed(
      MusicProviderType providerType,
      String providerTaskId,
      String failureCode,
      String failureMessage) {
    return failed(providerType, providerTaskId, null, failureCode, failureMessage);
  }

  public static MusicGenerationResult failed(
      MusicProviderType providerType,
      String providerTaskId,
      String modelName,
      String failureCode,
      String failureMessage) {
    return new MusicGenerationResult(
        providerType,
        providerTaskId,
        modelName,
        MusicGenerationStatus.FAILED,
        null,
        null,
        null,
        null,
        failureCode,
        failureMessage,
        failureMessage,
        Map.of());
  }
}
