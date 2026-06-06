import { useWorkDetail } from '../hooks/useWorkDetail';
import { Spinner } from '../components/Spinner';
import { Banner } from '../components/Banner';
import { Button } from '../components/Button';
import { LyricsGeneratingView } from './work/LyricsGeneratingView';
import { ConfirmView } from './work/ConfirmView';
import { GeneratingView } from './work/GeneratingView';
import { FailedView } from './work/FailedView';
import { FinishedView } from './work/FinishedView';
import { requestIdLine } from '../api/friendlyError';

interface WorkPageProps {
  workId: string;
  onBackToHome: () => void;
}

// 作品页路由：拉取详情 → 按后端状态派生的阶段渲染对应子视图。
export function WorkPage({ workId, onBackToHome }: WorkPageProps) {
  const { work, phase, loading, error, refresh } = useWorkDetail(workId);

  if (loading && !work) {
    return (
      <div className="page-center">
        <Spinner size={36} label="正在打开作品" />
      </div>
    );
  }

  if (error && !work) {
    return (
      <div className="page-center">
        <Banner
          tone="danger"
          title="没能打开作品"
          action={
            <Button tone="secondary" size="sm" onClick={() => void refresh()}>
              重试
            </Button>
          }
        >
          <span>{error.message}</span>
          {requestIdLine(error) && <span className="request-id">{requestIdLine(error)}</span>}
        </Banner>
        <button className="textlink" onClick={onBackToHome}>
          返回创作首页
        </button>
      </div>
    );
  }

  if (!work || !phase) {
    return (
      <div className="page-center">
        <Banner tone="danger" title="作品状态无法识别">
          请刷新后重试。
        </Banner>
      </div>
    );
  }

  const shared = { work, refresh, onBackToHome };

  switch (phase) {
    case 'LYRICS_GENERATING':
      return <LyricsGeneratingView {...shared} />;
    case 'CONFIRM':
      return <ConfirmView {...shared} />;
    case 'GENERATING':
      return <GeneratingView {...shared} />;
    case 'FAILED':
      return <FailedView {...shared} />;
    case 'FINISHED':
      return <FinishedView {...shared} />;
    default:
      return null;
  }
}
