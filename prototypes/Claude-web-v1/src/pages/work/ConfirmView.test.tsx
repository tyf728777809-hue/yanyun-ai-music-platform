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

  it('requires an instruction before submitting polish', () => {
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
