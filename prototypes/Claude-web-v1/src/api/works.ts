import { apiRequest } from './client';
import type {
  ConfirmWorkRequest,
  CreateWorkResponse,
  InspirationCreateRequest,
  JobAcceptedResponse,
  LyricsContinueRequest,
  LyricsCreateRequest,
  LyricsPolishRequest,
  PublishPackage,
  RetryMusicRequest,
  WorkDetail,
  WorkListResponse,
  WorkStatus,
} from './types';

// 与后端 works 接口一一对应的薄封装。所有 POST 由 client 自动注入幂等键。

export function createFromInspiration(
  body: InspirationCreateRequest,
  idempotencyKey?: string,
): Promise<CreateWorkResponse> {
  return apiRequest('/works/inspiration', { method: 'POST', body, idempotencyKey });
}

export function createFromLyrics(
  body: LyricsCreateRequest,
  idempotencyKey?: string,
): Promise<CreateWorkResponse> {
  return apiRequest('/works/lyrics', { method: 'POST', body, idempotencyKey });
}

export function listWorks(
  params: { page?: number; page_size?: number; status?: WorkStatus } = {},
  signal?: AbortSignal,
): Promise<WorkListResponse> {
  const search = new URLSearchParams();
  if (params.page != null) search.set('page', String(params.page));
  if (params.page_size != null) search.set('page_size', String(params.page_size));
  if (params.status) search.set('status', params.status);
  const suffix = search.toString() ? `?${search.toString()}` : '';
  return apiRequest(`/works${suffix}`, { signal });
}

export function getWork(workId: string, signal?: AbortSignal): Promise<WorkDetail> {
  return apiRequest(`/works/${workId}`, { signal });
}

export function polishLyrics(
  workId: string,
  body: LyricsPolishRequest,
): Promise<JobAcceptedResponse> {
  return apiRequest(`/works/${workId}/lyrics/polish`, { method: 'POST', body });
}

export function continueLyrics(
  workId: string,
  body: LyricsContinueRequest,
): Promise<JobAcceptedResponse> {
  return apiRequest(`/works/${workId}/lyrics/continue`, { method: 'POST', body });
}

export function confirmWork(
  workId: string,
  body: ConfirmWorkRequest,
): Promise<JobAcceptedResponse> {
  return apiRequest(`/works/${workId}/confirm`, { method: 'POST', body });
}

export function retryMusic(
  workId: string,
  body: RetryMusicRequest,
): Promise<JobAcceptedResponse> {
  return apiRequest(`/works/${workId}/music/retry`, { method: 'POST', body });
}

export function regenerateCover(workId: string): Promise<JobAcceptedResponse> {
  return apiRequest(`/works/${workId}/cover/regenerate`, { method: 'POST' });
}

export function rerenderVideo(workId: string): Promise<JobAcceptedResponse> {
  return apiRequest(`/works/${workId}/video/rerender`, { method: 'POST' });
}

export function getPublishPackage(workId: string, signal?: AbortSignal): Promise<PublishPackage> {
  return apiRequest(`/works/${workId}/publish-package`, { signal });
}

export function markPublishPackageFetched(workId: string): Promise<PublishPackage> {
  return apiRequest(`/works/${workId}/publish-package/mark-fetched`, { method: 'POST' });
}

export function refreshPublishPackageUrl(workId: string): Promise<PublishPackage> {
  return apiRequest(`/works/${workId}/publish-package/refresh-url`, { method: 'POST' });
}
