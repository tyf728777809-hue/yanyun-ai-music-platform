import * as realApi from '../api/works';
import { mockBackend } from './mockBackend';

// 运行模式：real = 调真实本地后端；demo = 内置内存模拟（不联网、不调真实模型）。
export type RunMode = 'real' | 'demo';

const STORAGE_KEY = 'yanyun-proto-run-mode';

function readInitialMode(): RunMode {
  // URL ?mock=1 强制演示模式，便于直接分享演示链接。
  if (typeof window !== 'undefined') {
    const params = new URLSearchParams(window.location.search);
    if (params.get('mock') === '1' || params.get('demo') === '1') return 'demo';
    if (import.meta.env.MODE === 'test') return 'real';
    try {
      const saved = window.localStorage?.getItem(STORAGE_KEY);
      if (saved === 'real' || saved === 'demo') return saved;
    } catch {
      // localStorage may be unavailable in tests or restricted browser contexts.
    }
  }
  return 'real';
}

let currentMode: RunMode = readInitialMode();

export function getRunMode(): RunMode {
  return currentMode;
}

export function setRunMode(mode: RunMode): void {
  currentMode = mode;
  if (typeof window !== 'undefined' && import.meta.env.MODE !== 'test') {
    try {
      window.localStorage?.setItem(STORAGE_KEY, mode);
    } catch {
      // Ignore persistence failure; in-memory mode still updates.
    }
  }
}

// 统一服务层：组件只调用这里，由运行模式决定走真实后端还是演示后端。
export const service = {
  createFromInspiration: (...args: Parameters<typeof realApi.createFromInspiration>) =>
    currentMode === 'demo'
      ? mockBackend.createFromInspiration(args[0])
      : realApi.createFromInspiration(...args),

  createFromLyrics: (...args: Parameters<typeof realApi.createFromLyrics>) =>
    currentMode === 'demo'
      ? mockBackend.createFromLyrics(args[0])
      : realApi.createFromLyrics(...args),

  listWorks: (...args: Parameters<typeof realApi.listWorks>) =>
    currentMode === 'demo'
      ? mockBackend.listWorks(args[0])
      : realApi.listWorks(...args),

  getWork: (...args: Parameters<typeof realApi.getWork>) =>
    currentMode === 'demo' ? mockBackend.getWork(args[0]) : realApi.getWork(...args),

  polishLyrics: (...args: Parameters<typeof realApi.polishLyrics>) =>
    currentMode === 'demo'
      ? mockBackend.polishLyrics(args[0], args[1])
      : realApi.polishLyrics(...args),

  continueLyrics: (...args: Parameters<typeof realApi.continueLyrics>) =>
    currentMode === 'demo'
      ? mockBackend.continueLyrics(args[0], args[1])
      : realApi.continueLyrics(...args),

  confirmWork: (...args: Parameters<typeof realApi.confirmWork>) =>
    currentMode === 'demo'
      ? mockBackend.confirmWork(args[0], args[1])
      : realApi.confirmWork(...args),

  retryMusic: (...args: Parameters<typeof realApi.retryMusic>) =>
    currentMode === 'demo' ? mockBackend.retryMusic(args[0], args[1]) : realApi.retryMusic(...args),

  regenerateCover: (...args: Parameters<typeof realApi.regenerateCover>) =>
    currentMode === 'demo' ? mockBackend.regenerateCover(args[0]) : realApi.regenerateCover(...args),

  rerenderVideo: (...args: Parameters<typeof realApi.rerenderVideo>) =>
    currentMode === 'demo' ? mockBackend.rerenderVideo(args[0]) : realApi.rerenderVideo(...args),

  getPublishPackage: (...args: Parameters<typeof realApi.getPublishPackage>) =>
    currentMode === 'demo'
      ? mockBackend.getPublishPackage(args[0])
      : realApi.getPublishPackage(...args),

  markPublishPackageFetched: (...args: Parameters<typeof realApi.markPublishPackageFetched>) =>
    currentMode === 'demo'
      ? mockBackend.markPublishPackageFetched(args[0])
      : realApi.markPublishPackageFetched(...args),

  refreshPublishPackageUrl: (...args: Parameters<typeof realApi.refreshPublishPackageUrl>) =>
    currentMode === 'demo'
      ? mockBackend.refreshPublishPackageUrl(args[0])
      : realApi.refreshPublishPackageUrl(...args),
};
