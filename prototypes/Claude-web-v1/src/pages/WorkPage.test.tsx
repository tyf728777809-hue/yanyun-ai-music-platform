import { render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ToastProvider } from '../components/Toast';
import { service } from '../mock/service';
import type { WorkDetail } from '../api/types';
import { WorkPage } from './WorkPage';

function work(overrides: Partial<WorkDetail> = {}): WorkDetail {
  return {
    work_id: 'work-2',
    work_code: 'YY-0002',
    creation_mode: 'INSPIRATION',
    status: 'LYRICS_READY',
    generation_stage: 'WAITING_CONFIRM',
    package_status: 'PACKAGE_NOT_READY',
    song_title: '旧雨新词',
    song_summary: '一首用于测试页面切换加载态的歌',
    lyrics_draft: {
      lyrics_draft_id: 'draft-2',
      version_no: 1,
      song_title: '旧雨新词',
      song_summary: '一首用于测试页面切换加载态的歌',
      lyrics_text: '[Verse]\n旧雨落在新灯前',
      music_prompt: 'pop',
      yanyun_references: [],
      risk_notes: [],
    },
    media_assets: null,
    polish_used_count: 0,
    polish_remaining_count: 2,
    quota_hint: null,
    failure: null,
    available_actions: ['POLISH_LYRICS', 'CONTINUE_LYRICS', 'CONFIRM_WORK'],
    publish_handoff_hint: null,
    created_at: '2026-06-06T00:00:00Z',
    updated_at: '2026-06-06T00:00:00Z',
    generated_at: null,
    ...overrides,
  };
}

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

function renderWorkPage(workId: string) {
  return render(
    <ToastProvider>
      <WorkPage workId={workId} onBackToHome={() => {}} />
    </ToastProvider>,
  );
}

describe('WorkPage', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('keeps the loading view when a stale aborted request settles during work switching', async () => {
    const first = deferred<WorkDetail>();
    const second = deferred<WorkDetail>();

    vi.spyOn(service, 'getWork').mockImplementation((workId, signal) => {
      const request = workId === 'work-1' ? first : second;
      signal?.addEventListener('abort', () => {
        request.reject(new DOMException('Aborted', 'AbortError'));
      });
      return request.promise;
    });

    const { rerender } = renderWorkPage('work-1');

    rerender(
      <ToastProvider>
        <WorkPage workId="work-2" onBackToHome={() => {}} />
      </ToastProvider>,
    );

    await waitFor(() => expect(service.getWork).toHaveBeenCalledTimes(2));
    expect(screen.queryByText('作品状态无法识别')).not.toBeInTheDocument();
    expect(screen.getByText('正在打开作品')).toBeInTheDocument();

    second.resolve(work({ work_id: 'work-2' }));

    expect(await screen.findByText('旧雨新词')).toBeInTheDocument();
    expect(screen.queryByText('作品状态无法识别')).not.toBeInTheDocument();
  });
});
