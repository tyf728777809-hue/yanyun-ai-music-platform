import { useWorkDetail } from '../hooks/useWorkDetail';
import { Spinner } from '../components/Spinner';
import { Banner } from '../components/Banner';
import { Button } from '../components/Button';
import { LyricsGeneratingView } from './work/LyricsGeneratingView';
import { ConfirmView } from './work/ConfirmView';
import { GeneratingView } from './work/GeneratingView';
import { FailedView } from './work/FailedView';
import { FinishedView } from './work/FinishedView';
import { isRecoverableConnectionError, requestIdLine } from '../api/friendlyError';

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
        <Spinner size={36} label="正在同步作品状态" />
        <Banner
          tone="info"
          title="正在同步作品状态"
          action={
            <Button tone="secondary" size="sm" onClick={() => void refresh()}>
              刷新
            </Button>
          }
        >
          作品状态正在更新，请稍候。
        </Banner>
      </div>
    );
  }

  const shared = { work, refresh, onBackToHome };
  const statusBanner = error ? (
    isRecoverableConnectionError(error) ? (
      <ReconnectingBanner error={error} refresh={refresh} />
    ) : (
      <WorkStateErrorBanner error={error} refresh={refresh} />
    )
  ) : null;

  switch (phase) {
    case 'LYRICS_GENERATING':
      return (
        <>
          {statusBanner}
          <LyricsGeneratingView {...shared} />
        </>
      );
    case 'CONFIRM':
      return (
        <>
          {statusBanner}
          <ConfirmView {...shared} />
        </>
      );
    case 'GENERATING':
      return (
        <>
          {statusBanner}
          <GeneratingView {...shared} />
        </>
      );
    case 'FAILED':
      return (
        <>
          {statusBanner}
          <FailedView {...shared} />
        </>
      );
    case 'FINISHED':
      return (
        <>
          {statusBanner}
          <FinishedView {...shared} />
        </>
      );
    default:
      return null;
  }
}

function WorkStateErrorBanner({
  error,
  refresh,
}: {
  error: NonNullable<ReturnType<typeof useWorkDetail>['error']>;
  refresh: () => Promise<void>;
}) {
  return (
    <Banner
      tone="danger"
      title="作品状态需要刷新"
      action={
        <Button tone="secondary" size="sm" onClick={() => void refresh()}>
          刷新
        </Button>
      }
    >
      <span>{error.message}</span>
      {requestIdLine(error) && <span className="request-id">{requestIdLine(error)}</span>}
    </Banner>
  );
}

function ReconnectingBanner({
  error,
  refresh,
}: {
  error: NonNullable<ReturnType<typeof useWorkDetail>['error']>;
  refresh: () => Promise<void>;
}) {
  return (
    <Banner
      tone="gold"
      title="连接短暂中断，正在重连"
      action={
        <Button tone="secondary" size="sm" onClick={() => void refresh()}>
          立即刷新
        </Button>
      }
    >
      <span>{error.message}</span>
      {requestIdLine(error) && <span className="request-id">{requestIdLine(error)}</span>}
    </Banner>
  );
}
