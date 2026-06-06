import { ApiError } from './client';

export function userFriendlyErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.isQuotaConflict) {
      return '改词次数已用完。你仍可以确认当前歌词出歌，或返回编辑重新开始。';
    }
    if (error.code === 'NETWORK_ERROR') {
      return `${error.message} 如果只是想体验流程，可以打开右上角「演示模式」。`;
    }
    return error.message || '请求失败，请稍后再试。';
  }

  if (error instanceof Error) {
    return error.message;
  }

  return '请求失败，请稍后再试。';
}

export function requestIdLine(error: unknown): string | null {
  if (error instanceof ApiError && error.requestId) {
    return `请求编号：${error.requestId}`;
  }
  return null;
}

export function userFriendlyErrorWithRequestId(error: unknown): string {
  const message = userFriendlyErrorMessage(error);
  const requestId = requestIdLine(error);
  return requestId ? `${message} ${requestId}` : message;
}
