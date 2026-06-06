import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ActionButtons } from './ActionButtons';

describe('ActionButtons', () => {
  it('renders only actions provided by the backend available_actions list', () => {
    render(
      <ActionButtons
        actions={['POLISH_LYRICS', 'CONFIRM_WORK']}
        loadingAction={null}
        onAction={vi.fn()}
      />,
    );

    expect(screen.getByRole('button', { name: 'AI 润色' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '确认出歌' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'AI 续写' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '重新生成' })).not.toBeInTheDocument();
  });

  it('disables all actions while one action is loading', () => {
    render(
      <ActionButtons
        actions={['POLISH_LYRICS', 'CONFIRM_WORK']}
        loadingAction="POLISH_LYRICS"
        onAction={vi.fn()}
      />,
    );

    expect(screen.getByRole('button', { name: '处理中…' })).toBeDisabled();
    expect(screen.getByRole('button', { name: '确认出歌' })).toBeDisabled();
  });
});
