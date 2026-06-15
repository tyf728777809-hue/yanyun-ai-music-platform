import { useState } from 'react';
import { ApiError } from '../api/client';
import { requestIdLine, userFriendlyErrorMessage } from '../api/friendlyError';
import { useToast } from '../components/Toast';

// 统一执行带副作用的动作（POST），管理单一 in-flight key 的 loading，
// 并把 ApiError 翻译成友好提示。返回 run 函数与当前忙碌的 key。
export function useAction(onSettled?: () => void | Promise<void>) {
  const toast = useToast();
  const [busyKey, setBusyKey] = useState<string | null>(null);

  async function run<T>(
    key: string,
    fn: () => Promise<T>,
    opts?: {
      successMsg?: string;
      conflictMsg?: string;
      onSuccess?: (r: T) => void | Promise<void>;
      onError?: (message: string) => void | Promise<void>;
      suppressErrorToast?: boolean;
    },
  ): Promise<void> {
    if (busyKey) return;
    setBusyKey(key);
    try {
      const result = await fn();
      if (opts?.successMsg) toast.success(opts.successMsg);
      await opts?.onSuccess?.(result);
    } catch (err) {
      let message = '操作失败，请稍后重试';
      if (err instanceof ApiError) {
        const suffix = requestIdLine(err);
        if (err.isQuotaConflict && opts?.conflictMsg) {
          message = suffix ? `${opts.conflictMsg} ${suffix}` : opts.conflictMsg;
        } else {
          const friendly = userFriendlyErrorMessage(err);
          message = suffix ? `${friendly} ${suffix}` : friendly;
        }
      }
      if (!opts?.suppressErrorToast) {
        toast.error(message);
      }
      await opts?.onError?.(message);
    } finally {
      setBusyKey(null);
      await onSettled?.();
    }
  }

  return { run, busyKey, isBusy: busyKey !== null };
}
