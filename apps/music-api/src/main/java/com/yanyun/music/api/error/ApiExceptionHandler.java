package com.yanyun.music.api.error;

import com.yanyun.music.api.idempotency.IdempotencyConflictException;
import com.yanyun.music.lyrics.LyricsCreativeDomainException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<ApiErrorResponse> handleResponseStatus(
      ResponseStatusException exception, HttpServletRequest request) {
    HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
    return ResponseEntity.status(status)
        .body(errorResponse(errorCode(status), exception.getReason(), request));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ApiErrorResponse> handleBadRequest(
      IllegalArgumentException exception, HttpServletRequest request) {
    return ResponseEntity.badRequest()
        .body(errorResponse("VALIDATION_ERROR", exception.getMessage(), request));
  }

  @ExceptionHandler(IdempotencyConflictException.class)
  ResponseEntity<ApiErrorResponse> handleIdempotencyConflict(
      IdempotencyConflictException exception, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(errorResponse("IDEMPOTENCY_CONFLICT", exception.getMessage(), request));
  }

  @ExceptionHandler(LyricsCreativeDomainException.class)
  ResponseEntity<ApiErrorResponse> handleCreativeDomain(
      LyricsCreativeDomainException exception, HttpServletRequest request) {
    return ResponseEntity.badRequest()
        .body(errorResponse("CREATIVE_DOMAIN_REJECTED", exception.getMessage(), request));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiErrorResponse> handleUnknown(Exception exception, HttpServletRequest request) {
    String requestId = requestId(request);
    log.warn(
        "Unhandled API exception. request_id={} path={} exception_type={} message={}",
        requestId,
        request.getRequestURI(),
        exception.getClass().getSimpleName(),
        sanitize(exception.getMessage()),
        exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(errorResponse("INTERNAL_ERROR", "Internal server error", request, requestId));
  }

  private ApiErrorResponse errorResponse(String code, String message, HttpServletRequest request) {
    return errorResponse(code, message, request, requestId(request));
  }

  private ApiErrorResponse errorResponse(
      String code, String message, HttpServletRequest request, String requestId) {
    return new ApiErrorResponse(
        new ApiError(
            code,
            message == null || message.isBlank() ? "Request failed" : message,
            List.of(Map.of("path", request.getRequestURI())),
            requestId,
            OffsetDateTime.now()));
  }

  private String requestId(HttpServletRequest request) {
    String requestId = request.getHeader("X-Request-Id");
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString();
    }
    return requestId;
  }

  private String sanitize(String message) {
    if (message == null || message.isBlank()) {
      return "";
    }
    String sanitized = message.replaceAll("[\\r\\n\\t]+", " ").trim();
    return sanitized.length() <= 240 ? sanitized : sanitized.substring(0, 240);
  }

  private String errorCode(HttpStatus status) {
    return switch (status) {
      case BAD_REQUEST -> "VALIDATION_ERROR";
      case UNAUTHORIZED -> "UNAUTHORIZED";
      case FORBIDDEN -> "FORBIDDEN";
      case NOT_FOUND -> "NOT_FOUND";
      case CONFLICT -> "CONFLICT";
      case TOO_MANY_REQUESTS -> "RATE_LIMITED";
      default -> "INTERNAL_ERROR";
    };
  }

  record ApiErrorResponse(ApiError error) {}

  record ApiError(
      String code,
      String message,
      List<Map<String, String>> details,
      String requestId,
      OffsetDateTime timestamp) {}
}
