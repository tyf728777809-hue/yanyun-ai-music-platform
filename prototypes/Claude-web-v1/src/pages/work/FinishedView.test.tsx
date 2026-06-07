import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ToastProvider } from '../../components/Toast';
import { service } from '../../mock/service';
import type { WorkDetail } from '../../api/types';
import { FinishedView } from './FinishedView';

function work(overrides: Partial<WorkDetail> = {}): WorkDetail {
  return {
    work_id: 'work-1',
    work_code: 'YY-0001',
    creation_mode: 'LYRICS',
    status: 'GENERATED',
    generation_stage: 'PACKAGE_READY',
    package_status: 'PACKAGE_READY',
    song_title: '成品测试歌',
    song_summary: '一首用于测试交接信息的歌',
    lyrics_draft: null,
    media_assets: {
      audio_url: 'https://cdn.local/audio.mp3',
      cover_url: 'https://cdn.local/cover.png',
      video_url: 'https://cdn.local/video.mp4',
      video_duration_ms: 1000,
      video_file_size_bytes: 1000,
    },
    polish_used_count: 0,
    polish_remaining_count: 2,
    quota_hint: null,
    failure: null,
    available_actions: ['REFRESH_PACKAGE_URL', 'MARK_PACKAGE_FETCHED'],
    publish_handoff_hint: {
      ready_for_handoff: true,
      message: '作品已准备好，可交给社区发布。',
    },
    created_at: '2026-06-06T00:00:00Z',
    updated_at: '2026-06-06T00:00:00Z',
    generated_at: '2026-06-06T00:00:00Z',
    ...overrides,
  };
}

describe('FinishedView', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders user-facing handoff links and lyrics from publish package', async () => {
    vi.spyOn(service, 'getPublishPackage').mockResolvedValue({
      work_id: 'work-1',
      package_status: 'PACKAGE_READY',
      package_url: 'https://cdn.local/package.zip',
      package_url_expires_at: '2026-06-07T00:00:00Z',
      package_json: {
        work_id: 'work-1',
        audio: { url: 'https://cdn.local/package-audio.mp3', mime_type: 'audio/mpeg' },
        video: { url: 'https://cdn.local/package-video.mp4', mime_type: 'video/mp4' },
        cover: { url: 'https://cdn.local/package-cover.png', mime_type: 'image/png' },
        lyrics: { text: '第一句\n第二句', timeline_url: 'https://cdn.local/timeline.json' },
        metadata: { song_title: '成品测试歌' },
      },
      available_actions: ['REFRESH_PACKAGE_URL', 'MARK_PACKAGE_FETCHED'],
    });

    render(
      <ToastProvider>
        <FinishedView work={work()} refresh={async () => {}} onBackToHome={() => {}} />
      </ToastProvider>,
    );

    expect(await screen.findByText('交接下载链接')).toBeInTheDocument();
    expect(screen.getByText('音频地址')).toBeInTheDocument();
    expect(screen.getByText('视频地址')).toBeInTheDocument();
    expect(screen.getByText('封面地址')).toBeInTheDocument();
    expect(screen.getByText((_, element) => element?.textContent === '第一句\n第二句')).toBeInTheDocument();
  });

  it('hides mark fetched after package handoff is marked fetched locally', async () => {
    vi.spyOn(service, 'getPublishPackage').mockResolvedValue({
      work_id: 'work-1',
      package_status: 'PACKAGE_READY',
      package_url: 'https://cdn.local/package.zip',
      package_url_expires_at: '2026-06-07T00:00:00Z',
      package_json: {
        work_id: 'work-1',
        audio: { url: 'https://cdn.local/package-audio.mp3', mime_type: 'audio/mpeg' },
        video: { url: 'https://cdn.local/package-video.mp4', mime_type: 'video/mp4' },
        cover: { url: 'https://cdn.local/package-cover.png', mime_type: 'image/png' },
        lyrics: { text: '第一句', timeline_url: 'https://cdn.local/timeline.json' },
        metadata: { song_title: '成品测试歌' },
      },
      available_actions: ['REFRESH_PACKAGE_URL', 'MARK_PACKAGE_FETCHED'],
    });
    vi.spyOn(service, 'markPublishPackageFetched').mockResolvedValue({
      work_id: 'work-1',
      package_status: 'PACKAGE_FETCHED',
      package_url: 'https://cdn.local/package.zip',
      package_url_expires_at: '2026-06-07T00:00:00Z',
      package_json: null,
      available_actions: ['REFRESH_PACKAGE_URL'],
    });

    render(
      <ToastProvider>
        <FinishedView work={work()} refresh={async () => {}} onBackToHome={() => {}} />
      </ToastProvider>,
    );

    fireEvent.click(await screen.findByRole('button', { name: '标记已交接' }));

    expect(await screen.findByText('作品已交接给社区发布流程。')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '标记已交接' })).not.toBeInTheDocument();
  });

  it('renders package-blocked actions from available_actions and keeps handoff disabled', async () => {
    vi.spyOn(service, 'getPublishPackage').mockResolvedValue({
      work_id: 'work-1',
      package_status: 'PACKAGE_BLOCKED',
      package_url: null,
      package_url_expires_at: null,
      package_json: null,
      available_actions: ['RERENDER_VIDEO', 'CONTACT_SUPPORT'],
      blocked_reason: '作品暂不能交给社区发布。',
    });
    const rerenderVideo = vi.spyOn(service, 'rerenderVideo').mockResolvedValue({
      work_id: 'work-1',
      status: 'GENERATED',
      generation_stage: 'PACKAGE_READY',
      job_id: 'job-video',
      available_actions: ['RERENDER_VIDEO', 'CONTACT_SUPPORT'],
    });

    render(
      <ToastProvider>
        <FinishedView
          work={work({
            package_status: 'PACKAGE_BLOCKED',
            available_actions: ['RERENDER_VIDEO', 'CONTACT_SUPPORT'],
          })}
          refresh={async () => {}}
          onBackToHome={() => {}}
        />
      </ToastProvider>,
    );

    expect(await screen.findByText('作品暂不能交给社区发布。')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '标记已交接' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '刷新下载链接' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: '重新渲染画面' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '联系平台协助' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '重新渲染画面' }));
    await waitFor(() => expect(rerenderVideo).toHaveBeenCalledWith('work-1'));
  });
});
