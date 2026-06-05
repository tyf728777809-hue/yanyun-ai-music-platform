package com.yanyun.music.dreammaker;

public final class DreamMakerClientException extends RuntimeException {

  private final String failureCode;

  public DreamMakerClientException(String failureCode, String message) {
    super(message);
    this.failureCode = failureCode;
  }

  public DreamMakerClientException(String failureCode, String message, Throwable cause) {
    super(message, cause);
    this.failureCode = failureCode;
  }

  public String failureCode() {
    return failureCode;
  }
}
