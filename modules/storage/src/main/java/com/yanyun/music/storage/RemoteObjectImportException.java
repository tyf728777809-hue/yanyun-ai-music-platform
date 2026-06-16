package com.yanyun.music.storage;

public final class RemoteObjectImportException extends IllegalStateException {

  private final Integer statusCode;
  private final boolean retryable;

  public RemoteObjectImportException(
      String message, Integer statusCode, boolean retryable, Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
    this.retryable = retryable;
  }

  public Integer statusCode() {
    return statusCode;
  }

  public boolean retryable() {
    return retryable;
  }
}
