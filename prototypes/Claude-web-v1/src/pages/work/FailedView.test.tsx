import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { WorkDetail } from '../../api/types';
import { ToastProvider } from '../../components/Toast';
import { service } from '../../mock/service';
import { FailedView } from './FailedView';

function failedWork(overrides: Partial<WorkDetail> = {}): WorkDetail {
  return {
    work_id: 'work-failed-1',
    work_code: 'YY-FAILED',
    creation_mode: 'LYRICS',
    status: 'FAILED',
    generation_stage: 'FAILED',
    package_status: 'PACKAGE_NOT_READY',
    song_title: '失败动作测试歌',
    song_summary: '用于验证失败页动作矩阵',
    lyrics_draft: null,
    media_assets: null,
    polish_used_count: 0,
    polish_remaining_count: 2,
    quota_hint: null,
    failure: {
      failure_code: 'COVER_GENERATION_FAILED',
      failure_message: 'cover failed',
      retryable: true,
      remaining_retry_count: null,
      recommended_action: 'RETRY_COVER',
    },
    available_actions: ['RETRY_COVER', 'RERENDER_VIDEO', 'RETURN_TO_EDIT', 'CONTACT_SUPPORT'],
    publish_handoff_hint: null,
    created_at: '2026-06-06T00:00:00Z',
    updated_at: '2026-06-06T00:00:00Z',
    generated_at: null,
    ...overrides,
  };
}

describe('FailedView action matrix', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders only backend-provided secondary failure actions', () => {
    render(
      <ToastProvider>
        <FailedView work={failedWork()} refresh={async () => {}} onBackToHome={() => {}} />
      </ToastProvider>,
    );

    expect(screen.getByRole('button', { name: '重新生成封面' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '重新渲染画面' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '返回编辑' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '联系平台协助' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '重新生成' })).not.toBeInTheDocument();
  });

  it('calls the matching service for retry-cover and rerender-video actions', async () => {
    const regenerateCover = vi.spyOn(service, 'regenerateCover').mockResolvedValue({
      work_id: 'work-failed-1',
      status: 'FAILED',
      generation_stage: 'FAILED',
      job_id: 'job-cover',
      available_actions: ['RETRY_COVER', 'RETURN_TO_EDIT'],
    });
    const rerenderVideo = vi.spyOn(service, 'rerenderVideo').mockResolvedValue({
      work_id: 'work-failed-1',
      status: 'FAILED',
      generation_stage: 'FAILED',
      job_id: 'job-video',
      available_actions: ['RERENDER_VIDEO', 'RETURN_TO_EDIT'],
    });

    render(
      <ToastProvider>
        <FailedView work={failedWork()} refresh={async () => {}} onBackToHome={() => {}} />
      </ToastProvider>,
    );

    fireEvent.click(screen.getByRole('button', { name: '重新生成封面' }));
    await waitFor(() => expect(regenerateCover).toHaveBeenCalledWith('work-failed-1'));

    fireEvent.click(screen.getByRole('button', { name: '重新渲染画面' }));
    await waitFor(() => expect(rerenderVideo).toHaveBeenCalledWith('work-failed-1'));
  });
});
