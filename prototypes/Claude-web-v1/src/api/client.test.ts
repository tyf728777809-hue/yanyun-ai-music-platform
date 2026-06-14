import { beforeEach, describe, expect, it } from 'vitest';
import { getMockUserId, MOCK_USER_ID_STORAGE_KEY } from './client';

describe('getMockUserId', () => {
  beforeEach(() => {
    const store = new Map<string, string>();
    Object.defineProperty(window, 'localStorage', {
      configurable: true,
      value: {
        clear: () => store.clear(),
        getItem: (key: string) => store.get(key) ?? null,
        removeItem: (key: string) => store.delete(key),
        setItem: (key: string, value: string) => store.set(key, value),
      },
    });
  });

  it('keeps the backend default mock user id for local real-backend smoke', () => {
    window.localStorage.setItem(MOCK_USER_ID_STORAGE_KEY, 'mock_user_001');

    expect(getMockUserId()).toBe('mock_user_001');
  });
});
