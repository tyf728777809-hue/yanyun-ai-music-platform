import { useState } from 'react';
import { ApiError } from '../api/client';
import { requestIdLine } from '../api/friendlyError';
import { useToast } from '../components/Toast';

// 统一执行带副作用的动作（POST），管理单一 in-flight key 的 loading，
// 并把 ApiError 翻译成友好提示。返回 run 函数与当前忙碌的 key。
export function useAction(onSettled?: () => void) {
  const toast = useToast();
  const [busyKey, setBusyKey] = useState<string | null>(null);

  async function run<T>(
    key: string,
    fn: () => Promise<T>,
    opts?: {
      successMsg?: string;
      conflictMsg?: string;
      onSuccess?: (r: T) => void;
      onError?: (message: string) => void;
    },
  ): Promise<void> {
    if (busyKey) return;
    setBusyKey(key);
    try {
      const result = await fn();
      if (opts?.successMsg) toast.success(opts.successMsg);
      opts?.onSuccess?.(result);
    } catch (err) {
      let message = '操作失败，请稍后重试';
      if (err instanceof ApiError) {
        const suffix = requestIdLine(err);
        if (err.isQuotaConflict && opts?.conflictMsg) {
          message = suffix ? `${opts.conflictMsg} ${suffix}` : opts.conflictMsg;
        } else {
          message = suffix ? `${err.message} ${suffix}` : err.message;
        }
      }
      toast.error(message);
      opts?.onError?.(message);
    } finally {
      setBusyKey(null);
      onSettled?.();
    }
  }

  return { run, busyKey, isBusy: busyKey !== null };
}
