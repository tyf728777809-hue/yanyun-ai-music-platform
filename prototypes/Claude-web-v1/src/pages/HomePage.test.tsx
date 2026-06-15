import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ToastProvider } from '../components/Toast';
import { HomePage } from './HomePage';

function renderHomePage() {
  return render(
    <ToastProvider>
      <HomePage onWorkCreated={() => {}} onOpenWorks={() => {}} />
    </ToastProvider>,
  );
}

describe('HomePage', () => {
  it('describes lyrics mode as preparing a draft before music generation', () => {
    renderHomePage();

    expect(screen.getByText('已有词，先整理歌词草稿')).toBeInTheDocument();
    expect(screen.queryByText('已有词，直接谱成曲')).not.toBeInTheDocument();

    fireEvent.click(screen.getByText('填词成歌'));

    expect(screen.getByRole('button', { name: '整理歌词草稿' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '开始谱曲' })).not.toBeInTheDocument();
  });
});
