import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../../api/client';
import { ToastProvider } from '../../components/Toast';
import { service } from '../../mock/service';
import type { WorkDetail } from '../../api/types';
import { ConfirmView } from './ConfirmView';

function work(overrides: Partial<WorkDetail> = {}): WorkDetail {
  return {
    work_id: 'work-1',
    work_code: 'YY-0001',
    creation_mode: 'INSPIRATION',
    status: 'LYRICS_READY',
    generation_stage: 'WAITING_CONFIRM',
    package_status: 'PACKAGE_NOT_READY',
    song_title: '燕云未名曲',
    song_summary: '一首关于山河与故人的歌',
    lyrics_draft: {
      lyrics_draft_id: 'draft-1',
      version_no: 3,
      song_title: '燕云未名曲',
      song_summary: '一首关于山河与故人的歌',
      lyrics_text: '[主歌]\n提灯夜行',
      music_prompt: '国风民谣',
      yanyun_references: ['清河镇'],
      risk_notes: [],
    },
    media_assets: null,
    polish_used_count: 2,
    polish_remaining_count: 0,
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

describe('ConfirmView', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('keeps backend-provided edit actions visible when edit quota is exhausted but shows exhausted copy', () => {
    render(
      <ToastProvider>
        <ConfirmView work={work()} refresh={async () => {}} onBackToHome={() => {}} />
      </ToastProvider>,
    );

    expect(screen.getByText('AI 改词次数已用完，确认满意后即可出歌。')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'AI 润色' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'AI 续写' })).toBeInTheDocument();
  });

  it('keeps polish submit disabled until an instruction is provided', () => {
    render(
      <ToastProvider>
        <ConfirmView
          work={work({ polish_used_count: 0, polish_remaining_count: 2 })}
          refresh={async () => {}}
          onBackToHome={() => {}}
        />
      </ToastProvider>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'AI 润色' }));

    expect(screen.queryByText('请先写下润色方向。')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: '开始润色' })).toBeDisabled();

    fireEvent.change(screen.getByRole('textbox', { name: /想让 AI/ }), {
      target: { value: '更押韵一点' },
    });

    expect(screen.queryByText('请先写下润色方向。')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: '开始润色' })).not.toBeDisabled();
  });

  it('keeps the old lyrics visible and disables edit actions while a lyrics edit job is active', () => {
    render(
      <ToastProvider>
        <ConfirmView
          work={work({
            polish_used_count: 0,
            polish_remaining_count: 2,
            active_lyrics_job: {
              job_id: 'job-1',
              operation: 'POLISH',
              status: 'RUNNING',
              message: 'AI 正在润色歌词，原歌词会保留。',
              source_version_no: 3,
              created_at: '2026-06-06T00:00:00Z',
              updated_at: '2026-06-06T00:00:00Z',
            },
          })}
          refresh={async () => {}}
          onBackToHome={() => {}}
        />
      </ToastProvider>,
    );

    expect(screen.getByText('AI 正在润色')).toBeInTheDocument();
    expect(screen.getByText('AI 正在润色歌词，原歌词会保留。')).toBeInTheDocument();
    expect(screen.getByText((content) => content.includes('[主歌]') && content.includes('提灯夜行')))
      .toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'AI 润色' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'AI 续写' })).toBeDisabled();
    expect(screen.getByRole('button', { name: '确认出歌' })).toBeDisabled();
  });

  it('keeps focus in the edit textarea while typing', async () => {
    render(
      <ToastProvider>
        <ConfirmView
          work={work({ polish_used_count: 0, polish_remaining_count: 2 })}
          refresh={async () => {}}
          onBackToHome={() => {}}
        />
      </ToastProvider>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'AI 润色' }));
    const textarea = screen.getByRole('textbox', { name: /想让 AI/ });
    await waitFor(() => expect(textarea).toHaveFocus());

    fireEvent.change(textarea, { target: { value: '更' } });
    await waitFor(() => expect(textarea).toHaveFocus());

    fireEvent.change(textarea, { target: { value: '更押' } });
    await waitFor(() => expect(textarea).toHaveFocus());
  });

  it('copies lyrics from the confirm page with clipboard fallback support', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText },
    });

    render(
      <ToastProvider>
        <ConfirmView work={work()} refresh={async () => {}} onBackToHome={() => {}} />
      </ToastProvider>,
    );

    fireEvent.click(screen.getByRole('button', { name: '复制歌词' }));

    await waitFor(() => expect(writeText).toHaveBeenCalledWith('[主歌]\n提灯夜行'));
    expect(await screen.findByText('歌词已复制')).toBeInTheDocument();
  });

  it('shows a readable error when polish instruction only contains spaces', () => {
    render(
      <ToastProvider>
        <ConfirmView
          work={work({ polish_used_count: 0, polish_remaining_count: 2 })}
          refresh={async () => {}}
          onBackToHome={() => {}}
        />
      </ToastProvider>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'AI 润色' }));
    fireEvent.change(screen.getByRole('textbox', { name: /想让 AI/ }), {
      target: { value: '   ' },
    });

    expect(screen.getByText('请先写下润色方向。')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '开始润色' })).toBeDisabled();
  });

  it('keeps quota conflict visible inside the edit modal', async () => {
    vi.spyOn(service, 'polishLyrics').mockRejectedValue(
      new ApiError(409, 'LYRICS_POLISH_QUOTA_EXHAUSTED', 'quota exhausted', 'req-1'),
    );

    render(
      <ToastProvider>
        <ConfirmView work={work()} refresh={async () => {}} onBackToHome={() => {}} />
      </ToastProvider>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'AI 润色' }));
    fireEvent.change(screen.getByRole('textbox', { name: /想让 AI/ }), {
      target: { value: '更热血一点' },
    });
    fireEvent.click(screen.getByRole('button', { name: '开始润色' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('改词次数已用完，本次未生效');
    expect(screen.getByRole('alert')).toHaveTextContent('请求编号：req-1');
  });

  it('waits for refreshed work detail before leaving the polish flow', async () => {
    vi.spyOn(service, 'polishLyrics').mockResolvedValue({
      work_id: 'work-1',
      status: 'LYRICS_READY',
      generation_stage: 'WAITING_CONFIRM',
      job_id: 'job-1',
      available_actions: ['CONFIRM_WORK'],
    });
    let resolveRefresh: () => void = () => {};
    const refresh = vi.fn(
      () =>
        new Promise<void>((resolve) => {
          resolveRefresh = resolve;
        }),
    );

    render(
      <ToastProvider>
        <ConfirmView
          work={work({ polish_used_count: 0, polish_remaining_count: 2 })}
          refresh={refresh}
          onBackToHome={() => {}}
        />
      </ToastProvider>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'AI 润色' }));
    fireEvent.change(screen.getByRole('textbox', { name: /想让 AI/ }), {
      target: { value: '更押韵一点' },
    });
    fireEvent.click(screen.getByRole('button', { name: '开始润色' }));

    await waitFor(() => expect(refresh).toHaveBeenCalled());
    expect(screen.getByRole('button', { name: '确认出歌' })).toBeDisabled();
    expect(screen.getByRole('dialog', { name: 'AI 润色歌词' })).toBeInTheDocument();

    resolveRefresh();

    await waitFor(() =>
      expect(screen.queryByRole('dialog', { name: 'AI 润色歌词' })).not.toBeInTheDocument(),
    );
    expect(screen.getByRole('button', { name: '确认出歌' })).not.toBeDisabled();
  });

  it('recovers a completed polish result after a public network disconnect', async () => {
    vi.spyOn(service, 'polishLyrics').mockRejectedValue(
      new ApiError(0, 'NETWORK_ERROR', '作曲服务连接中断，请稍后重试。'),
    );
    const updatedWork = work({ polish_used_count: 1, polish_remaining_count: 1 });
    updatedWork.lyrics_draft = {
      ...updatedWork.lyrics_draft!,
      lyrics_draft_id: 'draft-2',
      version_no: 4,
      lyrics_text: '[主歌]\n润色后的新歌词',
    };
    let resolveGetWork: () => void = () => {};
    vi.spyOn(service, 'getWork').mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveGetWork = () => resolve(updatedWork);
        }),
    );
    const refresh = vi.fn().mockResolvedValue(undefined);

    render(
      <ToastProvider>
        <ConfirmView
          work={work({ polish_used_count: 0, polish_remaining_count: 2 })}
          refresh={refresh}
          onBackToHome={() => {}}
        />
      </ToastProvider>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'AI 润色' }));
    fireEvent.change(screen.getByRole('textbox', { name: /想让 AI/ }), {
      target: { value: '更押韵一点' },
    });
    fireEvent.click(screen.getByRole('button', { name: '开始润色' }));

    expect(await screen.findByText('请求可能仍在后台处理中，正在为你确认最新歌词…')).toBeInTheDocument();
    await waitFor(() => expect(service.getWork).toHaveBeenCalledWith('work-1'));
    resolveGetWork();
    await waitFor(() => expect(refresh).toHaveBeenCalled());
    await waitFor(() =>
      expect(screen.queryByRole('dialog', { name: 'AI 润色歌词' })).not.toBeInTheDocument(),
    );
    expect(await screen.findByText('已获取润色后的歌词')).toBeInTheDocument();
  });

  it('refreshes and shows a friendly hint when confirming a stale lyrics draft', async () => {
    vi.spyOn(service, 'confirmWork').mockRejectedValue(
      new ApiError(409, 'CONFLICT', 'Lyrics draft is not the current confirmable draft', 'req-2'),
    );
    const refresh = vi.fn().mockResolvedValue(undefined);

    render(
      <ToastProvider>
        <ConfirmView work={work()} refresh={refresh} onBackToHome={() => {}} />
      </ToastProvider>,
    );

    fireEvent.click(screen.getByRole('button', { name: '确认出歌' }));

    expect(await screen.findByText(/歌词已更新，请重新确认出歌/)).toBeInTheDocument();
    expect(screen.getByText(/请求编号：req-2/)).toBeInTheDocument();
    await waitFor(() => expect(refresh).toHaveBeenCalled());
  });

  it('confirms work without forcing mock music provider in real mode', async () => {
    const confirmWork = vi.spyOn(service, 'confirmWork').mockResolvedValue({
      work_id: 'work-1',
      status: 'GENERATING',
      generation_stage: 'QUOTA_LOCKING',
      job_id: 'job-1',
      available_actions: [],
    });

    render(
      <ToastProvider>
        <ConfirmView work={work()} refresh={async () => {}} onBackToHome={() => {}} />
      </ToastProvider>,
    );

    fireEvent.click(screen.getByRole('button', { name: '确认出歌' }));

    await waitFor(() =>
      expect(confirmWork).toHaveBeenCalledWith(
        'work-1',
        expect.not.objectContaining({ music_provider: 'mock' }),
      ),
    );
  });
});
