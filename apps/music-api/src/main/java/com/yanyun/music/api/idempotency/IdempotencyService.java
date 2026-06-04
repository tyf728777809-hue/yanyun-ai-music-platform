package com.yanyun.music.api.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.api.work.WorkRepository;
import com.yanyun.music.api.work.WorkRepository.IdempotencyRecord;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IdempotencyService {

  private final WorkRepository workRepository;
  private final ObjectMapper objectMapper;

  public IdempotencyService(WorkRepository workRepository, ObjectMapper objectMapper) {
    this.workRepository = workRepository;
    this.objectMapper = objectMapper;
  }

  @Transactional(noRollbackFor = ResponseStatusException.class)
  public <T> T execute(
      String userId,
      String idempotencyKey,
      String operation,
      Object requestFingerprint,
      Class<T> responseType,
      Supplier<T> operationSupplier) {
    String requestHash = hashJson(requestFingerprint);
    return workRepository
        .findIdempotency(userId, idempotencyKey, operation)
        .map(record -> replay(record, requestHash, responseType))
        .orElseGet(
            () -> {
              T response = operationSupplier.get();
              workRepository.insertIdempotency(
                  userId,
                  idempotencyKey,
                  operation,
                  requestHash,
                  writeJson(response),
                  OffsetDateTime.now().plusHours(24));
              return response;
            });
  }

  private <T> T replay(IdempotencyRecord record, String requestHash, Class<T> responseType) {
    if (!requestHash.equals(record.requestHash())) {
      throw new IdempotencyConflictException(
          "Idempotency-Key was already used with a different request");
    }
    try {
      return objectMapper.readValue(record.responseJson(), responseType);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to replay idempotent response", exception);
    }
  }

  private String hashJson(Object value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of()
          .formatHex(digest.digest(writeJson(value).getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 digest is unavailable", exception);
    }
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to write JSON", exception);
    }
  }
}
