package com.yanyun.music.musicprovider;

public record MusicGenerationResult(
    MusicProviderType providerType,
    String providerTaskId,
    MusicGenerationStatus status,
    String audioObjectKey,
    Integer durationMs,
    String failureCode,
    String failureMessage,
    String message) {

  public static MusicGenerationResult accepted(
      MusicProviderType providerType, String providerTaskId) {
    return new MusicGenerationResult(
        providerType, providerTaskId, MusicGenerationStatus.QUEUED, null, null, null, null, null);
  }

  public static MusicGenerationResult succeeded(
      MusicProviderType providerType,
      String providerTaskId,
      String audioObjectKey,
      Integer durationMs,
      String message) {
    return new MusicGenerationResult(
        providerType,
        providerTaskId,
        MusicGenerationStatus.SUCCEEDED,
        audioObjectKey,
        durationMs,
        null,
        null,
        message);
  }

  public static MusicGenerationResult failed(
      MusicProviderType providerType,
      String providerTaskId,
      String failureCode,
      String failureMessage) {
    return new MusicGenerationResult(
        providerType,
        providerTaskId,
        MusicGenerationStatus.FAILED,
        null,
        null,
        failureCode,
        failureMessage,
        failureMessage);
  }
}
