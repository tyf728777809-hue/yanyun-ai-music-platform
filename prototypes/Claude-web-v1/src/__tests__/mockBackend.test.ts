import { describe, expect, it } from 'vitest';
import { mockBackend } from '../mock/mockBackend';
import { ApiError } from '../api/client';

// 验证演示后端忠实复刻后端契约的关键边界：润色/续写最多 2 次，第 3 次 409。
describe('mockBackend lyrics edit limit', () => {
  it('allows two AI edits then returns 409 CONFLICT', async () => {
    const created = await mockBackend.createFromLyrics({ lyrics_input: '一段歌词' });
    const id = created.work_id;

    await new Promise((r) => setTimeout(r, 2400));
    await mockBackend.polishLyrics(id, { instruction: '更温柔' });
    await mockBackend.continueLyrics(id, {});

    const detail = await mockBackend.getWork(id);
    expect(detail.polish_used_count).toBe(2);
    expect(detail.polish_remaining_count).toBe(0);
    expect(detail.available_actions).toContain('POLISH_LYRICS');
    expect(detail.available_actions).toContain('CONTINUE_LYRICS');

    await expect(mockBackend.polishLyrics(id, { instruction: '更押韵' })).rejects.toMatchObject({
      httpStatus: 409,
    });
    try {
      await mockBackend.polishLyrics(id, { instruction: '更押韵' });
    } catch (err) {
      expect(err).toBeInstanceOf(ApiError);
      expect((err as ApiError).isQuotaConflict).toBe(true);
    }
  });

  it('rejects empty polish instructions', async () => {
    const created = await mockBackend.createFromLyrics({ lyrics_input: '一段歌词' });

    await expect(mockBackend.polishLyrics(created.work_id, { instruction: '' })).rejects.toMatchObject({
      httpStatus: 400,
    });
  });

  it('exposes RETRY_MUSIC after a simulated music failure and recovers on retry', async () => {
    const created = await mockBackend.createFromLyrics({
      lyrics_input: '触发失败 fail 的歌词',
    });
    const id = created.work_id;

    // 等歌词生成完成再确认出歌。
    await new Promise((r) => setTimeout(r, 2400));
    await mockBackend.confirmWork(id, {});

    // 推进到失败窗口。
    await new Promise((r) => setTimeout(r, 4200));
    const failed = await mockBackend.getWork(id);
    expect(failed.status).toBe('FAILED');
    expect(failed.available_actions).toContain('RETRY_MUSIC');
    expect(failed.failure?.failure_code).toBe('MUSIC_GENERATION_FAILED');

    await mockBackend.retryMusic(id, { music_provider: 'mock' });
    // 重试后跑完生产链路应当成品就绪。
    await new Promise((r) => setTimeout(r, 11000));
    const done = await mockBackend.getWork(id);
    expect(done.status).toBe('GENERATED');
    expect(done.package_status).toBe('PACKAGE_READY');
  }, 20000);
});
