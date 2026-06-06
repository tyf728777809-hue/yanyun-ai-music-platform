import { describe, expect, it } from 'vitest';
import {
  deriveViewPhase,
  hasAction,
  progressStepStates,
  PRODUCTION_PROGRESS_STEPS,
} from '../api/workState';
import type { WorkDetail } from '../api/types';

function baseWork(overrides: Partial<WorkDetail>): WorkDetail {
  return {
    work_id: 'w1',
    work_code: 'YY-TEST',
    creation_mode: 'INSPIRATION',
    status: 'DRAFT',
    generation_stage: 'NONE',
    package_status: 'PACKAGE_NOT_READY',
    polish_used_count: 0,
    polish_remaining_count: 2,
    available_actions: [],
    created_at: '2026-06-06T00:00:00Z',
    updated_at: '2026-06-06T00:00:00Z',
    ...overrides,
  };
}

describe('deriveViewPhase', () => {
  it('routes lyrics generation states to LYRICS_GENERATING', () => {
    expect(
      deriveViewPhase(baseWork({ status: 'LYRICS_GENERATING', generation_stage: 'LYRICS_GENERATING' })),
    ).toBe('LYRICS_GENERATING');
    expect(
      deriveViewPhase(baseWork({ status: 'DRAFT', generation_stage: 'USER_INPUT_PRECHECK' })),
    ).toBe('LYRICS_GENERATING');
  });

  it('routes lyrics-ready / waiting-confirm to CONFIRM', () => {
    expect(
      deriveViewPhase(baseWork({ status: 'LYRICS_READY', generation_stage: 'WAITING_CONFIRM' })),
    ).toBe('CONFIRM');
  });

  it('routes production stages to GENERATING', () => {
    for (const stage of ['QUOTA_LOCKING', 'MUSIC_GENERATING', 'COVER_GENERATING', 'VIDEO_RENDERING', 'PACKAGE_BUILDING'] as const) {
      expect(deriveViewPhase(baseWork({ status: 'GENERATING', generation_stage: stage }))).toBe(
        'GENERATING',
      );
    }
  });

  it('routes generated + package ready to FINISHED', () => {
    expect(
      deriveViewPhase(
        baseWork({ status: 'GENERATED', generation_stage: 'PACKAGE_READY', package_status: 'PACKAGE_READY' }),
      ),
    ).toBe('FINISHED');
    expect(
      deriveViewPhase(
        baseWork({ status: 'GENERATED', generation_stage: 'PACKAGE_READY', package_status: 'PACKAGE_FETCHED' }),
      ),
    ).toBe('FINISHED');
  });

  it('treats any failure state or failure object as FAILED, even mid-production', () => {
    expect(deriveViewPhase(baseWork({ status: 'FAILED', generation_stage: 'FAILED' }))).toBe('FAILED');
    expect(deriveViewPhase(baseWork({ status: 'LYRICS_FAILED' }))).toBe('FAILED');
    expect(
      deriveViewPhase(
        baseWork({
          status: 'GENERATING',
          generation_stage: 'MUSIC_GENERATING',
          failure: {
            failure_code: 'MUSIC_GENERATION_FAILED',
            failure_message: 'x',
            retryable: true,
          },
        }),
      ),
    ).toBe('FAILED');
  });
});

describe('hasAction', () => {
  it('reflects the backend available_actions list only', () => {
    const w = baseWork({ available_actions: ['CONFIRM_WORK', 'POLISH_LYRICS'] });
    expect(hasAction(w, 'CONFIRM_WORK')).toBe(true);
    expect(hasAction(w, 'POLISH_LYRICS')).toBe(true);
    expect(hasAction(w, 'RETRY_MUSIC')).toBe(false);
  });
});

describe('progressStepStates', () => {
  it('marks earlier steps done, current active, later pending', () => {
    const states = progressStepStates('COVER_GENERATING');
    const coverIdx = PRODUCTION_PROGRESS_STEPS.findIndex((s) => s.key === 'cover');
    expect(states[coverIdx]).toBe('active');
    expect(states[coverIdx - 1]).toBe('done');
    expect(states[coverIdx + 1]).toBe('pending');
  });

  it('groups timeline + video render under one step', () => {
    expect(progressStepStates('TIMELINE_BUILDING')).toEqual(progressStepStates('VIDEO_RENDERING'));
  });
});
