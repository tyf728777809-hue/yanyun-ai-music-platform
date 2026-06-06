import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it } from 'vitest';
import { ToastProvider } from '../components/Toast';
import { mockBackend } from '../mock/mockBackend';
import { setRunMode } from '../mock/service';
import { WorksPage } from './WorksPage';

describe('WorksPage', () => {
  beforeEach(() => {
    setRunMode('demo');
  });

  it('renders works returned by listWorks', async () => {
    await mockBackend.createFromLyrics({
      lyrics_input: '一段列表测试歌词',
      song_title: '列表里的歌',
    });

    render(
      <ToastProvider>
        <WorksPage onOpenWork={() => {}} />
      </ToastProvider>,
    );

    expect(await screen.findByText('列表里的歌')).toBeInTheDocument();
    expect(screen.getByText('创作记录')).toBeInTheDocument();
  });
});
