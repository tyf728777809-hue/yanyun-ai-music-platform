import { fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ToastProvider } from '../components/Toast';
import { mockBackend } from '../mock/mockBackend';
import { service, setRunMode } from '../mock/service';
import { WorksPage } from './WorksPage';

describe('WorksPage', () => {
  beforeEach(() => {
    setRunMode('demo');
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders works returned by listWorks', async () => {
    await mockBackend.createFromLyrics({
      lyrics_input: '一段列表测试歌词',
      song_title: '列表里的歌',
    });

    render(
      <ToastProvider>
        <WorksPage onOpenWork={() => {}} />
      </ToastProvider>,
    );

    expect(await screen.findByText('列表里的歌')).toBeInTheDocument();
    expect(screen.getByText('创作记录')).toBeInTheDocument();
  });

  it('loads more pages using total_pages from the backend contract', async () => {
    const listWorks = vi.spyOn(service, 'listWorks');
    listWorks
      .mockResolvedValueOnce({
        items: [
          {
            work_id: 'work-1',
            work_code: 'YY-0001',
            song_title: '第一页作品',
            status: 'LYRICS_READY',
            generation_stage: 'WAITING_CONFIRM',
            package_status: 'PACKAGE_NOT_READY',
            cover_url: null,
            video_preview_url: null,
            updated_at: '2026-06-06T00:00:00Z',
          },
        ],
        pagination: { page: 1, page_size: 20, total_items: 21, total_pages: 2 },
      })
      .mockResolvedValueOnce({
        items: [
          {
            work_id: 'work-2',
            work_code: 'YY-0002',
            song_title: '第二页作品',
            status: 'GENERATED',
            generation_stage: 'PACKAGE_READY',
            package_status: 'PACKAGE_READY',
            cover_url: null,
            video_preview_url: null,
            updated_at: '2026-06-06T00:00:00Z',
          },
        ],
        pagination: { page: 2, page_size: 20, total_items: 21, total_pages: 2 },
      });

    render(
      <ToastProvider>
        <WorksPage onOpenWork={() => {}} />
      </ToastProvider>,
    );

    expect(await screen.findByText('第一页作品')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '继续加载' }));

    expect(await screen.findByText('第二页作品')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '继续加载' })).not.toBeInTheDocument();
    expect(listWorks).toHaveBeenLastCalledWith({ page: 2, page_size: 20 });
  });
});
