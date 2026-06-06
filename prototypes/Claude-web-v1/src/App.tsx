import { useCallback, useEffect, useState } from 'react';
import { AppShell } from './components/AppShell';
import { ToastProvider } from './components/Toast';
import { HomePage } from './pages/HomePage';
import { WorksPage } from './pages/WorksPage';
import { WorkPage } from './pages/WorkPage';
import { getRunMode, type RunMode } from './mock/service';

// 极简哈希路由：#/ → 创作首页；#/works → 我的作品；#/work/:id → 作品页。
// 无需引入路由库，保持原型自包含。
type Route = { name: 'home' } | { name: 'works' } | { name: 'work'; workId: string };

function parseHash(): Route {
  const hash = window.location.hash.replace(/^#/, '');
  if (hash.match(/^\/works(?:[/?]|$)/)) return { name: 'works' };
  const match = hash.match(/^\/work\/([^/?]+)/);
  if (match) return { name: 'work', workId: decodeURIComponent(match[1]) };
  return { name: 'home' };
}

export function App() {
  const [route, setRoute] = useState<Route>(() => parseHash());
  const [runMode, setRunMode] = useState<RunMode>(() => getRunMode());

  useEffect(() => {
    const onHashChange = () => setRoute(parseHash());
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, []);

  const goHome = useCallback(() => {
    window.location.hash = '/';
  }, []);

  const goWork = useCallback((workId: string) => {
    window.location.hash = `/work/${encodeURIComponent(workId)}`;
  }, []);

  const goWorks = useCallback(() => {
    window.location.hash = '/works';
  }, []);

  // 切换运行模式时回到首页，避免拿着旧模式的 workId 跨库查询。
  const handleRunModeChange = useCallback(
    (mode: RunMode) => {
      setRunMode(mode);
      if (route.name !== 'home') goHome();
    },
    [route.name, goHome],
  );

  return (
    <ToastProvider>
      <AppShell
        onHome={goHome}
        onWorks={goWorks}
        onBack={route.name === 'work' || route.name === 'works' ? goHome : undefined}
        runMode={runMode}
        onRunModeChange={handleRunModeChange}
      >
        {route.name === 'home' ? (
          <HomePage onWorkCreated={goWork} onOpenWorks={goWorks} />
        ) : route.name === 'works' ? (
          <WorksPage onOpenWork={goWork} />
        ) : (
          <WorkPage
            key={`${runMode}:${route.workId}`}
            workId={route.workId}
            onBackToHome={goHome}
          />
        )}
      </AppShell>
    </ToastProvider>
  );
}
