import { describe, expect, it } from 'vitest';
import { ApiError } from './client';
import { userFriendlyErrorMessage } from './friendlyError';

describe('userFriendlyErrorMessage', () => {
  it('turns a 409 polish limit response into a human-friendly copy', () => {
    const message = userFriendlyErrorMessage(
      new ApiError(409, 'CONFLICT', 'No remaining polish attempts'),
    );

    expect(message).toContain('改词次数已用完');
    expect(message).not.toContain('CONFLICT');
    expect(message).not.toContain('No remaining');
  });

  it('keeps network guidance actionable for local development', () => {
    const message = userFriendlyErrorMessage(
      new ApiError(0, 'NETWORK_ERROR', '连不上作曲服务，请确认本地后端已启动。'),
    );

    expect(message).toContain('作曲服务');
    expect(message).toContain('演示模式');
  });
});
