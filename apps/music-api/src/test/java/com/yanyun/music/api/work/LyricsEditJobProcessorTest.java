package com.yanyun.music.api.work;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.lyrics.LyricsGenerationRequest;
import com.yanyun.music.lyrics.LyricsGenerationResult;
import com.yanyun.music.lyrics.LyricsGenerationService;
import com.yanyun.music.lyrics.LyricsGenerationTransientException;
import com.yanyun.music.production.lyrics.LyricsEditJobProcessor;
import com.yanyun.music.workdomain.CreationMode;
import com.yanyun.music.workdomain.GenerationStage;
import com.yanyun.music.workdomain.PackageStatus;
import com.yanyun.music.workdomain.WorkStatus;
import com.yanyun.music.workpersistence.WorkRepository;
import com.yanyun.music.workpersistence.WorkRepository.LyricsDraftRow;
import com.yanyun.music.workpersistence.WorkRepository.LyricsEditJobRow;
import com.yanyun.music.workpersistence.WorkRepository.WorkRow;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

class LyricsEditJobProcessorTest {

  private final WorkRepository workRepository = mock(WorkRepository.class);
  private final LyricsGenerationService lyricsGenerationService =
      mock(LyricsGenerationService.class);
  private final LyricsEditJobProcessor processor =
      new LyricsEditJobProcessor(
          workRepository,
          lyricsGenerationService,
          new ObjectMapper(),
          new NoopTransactionManager());

  @Test
  void writesNewDraftAndMarksJobSucceededWhenLyricsEditCompletes() {
    UUID workId = UUID.randomUUID();
    UUID sourceDraftId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    WorkRow work = work(workId);
    LyricsDraftRow draft = draft(workId, sourceDraftId, 1);
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(Optional.of(work))
        .thenReturn(Optional.of(work));
    when(workRepository.findLatestLyricsDraft(workId))
        .thenReturn(Optional.of(draft))
        .thenReturn(Optional.of(draft));
    when(lyricsGenerationService.generate(any(LyricsGenerationRequest.class)))
        .thenReturn(
            new LyricsGenerationResult(
                "新歌名",
                "新摘要",
                "[Verse]\n新歌词",
                "new music prompt",
                "new cover seed",
                List.of(),
                List.of("清河"),
                "kb-v1",
                Map.of("lyrics", 7),
                BigDecimal.valueOf(0.88)));
    when(workRepository.markLyricsReadyIfVersion(workId, "user-1", 3, "新歌名", "新摘要", true))
        .thenReturn(true);
    when(workRepository.nextLyricsVersion(workId)).thenReturn(2);

    processor.process(job(jobId, workId, sourceDraftId, "POLISH"));

    verify(workRepository).insertLyricsDraft(any(LyricsDraftRow.class));
    verify(workRepository)
        .insertGenerationJob(
            eq(workId),
            eq("LYRICS_POLISH"),
            eq("SUCCEEDED"),
            eq(GenerationStage.WAITING_CONFIRM),
            any(OffsetDateTime.class),
            any(OffsetDateTime.class));
    verify(workRepository).markLyricsEditJobSucceeded(jobId);
  }

  @Test
  void keepsJobRetryableWhenLyricsProviderFailsTransiently() {
    UUID workId = UUID.randomUUID();
    UUID sourceDraftId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    when(workRepository.findWorkForUser(workId, "user-1")).thenReturn(Optional.of(work(workId)));
    when(workRepository.findLatestLyricsDraft(workId))
        .thenReturn(Optional.of(draft(workId, sourceDraftId, 1)));
    when(lyricsGenerationService.generate(any(LyricsGenerationRequest.class)))
        .thenThrow(new LyricsGenerationTransientException("provider timeout", null));

    processor.process(job(jobId, workId, sourceDraftId, "CONTINUE"));

    verify(workRepository)
        .markLyricsEditJobFailed(
            eq(jobId), eq("LYRICS_GENERATION_TRANSIENT"), eq("provider timeout"), eq(true));
  }

  @Test
  void writesInitialDraftAndMarksWorkReadyWhenLyricsCreationCompletes() {
    UUID workId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    WorkRow work =
        new WorkRow(
            workId,
            "YYM-20260615-ABCDEF",
            "user-1",
            CreationMode.INSPIRATION,
            WorkStatus.LYRICS_GENERATING,
            GenerationStage.LYRICS_GENERATING,
            PackageStatus.PACKAGE_NOT_READY,
            "正在写词",
            "AI 正在根据你的灵感创作歌词。",
            0,
            0,
            null,
            null,
            null,
            null,
            false,
            false,
            0,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            null,
            1);
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(Optional.of(work))
        .thenReturn(Optional.of(work));
    when(lyricsGenerationService.generate(any(LyricsGenerationRequest.class)))
        .thenReturn(
            new LyricsGenerationResult(
                "初稿",
                "初稿摘要",
                "[Verse]\n初稿歌词",
                "music prompt",
                "cover seed",
                List.of(),
                List.of("清河"),
                "kb-v1",
                Map.of("lyrics", 7),
                BigDecimal.valueOf(0.9)));
    when(workRepository.markLyricsReadyFromGeneration(workId, "user-1", "初稿", "初稿摘要"))
        .thenReturn(true);
    when(workRepository.nextLyricsVersion(workId)).thenReturn(1);

    processor.process(
        new LyricsEditJobRow(
            jobId,
            workId,
            "user-1",
            "CREATE_INSPIRATION",
            null,
            null,
            null,
            "RUNNING",
            null,
            null,
            false,
            1,
            2,
            null,
            null,
            OffsetDateTime.now(),
            null,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            "{\"story_input\":\"清河小路上的少年\",\"music_style\":\"R&B\"}"));

    verify(workRepository)
        .markGenerationJobRunning(jobId, workId, GenerationStage.LYRICS_GENERATING);
    verify(workRepository).insertLyricsDraft(any(LyricsDraftRow.class));
    verify(workRepository)
        .completeGenerationJob(jobId, "SUCCEEDED", GenerationStage.WAITING_CONFIRM, null, null);
    verify(workRepository).markLyricsEditJobSucceeded(jobId);
  }

  private WorkRow work(UUID workId) {
    return new WorkRow(
        workId,
        "YYM-20260615-ABCDEF",
        "user-1",
        CreationMode.LYRICS,
        WorkStatus.LYRICS_READY,
        GenerationStage.WAITING_CONFIRM,
        PackageStatus.PACKAGE_NOT_READY,
        "旧歌名",
        "旧摘要",
        0,
        0,
        null,
        null,
        null,
        null,
        false,
        false,
        0,
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        null,
        3);
  }

  private LyricsDraftRow draft(UUID workId, UUID draftId, int versionNo) {
    return new LyricsDraftRow(
        draftId,
        workId,
        versionNo,
        "旧歌名",
        "旧摘要",
        "[Verse]\n旧歌词",
        "old prompt",
        "[]",
        "[]",
        "old cover seed",
        BigDecimal.valueOf(0.8),
        "kb-v1",
        "{}",
        OffsetDateTime.now());
  }

  private LyricsEditJobRow job(UUID jobId, UUID workId, UUID draftId, String operation) {
    return new LyricsEditJobRow(
        jobId,
        workId,
        "user-1",
        operation,
        "更好听",
        draftId,
        1,
        "RUNNING",
        null,
        null,
        false,
        1,
        2,
        null,
        null,
        OffsetDateTime.now(),
        null,
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        "{}");
  }

  private static final class NoopTransactionManager implements PlatformTransactionManager {
    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) {
      return new SimpleTransactionStatus();
    }

    @Override
    public void commit(TransactionStatus status) {}

    @Override
    public void rollback(TransactionStatus status) {}
  }
}
