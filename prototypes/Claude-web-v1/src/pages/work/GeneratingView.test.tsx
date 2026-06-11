import { act, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { WorkDetail } from '../../api/types';
import { GeneratingView } from './GeneratingView';

function generatingWork(overrides: Partial<WorkDetail> = {}): WorkDetail {
  return {
    work_id: 'work-generating-1',
    work_code: 'YY-GEN',
    creation_mode: 'INSPIRATION',
    status: 'GENERATING',
    generation_stage: 'MUSIC_GENERATING',
    package_status: 'PACKAGE_NOT_READY',
    song_title: '雁门新声',
    song_summary: '用于验证生成中页',
    lyrics_draft: null,
    media_assets: null,
    polish_used_count: 0,
    polish_remaining_count: 2,
    quota_hint: null,
    failure: null,
    available_actions: [],
    publish_handoff_hint: null,
    created_at: '2026-06-06T00:00:00Z',
    updated_at: '2026-06-06T00:00:00Z',
    generated_at: null,
    ...overrides,
  };
}

describe('GeneratingView', () => {
  it('shows honest stage timing guidance and elapsed time', () => {
    vi.useFakeTimers();
    try {
      render(
        <GeneratingView
          work={generatingWork()}
          refresh={async () => {}}
          onBackToHome={() => {}}
        />,
      );

      expect(screen.getByText('正在谱写旋律，真实模型通常需要 2-5 分钟。')).toBeInTheDocument();
      expect(screen.getByText('已等待 0 秒')).toBeInTheDocument();

      act(() => {
        vi.advanceTimersByTime(3000);
      });
      expect(screen.getByText(/已等待 3 秒/)).toBeInTheDocument();
    } finally {
      vi.useRealTimers();
    }
  });
});
