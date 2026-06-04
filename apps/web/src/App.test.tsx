import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {App} from './App';

describe('App', () => {
  it('renders the platform scaffold', () => {
    render(<App />);

    expect(screen.getByRole('heading', {name: '燕云 AI 作曲平台'})).toBeInTheDocument();
    expect(screen.getByText('发布包交接')).toBeInTheDocument();
  });
});
