import { useCallback, useEffect, useRef, useState } from 'react';
import { service } from '../mock/service';
import { ApiError } from '../api/client';
import type { WorkDetail } from '../api/types';
import { deriveViewPhase, type WorkViewPhase } from '../api/workState';

interface PollState {
  work: WorkDetail | null;
  phase: WorkViewPhase | null;
  loading: boolean; // 首屏加载
  error: ApiError | null;
  refresh: () => Promise<void>;
}

// 这些阶段需要轮询：歌词生成中、出歌生成中。其余阶段保持静止。
const POLLING_PHASES = new Set<WorkViewPhase>(['LYRICS_GENERATING', 'GENERATING']);
const POLL_INTERVAL = 1500;

// 拉取并按需轮询作品详情。组件只读结果，不自行推断状态机。
export function useWorkDetail(workId: string | null): PollState {
  const [work, setWork] = useState<WorkDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<ApiError | null>(null);
  const timer = useRef<number | null>(null);
  const abort = useRef<AbortController | null>(null);

  const fetchOnce = useCallback(async () => {
    if (!workId) return;
    abort.current?.abort();
    const controller = new AbortController();
    abort.current = controller;
    try {
      const detail = await service.getWork(workId, controller.signal);
      setWork(detail);
      setError(null);
    } catch (err) {
      if (err instanceof DOMException && err.name === 'AbortError') return;
      if (err instanceof ApiError) setError(err);
      else setError(new ApiError(0, 'UNKNOWN', '加载作品失败'));
    } finally {
      setLoading(false);
    }
  }, [workId]);

  useEffect(() => {
    setLoading(true);
    setWork(null);
    setError(null);
    void fetchOnce();
    return () => {
      abort.current?.abort();
      if (timer.current) window.clearTimeout(timer.current);
    };
  }, [fetchOnce]);

  // 根据当前阶段决定是否继续轮询。
  useEffect(() => {
    if (!work) return;
    const phase = deriveViewPhase(work);
    if (!POLLING_PHASES.has(phase)) return;
    timer.current = window.setTimeout(() => {
      void fetchOnce();
    }, POLL_INTERVAL);
    return () => {
      if (timer.current) window.clearTimeout(timer.current);
    };
  }, [work, fetchOnce]);

  return {
    work,
    phase: work ? deriveViewPhase(work) : null,
    loading,
    error,
    refresh: fetchOnce,
  };
}
