package com.yanyun.music.api.idempotency;

public class IdempotencyConflictException extends RuntimeException {

  public IdempotencyConflictException(String message) {
    super(message);
  }
}
