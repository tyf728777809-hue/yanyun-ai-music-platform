package com.yanyun.music.api.idempotency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.api.work.WorkDtos.QuotaHint;
import com.yanyun.music.workpersistence.WorkRepository;
import com.yanyun.music.workpersistence.WorkRepository.IdempotencyRecord;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IdempotencyServiceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final WorkRepository workRepository = org.mockito.Mockito.mock(WorkRepository.class);
  private final IdempotencyService idempotencyService =
      new IdempotencyService(workRepository, objectMapper);

  @Test
  void replaysStoredResponseWhenRequestHashMatches() throws Exception {
    QuotaHint storedResponse =
        new QuotaHint(false, "ON_PACKAGE_READY", 999, 2, "Local mock quota available");
    String requestHash = hash(Map.of("field", "value"));
    when(workRepository.findIdempotency("user-1", "key-1", "operation-1"))
        .thenReturn(
            Optional.of(
                new IdempotencyRecord(
                    "user-1",
                    "key-1",
                    "operation-1",
                    requestHash,
                    objectMapper.writeValueAsString(storedResponse),
                    OffsetDateTime.now(),
                    OffsetDateTime.now().plusHours(1))));

    QuotaHint response =
        idempotencyService.execute(
            "user-1",
            "key-1",
            "operation-1",
            Map.of("field", "value"),
            QuotaHint.class,
            () -> {
              throw new AssertionError("operation should not run");
            });

    assertEquals(storedResponse, response);
    verify(workRepository, never()).insertIdempotency(any(), any(), any(), any(), any(), any());
  }

  @Test
  void rejectsSameKeyWithDifferentRequest() throws Exception {
    when(workRepository.findIdempotency("user-1", "key-1", "operation-1"))
        .thenReturn(
            Optional.of(
                new IdempotencyRecord(
                    "user-1",
                    "key-1",
                    "operation-1",
                    hash(Map.of("field", "old")),
                    objectMapper.writeValueAsString(
                        new QuotaHint(false, "ON_PACKAGE_READY", 999, 2, "ok")),
                    OffsetDateTime.now(),
                    OffsetDateTime.now().plusHours(1))));

    assertThrows(
        IdempotencyConflictException.class,
        () ->
            idempotencyService.execute(
                "user-1",
                "key-1",
                "operation-1",
                Map.of("field", "new"),
                QuotaHint.class,
                () -> new QuotaHint(false, "ON_PACKAGE_READY", 999, 2, "new")));
  }

  @Test
  void storesResponseWhenKeyIsNew() {
    when(workRepository.findIdempotency("user-1", "key-1", "operation-1"))
        .thenReturn(Optional.empty());

    QuotaHint response =
        idempotencyService.execute(
            "user-1",
            "key-1",
            "operation-1",
            Map.of("field", "value"),
            QuotaHint.class,
            () -> new QuotaHint(false, "ON_PACKAGE_READY", 999, 2, "ok"));

    assertEquals("ok", response.message());
    verify(workRepository)
        .insertIdempotency(eq("user-1"), eq("key-1"), eq("operation-1"), any(), any(), any());
  }

  private String hash(Object value) {
    return idempotencyServiceHash(value);
  }

  private String idempotencyServiceHash(Object value) {
    return new TestHashSupport(objectMapper).hash(value);
  }
}
