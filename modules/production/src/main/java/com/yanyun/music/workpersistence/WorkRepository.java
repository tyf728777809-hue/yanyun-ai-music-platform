package com.yanyun.music.workpersistence;

import com.yanyun.music.workdomain.CreationMode;
import com.yanyun.music.workdomain.FailureCode;
import com.yanyun.music.workdomain.GenerationStage;
import com.yanyun.music.workdomain.PackageStatus;
import com.yanyun.music.workdomain.WorkStatus;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WorkRepository {

  private final JdbcTemplate jdbcTemplate;

  public WorkRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void insertWork(
      WorkRow work, String userNameSnapshot, boolean quotaLocked, boolean quotaCommitted) {
    jdbcTemplate.update(
        """
        INSERT INTO works (
          id,
          work_code,
          user_id,
          user_name_snapshot,
          creation_mode,
          status,
          generation_stage,
          package_status,
          song_title,
          song_summary,
          quota_locked,
          quota_committed
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        work.id(),
        work.workCode(),
        work.userId(),
        userNameSnapshot,
        work.creationMode().name(),
        work.status().name(),
        work.generationStage().name(),
        work.packageStatus().name(),
        work.songTitle(),
        work.songSummary(),
        quotaLocked,
        quotaCommitted);
  }

  public void insertWorkInput(
      UUID workId,
      String storyInput,
      String lyricsInput,
      String mood,
      String scene,
      String relationship,
      String musicStyle,
      String vocalPreference,
      String inputSnapshotJson) {
    jdbcTemplate.update(
        """
        INSERT INTO work_inputs (
          work_id,
          story_input,
          lyrics_input,
          mood,
          scene,
          relationship,
          music_style,
          vocal_preference,
          input_snapshot_json
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
        """,
        workId,
        storyInput,
        lyricsInput,
        mood,
        scene,
        relationship,
        musicStyle,
        vocalPreference,
        inputSnapshotJson);
  }

  public void insertLyricsDraft(LyricsDraftRow draft) {
    jdbcTemplate.update(
        """
        INSERT INTO lyrics_drafts (
          id,
          work_id,
          version_no,
          song_title,
          song_summary,
          lyrics_text,
          structured_lyrics_json,
          sections_json,
          music_prompt,
          risk_notes_json,
          yanyun_references_json,
          cover_prompt_seed,
          quality_score,
          knowledge_base_version,
          prompt_template_versions
        )
        VALUES (?, ?, ?, ?, ?, ?, '{}'::jsonb, NULL, ?, ?::jsonb, ?::jsonb, ?, ?, ?, ?::jsonb)
        """,
        draft.id(),
        draft.workId(),
        draft.versionNo(),
        draft.songTitle(),
        draft.songSummary(),
        draft.lyricsText(),
        draft.musicPrompt(),
        draft.riskNotesJson(),
        draft.yanyunReferencesJson(),
        draft.coverPromptSeed(),
        draft.qualityScore(),
        draft.knowledgeBaseVersion(),
        draft.promptTemplateVersionsJson());
  }

  public UUID insertGenerationJob(
      UUID workId,
      String jobType,
      String status,
      GenerationStage stage,
      OffsetDateTime startedAt,
      OffsetDateTime completedAt) {
    UUID jobId = UUID.randomUUID();
    insertGenerationJob(jobId, workId, jobType, status, stage, startedAt, completedAt);
    return jobId;
  }

  public void insertGenerationJob(
      UUID jobId,
      UUID workId,
      String jobType,
      String status,
      GenerationStage stage,
      OffsetDateTime startedAt,
      OffsetDateTime completedAt) {
    jdbcTemplate.update(
        """
        INSERT INTO generation_jobs (
          id,
          work_id,
          job_type,
          status,
          stage,
          started_at,
          completed_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """,
        jobId,
        workId,
        jobType,
        status,
        stage == null ? null : stage.name(),
        startedAt,
        completedAt);
  }

  public void completeGenerationJob(
      UUID jobId,
      String status,
      GenerationStage stage,
      FailureCode failureCode,
      String failureMessage) {
    jdbcTemplate.update(
        """
        UPDATE generation_jobs
        SET status = ?,
            stage = ?,
            completed_at = now(),
            failure_code = ?,
            failure_message = ?,
            updated_at = now()
        WHERE id = ?
        """,
        status,
        stage == null ? null : stage.name(),
        failureCode == null ? null : failureCode.name(),
        failureMessage,
        jobId);
  }

  public void upsertGenerationJobStep(GenerationJobStepRow step) {
    jdbcTemplate.update(
        """
        INSERT INTO generation_job_steps (
          id,
          job_id,
          work_id,
          step_name,
          idempotency_key,
          status,
          attempt_count,
          external_trace_id,
          failure_code,
          failure_message,
          started_at,
          completed_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (job_id, step_name, idempotency_key)
        DO UPDATE SET
          status = EXCLUDED.status,
          attempt_count = EXCLUDED.attempt_count,
          external_trace_id = EXCLUDED.external_trace_id,
          failure_code = EXCLUDED.failure_code,
          failure_message = EXCLUDED.failure_message,
          started_at = COALESCE(generation_job_steps.started_at, EXCLUDED.started_at),
          completed_at = EXCLUDED.completed_at,
          updated_at = now()
        """,
        step.id(),
        step.jobId(),
        step.workId(),
        step.stepName(),
        step.idempotencyKey(),
        step.status(),
        step.attemptCount(),
        step.externalTraceId(),
        step.failureCode(),
        step.failureMessage(),
        step.startedAt(),
        step.completedAt());
  }

  public List<GenerationJobStepRow> findGenerationJobSteps(UUID jobId) {
    return jdbcTemplate.query(
        """
        SELECT id,
               job_id,
               work_id,
               step_name,
               idempotency_key,
               status,
               attempt_count,
               external_trace_id,
               failure_code,
               failure_message,
               started_at,
               completed_at,
               created_at,
               updated_at
        FROM generation_job_steps
        WHERE job_id = ?
        ORDER BY created_at, step_name
        """,
        this::mapGenerationJobStep,
        jobId);
  }

  public Optional<WorkRow> findWorkForUser(UUID workId, String userId) {
    try {
      return Optional.of(
          jdbcTemplate.queryForObject(
              """
              SELECT *
              FROM works
              WHERE id = ? AND user_id = ?
              """,
              this::mapWork,
              workId,
              userId));
    } catch (EmptyResultDataAccessException exception) {
      return Optional.empty();
    }
  }

  public List<WorkRow> listWorks(String userId, WorkStatus status, int limit, int offset) {
    if (status == null) {
      return jdbcTemplate.query(
          """
          SELECT *
          FROM works
          WHERE user_id = ?
          ORDER BY updated_at DESC
          LIMIT ? OFFSET ?
          """,
          this::mapWork,
          userId,
          limit,
          offset);
    }
    return jdbcTemplate.query(
        """
        SELECT *
        FROM works
        WHERE user_id = ? AND status = ?
        ORDER BY updated_at DESC
        LIMIT ? OFFSET ?
        """,
        this::mapWork,
        userId,
        status.name(),
        limit,
        offset);
  }

  public long countWorks(String userId, WorkStatus status) {
    if (status == null) {
      return jdbcTemplate.queryForObject(
          "SELECT count(*) FROM works WHERE user_id = ?", Long.class, userId);
    }
    return jdbcTemplate.queryForObject(
        "SELECT count(*) FROM works WHERE user_id = ? AND status = ?",
        Long.class,
        userId,
        status.name());
  }

  public Optional<LyricsDraftRow> findLatestLyricsDraft(UUID workId) {
    try {
      return Optional.of(
          jdbcTemplate.queryForObject(
              """
              SELECT *
              FROM lyrics_drafts
              WHERE work_id = ?
              ORDER BY version_no DESC
              LIMIT 1
              """,
              this::mapLyricsDraft,
              workId));
    } catch (EmptyResultDataAccessException exception) {
      return Optional.empty();
    }
  }

  public int nextLyricsVersion(UUID workId) {
    Integer version =
        jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(version_no), 0) + 1 FROM lyrics_drafts WHERE work_id = ?",
            Integer.class,
            workId);
    return version == null ? 1 : version;
  }

  public void markLyricsReady(UUID workId, String title, String summary, boolean incrementPolish) {
    jdbcTemplate.update(
        """
        UPDATE works
        SET status = ?,
            generation_stage = ?,
            song_title = ?,
            song_summary = ?,
            polish_used_count = polish_used_count + ?,
            updated_at = now(),
            version = version + 1
        WHERE id = ?
        """,
        WorkStatus.LYRICS_READY.name(),
        GenerationStage.WAITING_CONFIRM.name(),
        title,
        summary,
        incrementPolish ? 1 : 0,
        workId);
  }

  public void markPackageReady(
      UUID workId, String title, String summary, boolean quotaLocked, boolean quotaCommitted) {
    jdbcTemplate.update(
        """
        UPDATE works
        SET status = ?,
            generation_stage = ?,
            package_status = ?,
            song_title = ?,
            song_summary = ?,
            failure_code = NULL,
            failure_message = NULL,
            retryable = NULL,
            failed_at = NULL,
            quota_locked = ?,
            quota_committed = ?,
            generated_at = now(),
            updated_at = now(),
            version = version + 1
        WHERE id = ?
        """,
        WorkStatus.GENERATED.name(),
        GenerationStage.PACKAGE_READY.name(),
        PackageStatus.PACKAGE_READY.name(),
        title,
        summary,
        quotaLocked,
        quotaCommitted,
        workId);
  }

  public void markFailure(
      UUID workId, FailureCode failureCode, String failureMessage, boolean retryable) {
    jdbcTemplate.update(
        """
        UPDATE works
        SET status = ?,
            generation_stage = ?,
            package_status = ?,
            failure_code = ?,
            failure_message = ?,
            retryable = ?,
            failed_at = now(),
            updated_at = now(),
            version = version + 1
        WHERE id = ?
        """,
        WorkStatus.FAILED.name(),
        GenerationStage.FAILED.name(),
        failureCode == FailureCode.PACKAGE_BLOCKED
            ? PackageStatus.PACKAGE_BLOCKED.name()
            : PackageStatus.PACKAGE_NOT_READY.name(),
        failureCode.name(),
        failureMessage,
        retryable,
        workId);
  }

  public boolean reserveSongProduction(UUID workId, String userId, int expectedVersion) {
    int updated =
        jdbcTemplate.update(
            """
            UPDATE works
            SET status = ?,
                generation_stage = ?,
                package_status = ?,
                failure_code = NULL,
                failure_message = NULL,
                retryable = NULL,
                failed_at = NULL,
                updated_at = now(),
                version = version + 1
            WHERE id = ?
              AND user_id = ?
              AND version = ?
              AND status = ?
            """,
            WorkStatus.GENERATING.name(),
            GenerationStage.QUOTA_LOCKING.name(),
            PackageStatus.PACKAGE_NOT_READY.name(),
            workId,
            userId,
            expectedVersion,
            WorkStatus.LYRICS_READY.name());
    return updated == 1;
  }

  public boolean reserveMusicRetry(
      UUID workId, String userId, int expectedVersion, int retryLimit) {
    int updated =
        jdbcTemplate.update(
            """
            UPDATE works
            SET status = ?,
                generation_stage = ?,
                package_status = ?,
                failure_code = NULL,
                failure_message = NULL,
                retryable = NULL,
                failed_at = NULL,
                music_retry_count = music_retry_count + 1,
                updated_at = now(),
                version = version + 1
            WHERE id = ?
              AND user_id = ?
              AND version = ?
              AND status = ?
              AND retryable IS TRUE
              AND failure_code IN (?, ?, ?, ?)
              AND music_retry_count < ?
            """,
            WorkStatus.GENERATING.name(),
            GenerationStage.QUOTA_LOCKING.name(),
            PackageStatus.PACKAGE_NOT_READY.name(),
            workId,
            userId,
            expectedVersion,
            WorkStatus.FAILED.name(),
            FailureCode.MUSIC_GENERATION_FAILED.name(),
            FailureCode.MUSIC_QUALITY_FAILED.name(),
            FailureCode.PROVIDER_TIMEOUT.name(),
            FailureCode.RATE_LIMITED.name(),
            retryLimit);
    return updated == 1;
  }

  public void upsertMediaAsset(MediaAssetRow asset) {
    jdbcTemplate.update(
        """
        INSERT INTO media_assets (
          work_id,
          asset_type,
          object_key,
          mime_type,
          file_size_bytes,
          checksum,
          width,
          height,
          duration_ms,
          metadata_json
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
        ON CONFLICT (work_id, asset_type)
        DO UPDATE SET
          object_key = EXCLUDED.object_key,
          mime_type = EXCLUDED.mime_type,
          file_size_bytes = EXCLUDED.file_size_bytes,
          checksum = EXCLUDED.checksum,
          width = EXCLUDED.width,
          height = EXCLUDED.height,
          duration_ms = EXCLUDED.duration_ms,
          metadata_json = EXCLUDED.metadata_json,
          created_at = now()
        """,
        asset.workId(),
        asset.assetType(),
        asset.objectKey(),
        asset.mimeType(),
        asset.fileSizeBytes(),
        asset.checksum(),
        asset.width(),
        asset.height(),
        asset.durationMs(),
        asset.metadataJson());
  }

  public List<MediaAssetRow> findMediaAssets(UUID workId) {
    return jdbcTemplate.query(
        """
        SELECT *
        FROM media_assets
        WHERE work_id = ?
        """,
        this::mapMediaAsset,
        workId);
  }

  public void upsertPublishPackage(PublishPackageRow publishPackage) {
    jdbcTemplate.update(
        """
        INSERT INTO publish_packages (
          work_id,
          package_status,
          package_json,
          package_object_key,
          package_url,
          package_url_expires_at,
          last_url_refreshed_at
        )
        VALUES (?, ?, ?::jsonb, ?, ?, ?, now())
        ON CONFLICT (work_id)
        DO UPDATE SET
          package_status = EXCLUDED.package_status,
          package_json = EXCLUDED.package_json,
          package_object_key = EXCLUDED.package_object_key,
          package_url = EXCLUDED.package_url,
          package_url_expires_at = EXCLUDED.package_url_expires_at,
          last_url_refreshed_at = now(),
          updated_at = now()
        """,
        publishPackage.workId(),
        publishPackage.packageStatus().name(),
        publishPackage.packageJson(),
        publishPackage.packageObjectKey(),
        publishPackage.packageUrl(),
        publishPackage.packageUrlExpiresAt());
  }

  public Optional<PublishPackageRow> findPublishPackage(UUID workId) {
    try {
      return Optional.of(
          jdbcTemplate.queryForObject(
              """
              SELECT id,
                     work_id,
                     package_status,
                     package_json::text AS package_json,
                     package_object_key,
                     package_url,
                     package_url_expires_at,
                     fetched_at,
                     created_at,
                     updated_at
              FROM publish_packages
              WHERE work_id = ?
              """,
              this::mapPublishPackage,
              workId));
    } catch (EmptyResultDataAccessException exception) {
      return Optional.empty();
    }
  }

  public void markPackageFetched(UUID workId) {
    jdbcTemplate.update(
        """
        UPDATE publish_packages
        SET package_status = ?,
            fetched_at = now(),
            updated_at = now()
        WHERE work_id = ?
        """,
        PackageStatus.PACKAGE_FETCHED.name(),
        workId);
    jdbcTemplate.update(
        """
        UPDATE works
        SET package_status = ?,
            updated_at = now(),
            version = version + 1
        WHERE id = ?
        """,
        PackageStatus.PACKAGE_FETCHED.name(),
        workId);
  }

  public void updatePublishPackageUrl(UUID workId, String packageUrl, OffsetDateTime expiresAt) {
    jdbcTemplate.update(
        """
        UPDATE publish_packages
        SET package_status = ?,
            package_url = ?,
            package_url_expires_at = ?,
            last_url_refreshed_at = now(),
            updated_at = now()
        WHERE work_id = ?
        """,
        PackageStatus.PACKAGE_READY.name(),
        packageUrl,
        expiresAt,
        workId);
    jdbcTemplate.update(
        """
        UPDATE works
        SET package_status = ?,
            updated_at = now(),
            version = version + 1
        WHERE id = ?
        """,
        PackageStatus.PACKAGE_READY.name(),
        workId);
  }

  public void insertQuotaTransaction(
      UUID workId, String userId, String lockId, String action, String status, String reason) {
    jdbcTemplate.update(
        """
        INSERT INTO quota_transactions (
          work_id,
          user_id,
          external_lock_id,
          action,
          status,
          reason
        )
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        workId,
        userId,
        lockId,
        action,
        status,
        reason);
  }

  public void insertProviderCall(ProviderCallRow call) {
    jdbcTemplate.update(
        """
        INSERT INTO provider_calls (
          work_id,
          job_id,
          provider,
          operation,
          model_name,
          request_hash,
          prompt_hash,
          provider_trace_id,
          status,
          latency_ms,
          cost_units,
          error_code,
          error_message
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        call.workId(),
        call.jobId(),
        call.provider(),
        call.operation(),
        call.modelName(),
        call.requestHash(),
        call.promptHash(),
        call.providerTraceId(),
        call.status(),
        call.latencyMs(),
        call.costUnits(),
        call.errorCode(),
        call.errorMessage());
  }

  public Optional<IdempotencyRecord> findIdempotency(
      String userId, String idempotencyKey, String operation) {
    try {
      return Optional.of(
          jdbcTemplate.queryForObject(
              """
              SELECT user_id,
                     idempotency_key,
                     operation,
                     request_hash,
                     response_json::text AS response_json,
                     created_at,
                     expires_at
              FROM idempotency_keys
              WHERE user_id = ? AND idempotency_key = ? AND operation = ?
                AND (expires_at IS NULL OR expires_at > now())
              """,
              this::mapIdempotencyRecord,
              userId,
              idempotencyKey,
              operation));
    } catch (EmptyResultDataAccessException exception) {
      return Optional.empty();
    }
  }

  public void insertIdempotency(
      String userId,
      String idempotencyKey,
      String operation,
      String requestHash,
      String responseJson,
      OffsetDateTime expiresAt) {
    jdbcTemplate.update(
        """
        INSERT INTO idempotency_keys (
          user_id,
          idempotency_key,
          operation,
          request_hash,
          response_json,
          expires_at
        )
        VALUES (?, ?, ?, ?, ?::jsonb, ?)
        """,
        userId,
        idempotencyKey,
        operation,
        requestHash,
        responseJson,
        expiresAt);
  }

  private WorkRow mapWork(ResultSet resultSet, int rowNum) throws SQLException {
    String failureCode = resultSet.getString("failure_code");
    return new WorkRow(
        resultSet.getObject("id", UUID.class),
        resultSet.getString("work_code"),
        resultSet.getString("user_id"),
        CreationMode.valueOf(resultSet.getString("creation_mode")),
        WorkStatus.valueOf(resultSet.getString("status")),
        GenerationStage.valueOf(resultSet.getString("generation_stage")),
        PackageStatus.valueOf(resultSet.getString("package_status")),
        resultSet.getString("song_title"),
        resultSet.getString("song_summary"),
        resultSet.getInt("polish_used_count"),
        resultSet.getInt("cover_regen_count"),
        failureCode == null ? null : FailureCode.valueOf(failureCode),
        resultSet.getString("failure_message"),
        nullableBoolean(resultSet, "retryable"),
        resultSet.getObject("failed_at", OffsetDateTime.class),
        resultSet.getBoolean("quota_locked"),
        resultSet.getBoolean("quota_committed"),
        resultSet.getInt("music_retry_count"),
        resultSet.getObject("created_at", OffsetDateTime.class),
        resultSet.getObject("updated_at", OffsetDateTime.class),
        resultSet.getObject("generated_at", OffsetDateTime.class),
        resultSet.getInt("version"));
  }

  private LyricsDraftRow mapLyricsDraft(ResultSet resultSet, int rowNum) throws SQLException {
    return new LyricsDraftRow(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("work_id", UUID.class),
        resultSet.getInt("version_no"),
        resultSet.getString("song_title"),
        resultSet.getString("song_summary"),
        resultSet.getString("lyrics_text"),
        resultSet.getString("music_prompt"),
        jsonText(resultSet, "risk_notes_json"),
        jsonText(resultSet, "yanyun_references_json"),
        resultSet.getString("cover_prompt_seed"),
        resultSet.getBigDecimal("quality_score"),
        resultSet.getString("knowledge_base_version"),
        jsonText(resultSet, "prompt_template_versions"),
        resultSet.getObject("created_at", OffsetDateTime.class));
  }

  private MediaAssetRow mapMediaAsset(ResultSet resultSet, int rowNum) throws SQLException {
    return new MediaAssetRow(
        resultSet.getObject("work_id", UUID.class),
        resultSet.getString("asset_type"),
        resultSet.getString("object_key"),
        resultSet.getString("mime_type"),
        nullableLong(resultSet, "file_size_bytes"),
        resultSet.getString("checksum"),
        nullableInteger(resultSet, "width"),
        nullableInteger(resultSet, "height"),
        nullableInteger(resultSet, "duration_ms"),
        jsonText(resultSet, "metadata_json"));
  }

  private PublishPackageRow mapPublishPackage(ResultSet resultSet, int rowNum) throws SQLException {
    return new PublishPackageRow(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("work_id", UUID.class),
        PackageStatus.valueOf(resultSet.getString("package_status")),
        resultSet.getString("package_json"),
        resultSet.getString("package_object_key"),
        resultSet.getString("package_url"),
        resultSet.getObject("package_url_expires_at", OffsetDateTime.class),
        resultSet.getObject("fetched_at", OffsetDateTime.class),
        resultSet.getObject("created_at", OffsetDateTime.class),
        resultSet.getObject("updated_at", OffsetDateTime.class));
  }

  private GenerationJobStepRow mapGenerationJobStep(ResultSet resultSet, int rowNum)
      throws SQLException {
    return new GenerationJobStepRow(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("job_id", UUID.class),
        resultSet.getObject("work_id", UUID.class),
        resultSet.getString("step_name"),
        resultSet.getString("idempotency_key"),
        resultSet.getString("status"),
        resultSet.getInt("attempt_count"),
        resultSet.getString("external_trace_id"),
        resultSet.getString("failure_code"),
        resultSet.getString("failure_message"),
        resultSet.getObject("started_at", OffsetDateTime.class),
        resultSet.getObject("completed_at", OffsetDateTime.class),
        resultSet.getObject("created_at", OffsetDateTime.class),
        resultSet.getObject("updated_at", OffsetDateTime.class));
  }

  private IdempotencyRecord mapIdempotencyRecord(ResultSet resultSet, int rowNum)
      throws SQLException {
    return new IdempotencyRecord(
        resultSet.getString("user_id"),
        resultSet.getString("idempotency_key"),
        resultSet.getString("operation"),
        resultSet.getString("request_hash"),
        resultSet.getString("response_json"),
        resultSet.getObject("created_at", OffsetDateTime.class),
        resultSet.getObject("expires_at", OffsetDateTime.class));
  }

  private Boolean nullableBoolean(ResultSet resultSet, String columnName) throws SQLException {
    boolean value = resultSet.getBoolean(columnName);
    return resultSet.wasNull() ? null : value;
  }

  private Integer nullableInteger(ResultSet resultSet, String columnName) throws SQLException {
    int value = resultSet.getInt(columnName);
    return resultSet.wasNull() ? null : value;
  }

  private Long nullableLong(ResultSet resultSet, String columnName) throws SQLException {
    long value = resultSet.getLong(columnName);
    return resultSet.wasNull() ? null : value;
  }

  private String jsonText(ResultSet resultSet, String columnName) throws SQLException {
    Object object = resultSet.getObject(columnName);
    return object == null ? "[]" : object.toString();
  }

  public record WorkRow(
      UUID id,
      String workCode,
      String userId,
      CreationMode creationMode,
      WorkStatus status,
      GenerationStage generationStage,
      PackageStatus packageStatus,
      String songTitle,
      String songSummary,
      int polishUsedCount,
      int coverRegenCount,
      FailureCode failureCode,
      String failureMessage,
      Boolean retryable,
      OffsetDateTime failedAt,
      boolean quotaLocked,
      boolean quotaCommitted,
      int musicRetryCount,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt,
      OffsetDateTime generatedAt,
      int version) {}

  public record LyricsDraftRow(
      UUID id,
      UUID workId,
      int versionNo,
      String songTitle,
      String songSummary,
      String lyricsText,
      String musicPrompt,
      String riskNotesJson,
      String yanyunReferencesJson,
      String coverPromptSeed,
      BigDecimal qualityScore,
      String knowledgeBaseVersion,
      String promptTemplateVersionsJson,
      OffsetDateTime createdAt) {}

  public record MediaAssetRow(
      UUID workId,
      String assetType,
      String objectKey,
      String mimeType,
      Long fileSizeBytes,
      String checksum,
      Integer width,
      Integer height,
      Integer durationMs,
      String metadataJson) {}

  public record PublishPackageRow(
      UUID id,
      UUID workId,
      PackageStatus packageStatus,
      String packageJson,
      String packageObjectKey,
      String packageUrl,
      OffsetDateTime packageUrlExpiresAt,
      OffsetDateTime fetchedAt,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {}

  public record ProviderCallRow(
      UUID workId,
      UUID jobId,
      String provider,
      String operation,
      String modelName,
      String requestHash,
      String promptHash,
      String providerTraceId,
      String status,
      Integer latencyMs,
      BigDecimal costUnits,
      String errorCode,
      String errorMessage) {}

  public record GenerationJobStepRow(
      UUID id,
      UUID jobId,
      UUID workId,
      String stepName,
      String idempotencyKey,
      String status,
      int attemptCount,
      String externalTraceId,
      String failureCode,
      String failureMessage,
      OffsetDateTime startedAt,
      OffsetDateTime completedAt,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {}

  public record IdempotencyRecord(
      String userId,
      String idempotencyKey,
      String operation,
      String requestHash,
      String responseJson,
      OffsetDateTime createdAt,
      OffsetDateTime expiresAt) {}
}
