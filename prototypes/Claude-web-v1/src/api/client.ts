import type { ApiErrorBody } from './types';

// 本地请求头约定（见 docs/runbook/local-development.md）。
export const API_BASE = '/api/v1';
export const MOCK_USER_ID = 'mock_user_001';

// 把后端错误信封翻译成结构化错误，UI 据此给普通用户展示友好文案。
export class ApiError extends Error {
  readonly httpStatus: number;
  readonly code: string;
  readonly requestId?: string;

  constructor(httpStatus: number, code: string, message: string, requestId?: string) {
    super(message);
    this.name = 'ApiError';
    this.httpStatus = httpStatus;
    this.code = code;
    this.requestId = requestId;
  }

  /** 改词/续写次数耗尽：后端返回 409 CONFLICT。 */
  get isQuotaConflict(): boolean {
    return this.httpStatus === 409;
  }
}

// 每个 POST 必须携带唯一幂等键：web-<时间戳>-<uuid>。
export function newIdempotencyKey(): string {
  const uuid =
    typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? crypto.randomUUID()
      : Math.random().toString(36).slice(2) + Date.now().toString(36);
  return `web-${Date.now()}-${uuid}`;
}

interface RequestOptions {
  method?: 'GET' | 'POST';
  body?: unknown;
  /** POST 默认自动生成幂等键；可显式传入用于重放同一次操作。 */
  idempotencyKey?: string;
  signal?: AbortSignal;
}

async function parseError(response: Response): Promise<ApiError> {
  let code = `HTTP_${response.status}`;
  let message = response.statusText || '请求失败';
  let requestId: string | undefined;
  try {
    const data = (await response.json()) as ApiErrorBody;
    if (data?.error) {
      code = data.error.code ?? code;
      message = data.error.message ?? message;
      requestId = data.error.request_id;
    }
  } catch {
    // 非 JSON 错误体时退回到状态文案。
  }
  return new ApiError(response.status, code, message, requestId);
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, idempotencyKey, signal } = options;
  const headers: Record<string, string> = {
    'X-Mock-User-Id': MOCK_USER_ID,
    Accept: 'application/json',
  };
  if (method === 'POST') {
    headers['Content-Type'] = 'application/json';
    headers['Idempotency-Key'] = idempotencyKey ?? newIdempotencyKey();
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (err) {
    if (err instanceof DOMException && err.name === 'AbortError') {
      throw err;
    }
    throw new ApiError(
      0,
      'NETWORK_ERROR',
      '连不上作曲服务，请确认本地后端已启动（http://localhost:8080）。',
    );
  }

  if (!response.ok) {
    throw await parseError(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
