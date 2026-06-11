package com.yanyun.music.api.work;

import com.yanyun.music.api.idempotency.IdempotencyService;
import com.yanyun.music.api.security.MockUserAccessGuard;
import com.yanyun.music.api.work.WorkDtos.ConfirmWorkRequest;
import com.yanyun.music.api.work.WorkDtos.CreateWorkResponse;
import com.yanyun.music.api.work.WorkDtos.InspirationCreateRequest;
import com.yanyun.music.api.work.WorkDtos.JobAcceptedResponse;
import com.yanyun.music.api.work.WorkDtos.LyricsContinueRequest;
import com.yanyun.music.api.work.WorkDtos.LyricsCreateRequest;
import com.yanyun.music.api.work.WorkDtos.LyricsPolishRequest;
import com.yanyun.music.api.work.WorkDtos.PublishPackage;
import com.yanyun.music.api.work.WorkDtos.RetryMusicRequest;
import com.yanyun.music.api.work.WorkDtos.WorkDetail;
import com.yanyun.music.api.work.WorkDtos.WorkListResponse;
import com.yanyun.music.workdomain.WorkStatus;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class WorkController {

  private static final String DEFAULT_MOCK_USER_ID = "mock_user_001";

  private final WorkService workService;
  private final IdempotencyService idempotencyService;
  private final MockUserAccessGuard mockUserAccessGuard;

  public WorkController(
      WorkService workService,
      IdempotencyService idempotencyService,
      MockUserAccessGuard mockUserAccessGuard) {
    this.workService = workService;
    this.idempotencyService = idempotencyService;
    this.mockUserAccessGuard = mockUserAccessGuard;
  }

  @PostMapping("/works/inspiration")
  public ResponseEntity<CreateWorkResponse> createWorkFromInspiration(
      @RequestHeader(
              value = "X-Mock-User-Id",
              required = false,
              defaultValue = DEFAULT_MOCK_USER_ID)
          String userId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody InspirationCreateRequest request) {
    String safeUserId = requireMockUser(userId);
    requireIdempotencyKey(idempotencyKey);
    return ResponseEntity.accepted()
        .body(
            idempotencyService.execute(
                safeUserId,
                idempotencyKey,
                "works.inspiration.create",
                request,
                CreateWorkResponse.class,
                () -> workService.createFromInspiration(safeUserId, request)));
  }

  @PostMapping("/works/lyrics")
  public ResponseEntity<CreateWorkResponse> createWorkFromLyrics(
      @RequestHeader(
              value = "X-Mock-User-Id",
              required = false,
              defaultValue = DEFAULT_MOCK_USER_ID)
          String userId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody LyricsCreateRequest request) {
    String safeUserId = requireMockUser(userId);
    requireIdempotencyKey(idempotencyKey);
    return ResponseEntity.accepted()
        .body(
            idempotencyService.execute(
                safeUserId,
                idempotencyKey,
                "works.lyrics.create",
                request,
                CreateWorkResponse.class,
                () -> workService.createFromLyrics(safeUserId, request)));
  }

  @GetMapping("/works")
  public WorkListResponse listWorks(
      @RequestHeader(
              value = "X-Mock-User-Id",
              required = false,
              defaultValue = DEFAULT_MOCK_USER_ID)
          String userId,
      @RequestParam(value = "status", required = false) WorkStatus status,
      @RequestParam(value = "page", required = false, defaultValue = "1") int page,
      @RequestParam(value = "page_size", required = false, defaultValue = "20") int pageSize) {
    String safeUserId = requireMockUser(userId);
    return workService.listWorks(safeUserId, status, page, pageSize);
  }

  @GetMapping("/works/{work_id}")
  public WorkDetail getWork(
      @RequestHeader(
              value = "X-Mock-User-Id",
              required = false,
              defaultValue = DEFAULT_MOCK_USER_ID)
          String userId,
      @PathVariable("work_id") UUID workId) {
    String safeUserId = requireMockUser(userId);
    return workService.getWork(safeUserId, workId);
  }

  @PostMapping("/works/{work_id}/lyrics/polish")
  public ResponseEntity<JobAcceptedResponse> polishLyrics(
      @RequestHeader(
              value = "X-Mock-User-Id",
              required = false,
              defaultValue = DEFAULT_MOCK_USER_ID)
          String userId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @PathVariable("work_id") UUID workId,
      @RequestBody LyricsPolishRequest request) {
    String safeUserId = requireMockUser(userId);
    requireIdempotencyKey(idempotencyKey);
    return ResponseEntity.accepted()
        .body(
            idempotencyService.execute(
                safeUserId,
                idempotencyKey,
                "works.lyrics.polish",
                fingerprint(workId, request),
                JobAcceptedResponse.class,
                () -> workService.polishLyrics(safeUserId, workId, request)));
  }

  @PostMapping("/works/{work_id}/lyrics/continue")
  public ResponseEntity<JobAcceptedResponse> continueLyrics(
      @RequestHeader(
              value = "X-Mock-User-Id",
              required = false,
              defaultValue = DEFAULT_MOCK_USER_ID)
          String userId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @PathVariable("work_id") UUID workId,
      @RequestBody(required = false) LyricsContinueRequest request) {
    String safeUserId = requireMockUser(userId);
    requireIdempotencyKey(idempotencyKey);
    return ResponseEntity.accepted()
        .body(
            idempotencyService.execute(
                safeUserId,
                idempotencyKey,
                "works.lyrics.continue",
                fingerprint(workId, request),
                JobAcceptedResponse.class,
                () -> workService.continueLyrics(safeUserId, workId, request)));
  }

  @PostMapping("/works/{work_id}/confirm")
  public ResponseEntity<JobAcceptedResponse> confirmWork(
      @RequestHeader(
              value = "X-Mock-User-Id",
              required = false,
              defaultValue = DEFAULT_MOCK_USER_ID)
          String userId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @PathVariable("work_id") UUID workId,
      @RequestBody(required = false) ConfirmWorkRequest request) {
    String safeUserId = requireMockUser(userId);
    requireIdempotencyKey(idempotencyKey);
    return ResponseEntity.accepted()
        .body(
            idempotencyService.execute(
                safeUserId,
                idempotencyKey,
                "works.confirm",
                fingerprint(workId, request),
                JobAcceptedResponse.class,
                () -> workService.confirmWork(safeUserId, workId, request)));
  }

  @PostMapping("/works/{work_id}/cover/regenerate")
  public ResponseEntity<JobAcceptedResponse> regenerateCover(
      @RequestHeader(
              value = "X-Mock-User-Id",
              required = false,
              defaultValue = DEFAULT_MOCK_USER_ID)
          String userId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @PathVariable("work_id") UUID workId) {
    String safeUserId = requireMockUser(userId);
    requireIdempotencyKey(idempotencyKey);
    return ResponseEntity.accepted()
        .body(
            idempotencyService.execute(
                safeUserId,
                idempotencyKey,
                "works.cover.regenerate",
                fingerprint(workId, null),
                JobAcceptedResponse.class,
                () -> workService.regenerateCover(safeUserId, workId)));
  }

  @PostMapping("/works/{work_id}/music/retry")
  public ResponseEntity<JobAcceptedResponse> retryMusic(
      @RequestHeader(
              value = "X-Mock-User-Id",
              required = false,
              defaultValue = DEFAULT_MOCK_USER_ID)
          String userId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @PathVariable("work_id") UUID workId,
      @RequestBody(required = false) RetryMusicRequest request) {
    String safeUserId = requireMockUser(userId);
    requireIdempotencyKey(idempotencyKey);
    return ResponseEntity.accepted()
        .body(
            idempotencyService.execute(
                safeUserId,
                idempotencyKey,
                "works.music.retry",
                fingerprint(workId, request),
                JobAcceptedResponse.class,
                () -> workService.retryMusic(safeUserId, workId, request)));
  }

  @PostMapping("/works/{work_id}/video/rerender")
  public ResponseEntity<JobAcceptedResponse> rerenderVideo(
      @RequestHeader(
              value = "X-Mock-User-Id",
              required = false,
              defaultValue = DEFAULT_MOCK_USER_ID)
          String userId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @PathVariable("work_id") UUID workId) {
    String safeUserId = requireMockUser(userId);
    requireIdempotencyKey(idempotencyKey);
    return ResponseEntity.accepted()
        .body(
            idempotencyService.execute(
                safeUserId,
                idempotencyKey,
                "works.video.rerender",
                fingerprint(workId, null),
                JobAcceptedResponse.class,
                () -> workService.rerenderVideo(safeUserId, workId)));
  }

  @GetMapping("/works/{work_id}/publish-package")
  public PublishPackage getPublishPackage(
      @RequestHeader(
              value = "X-Mock-User-Id",
              required = false,
              defaultValue = DEFAULT_MOCK_USER_ID)
          String userId,
      @PathVariable("work_id") UUID workId) {
    String safeUserId = requireMockUser(userId);
    return workService.getPublishPackage(safeUserId, workId);
  }

  @PostMapping("/works/{work_id}/publish-package/mark-fetched")
  public PublishPackage markPublishPackageFetched(
      @RequestHeader(
              value = "X-Mock-User-Id",
              required = false,
              defaultValue = DEFAULT_MOCK_USER_ID)
          String userId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @PathVariable("work_id") UUID workId) {
    String safeUserId = requireMockUser(userId);
    requireIdempotencyKey(idempotencyKey);
    return idempotencyService.execute(
        safeUserId,
        idempotencyKey,
        "works.publish-package.mark-fetched",
        fingerprint(workId, null),
        PublishPackage.class,
        () -> workService.markPublishPackageFetched(safeUserId, workId));
  }

  @PostMapping("/works/{work_id}/publish-package/refresh-url")
  public PublishPackage refreshPublishPackageUrl(
      @RequestHeader(
              value = "X-Mock-User-Id",
              required = false,
              defaultValue = DEFAULT_MOCK_USER_ID)
          String userId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @PathVariable("work_id") UUID workId) {
    String safeUserId = requireMockUser(userId);
    requireIdempotencyKey(idempotencyKey);
    return idempotencyService.execute(
        safeUserId,
        idempotencyKey,
        "works.publish-package.refresh-url",
        fingerprint(workId, null),
        PublishPackage.class,
        () -> workService.refreshPublishPackageUrl(safeUserId, workId));
  }

  private void requireIdempotencyKey(String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() < 8) {
      throw new IllegalArgumentException("Idempotency-Key header must be at least 8 characters");
    }
  }

  private String requireMockUser(String userId) {
    return mockUserAccessGuard.requireAllowed(userId);
  }

  private Map<String, Object> fingerprint(UUID workId, Object request) {
    Map<String, Object> fingerprint = new HashMap<>();
    fingerprint.put("workId", workId);
    fingerprint.put("request", request);
    return fingerprint;
  }
}
