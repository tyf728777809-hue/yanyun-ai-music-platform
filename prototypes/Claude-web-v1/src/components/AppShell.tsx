import type { ReactNode } from 'react';
import { setRunMode, type RunMode } from '../mock/service';

interface AppShellProps {
  onBack?: () => void;
  onHome: () => void;
  onWorks: () => void;
  runMode: RunMode;
  onRunModeChange: (mode: RunMode) => void;
  children: ReactNode;
}

// 应用外壳：顶部品牌栏 + 运行模式切换 + 居中内容容器。
export function AppShell({
  onBack,
  onHome,
  onWorks,
  runMode,
  onRunModeChange,
  children,
}: AppShellProps) {
  const toggle = () => {
    const next: RunMode = runMode === 'demo' ? 'real' : 'demo';
    setRunMode(next);
    onRunModeChange(next);
  };

  return (
    <div className="shell">
      <header className="topbar">
        <div className="topbar__inner">
          <div className="topbar__left">
            {onBack ? (
              <button className="iconbtn" onClick={onBack} aria-label="返回">
                <span aria-hidden="true">‹</span>
              </button>
            ) : (
              <span className="seal" aria-hidden="true">
                燕
              </span>
            )}
            <button className="brand" onClick={onHome}>
              <span className="brand__name">燕云乐坊</span>
              <span className="brand__sub">AI 作曲工作台</span>
            </button>
          </div>

          <div className="topbar__actions">
            <button className="topbar-link" onClick={onWorks}>
              我的作品
            </button>
            <button
              className={`mode-toggle mode-toggle--${runMode}`}
              onClick={toggle}
              title={
                runMode === 'demo'
                  ? '当前为演示模式（纯本地模拟，不联网）。点击切换到本地创作服务。'
                  : '当前连接本地创作服务。点击切换到演示模式。'
              }
            >
              <span className="mode-toggle__dot" aria-hidden="true" />
              {runMode === 'demo' ? '演示模式' : '本地服务'}
            </button>
          </div>
        </div>
      </header>

      <main className="content">{children}</main>

      <footer className="footnote">
        燕云乐坊 · 创作工作台原型 ·{' '}
        {runMode === 'demo' ? '演示数据，不调用真实模型' : '连接本地创作服务'}
      </footer>
    </div>
  );
}
