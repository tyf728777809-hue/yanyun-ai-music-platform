import { useCallback, useEffect, useState } from 'react';
import { Banner } from '../components/Banner';
import { Button } from '../components/Button';
import { Spinner } from '../components/Spinner';
import { PackagePill, StagePill, StatusPill } from '../components/StatusPill';
import { ApiError } from '../api/client';
import { requestIdLine } from '../api/friendlyError';
import type { WorkListResponse, WorkSummary } from '../api/types';
import { service } from '../mock/service';

interface WorksPageProps {
  onOpenWork: (workId: string) => void;
}

export function WorksPage({ onOpenWork }: WorksPageProps) {
  const [data, setData] = useState<WorkListResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<{ message: string; requestId?: string | null } | null>(null);

  const loadPage = useCallback(async (page: number, append = false) => {
    append ? setLoadingMore(true) : setLoading(true);
    setError(null);
    try {
      const result = await service.listWorks({ page, page_size: 20 });
      setData((prev) =>
        append && prev
          ? {
              items: [...prev.items, ...result.items],
              pagination: result.pagination,
            }
          : result,
      );
    } catch (err) {
      const message = err instanceof ApiError ? err.message : '作品列表加载失败';
      setError({ message, requestId: requestIdLine(err) });
    } finally {
      append ? setLoadingMore(false) : setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadPage(1);
  }, [loadPage]);

  if (loading && !data) {
    return (
      <div className="page-center">
        <Spinner size={36} label="正在读取作品" />
      </div>
    );
  }

  return (
    <div className="works-page">
      <header className="stage-head">
        <p className="hero__eyebrow">我的作品</p>
        <h1 className="song-title">创作记录</h1>
        <p className="song-summary">查看已创建的歌词、生成状态和可交接作品。</p>
      </header>

      {error && (
        <Banner
          tone="danger"
          title="没能读取作品"
          action={
            <Button tone="secondary" size="sm" onClick={() => void loadPage(1)}>
              重试
            </Button>
          }
        >
          <span>{error.message}</span>
          {error.requestId && <span className="request-id">{error.requestId}</span>}
        </Banner>
      )}

      {data && data.items.length > 0 ? (
        <>
          <div className="work-list">
            {data.items.map((item) => (
              <WorkListItem key={item.work_id} item={item} onOpen={() => onOpenWork(item.work_id)} />
            ))}
          </div>
          {data.pagination.has_more && (
            <Button
              tone="secondary"
              block
              loading={loadingMore}
              onClick={() => void loadPage(data.pagination.page + 1, true)}
            >
              继续加载
            </Button>
          )}
        </>
      ) : (
        !error && (
          <section className="card empty-card">
            <h2 className="card__title">还没有作品</h2>
            <p className="song-summary">从灵感或歌词开始创作后，作品会出现在这里。</p>
          </section>
        )
      )}
    </div>
  );
}

function WorkListItem({ item, onOpen }: { item: WorkSummary; onOpen: () => void }) {
  const title = item.song_title || '燕云未名曲';
  return (
    <button className="work-list-item" onClick={onOpen}>
      <div className="work-list-item__thumb">
        {item.cover_url ? <img src={item.cover_url} alt="" /> : <span>燕</span>}
      </div>
      <div className="work-list-item__body">
        <div className="work-list-item__title-row">
          <h2>{title}</h2>
          <span className="work-code">{item.work_code}</span>
        </div>
        <div className="work-list-item__pills">
          <StatusPill status={item.status} />
          <StagePill stage={item.generation_stage} />
          <PackagePill status={item.package_status} />
        </div>
        <div className="work-list-item__meta">
          <span>更新于 {formatDate(item.updated_at)}</span>
          {item.video_preview_url && <span>视频已生成</span>}
        </div>
      </div>
    </button>
  );
}

function formatDate(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}
