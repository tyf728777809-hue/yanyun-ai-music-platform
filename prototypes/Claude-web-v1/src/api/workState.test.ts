import { describe, expect, it } from 'vitest';
import type { WorkDetail } from './types';
import { deriveViewPhase, progressStepStates } from './workState';

function work(overrides: Partial<WorkDetail> = {}): WorkDetail {
  return {
    work_id: 'work-1',
    work_code: 'YY-0001',
    creation_mode: 'INSPIRATION',
    status: 'LYRICS_READY',
    generation_stage: 'WAITING_CONFIRM',
    package_status: 'PACKAGE_NOT_READY',
    song_title: '燕云未名曲',
    song_summary: 'summary',
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

describe('deriveViewPhase', () => {
  it('routes lyric generation statuses to the lyric loading view', () => {
    expect(
      deriveViewPhase(work({ status: 'LYRICS_GENERATING', generation_stage: 'LYRICS_GENERATING' })),
    ).toBe('LYRICS_GENERATING');
  });

  it('routes waiting-confirm work to the confirmation detail view', () => {
    expect(deriveViewPhase(work())).toBe('CONFIRM');
  });

  it('routes generation stages to the production progress view', () => {
    expect(
      deriveViewPhase(work({ status: 'GENERATING', generation_stage: 'VIDEO_RENDERING' })),
    ).toBe('GENERATING');
  });

  it('routes generated package-ready work to the finished view', () => {
    expect(
      deriveViewPhase(
        work({
          status: 'GENERATED',
          generation_stage: 'PACKAGE_READY',
          package_status: 'PACKAGE_READY',
        }),
      ),
    ).toBe('FINISHED');
  });

  it('routes package-blocked generated work to the finished handoff view', () => {
    expect(
      deriveViewPhase(
        work({
          status: 'GENERATED',
          generation_stage: 'PACKAGE_READY',
          package_status: 'PACKAGE_BLOCKED',
        }),
      ),
    ).toBe('FINISHED');
  });

  it('routes any work with a failure object to the failed view first', () => {
    expect(
      deriveViewPhase(
        work({
          status: 'GENERATED',
          generation_stage: 'PACKAGE_READY',
          package_status: 'PACKAGE_READY',
          failure: {
            failure_code: 'MUSIC_GENERATION_FAILED',
            failure_message: 'failed',
            retryable: true,
            recommended_action: 'RETRY_MUSIC',
          },
        }),
      ),
    ).toBe('FAILED');
  });
});

describe('progressStepStates', () => {
  it('marks prior generation steps done and current step active', () => {
    expect(progressStepStates('VIDEO_RENDERING')).toEqual([
      'done',
      'done',
      'done',
      'active',
      'pending',
    ]);
  });
});
