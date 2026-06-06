import { useCallback, useEffect, useState } from 'react';
import type { WorkViewProps } from './types';
import type { AvailableAction, PublishPackage } from '../../api/types';
import { Button } from '../../components/Button';
import { Banner } from '../../components/Banner';
import { Spinner } from '../../components/Spinner';
import { PackagePill } from '../../components/StatusPill';
import { useAction } from '../../hooks/useAction';
import { ApiError } from '../../api/client';
import { actionLabel } from '../../api/actions';
import { requestIdLine } from '../../api/friendlyError';
import { service } from '../../mock/service';

// 成品页：作品已生成且可交接发布。媒体优先用 media_assets，交接信息来自 publish-package。
export function FinishedView({ work, refresh, onBackToHome }: WorkViewProps) {
  const { run, busyKey } = useAction(refresh);
  const [pkg, setPkg] = useState<PublishPackage | null>(null);
  const [pkgError, setPkgError] = useState<{ message: string; requestId?: string | null } | null>(null);
  const [pkgLoading, setPkgLoading] = useState(true);

  const loadPackage = useCallback(async () => {
    setPkgLoading(true);
    try {
      const result = await service.getPublishPackage(work.work_id);
      setPkg(result);
      setPkgError(null);
    } catch (err) {
      setPkgError({
        message: err instanceof ApiError ? err.message : '作品准备信息加载失败',
        requestId: requestIdLine(err),
      });
    } finally {
      setPkgLoading(false);
    }
  }, [work.work_id]);

  useEffect(() => {
    void loadPackage();
  }, [loadPackage]);

  const media = work.media_assets;
  const fetched = work.package_status === 'PACKAGE_FETCHED' || pkg?.package_status === 'PACKAGE_FETCHED';
  const blocked = work.package_status === 'PACKAGE_BLOCKED' || pkg?.package_status === 'PACKAGE_BLOCKED';
  const packageJson = pkg?.package_json;
  const availableActions = pkg?.available_actions ?? work.available_actions;
  const hasAvailableAction = (action: AvailableAction) => availableActions.includes(action);

  async function markFetched() {
    await run(
      'MARK_PACKAGE_FETCHED',
      () => service.markPublishPackageFetched(work.work_id),
      {
        successMsg: '已标记为交接给社区发布',
        onSuccess: (p) => setPkg(p),
      },
    );
  }

  async function refreshUrl() {
    await run('REFRESH_PACKAGE_URL', () => service.refreshPublishPackageUrl(work.work_id), {
      successMsg: '下载链接已刷新',
      onSuccess: (p) => setPkg(p),
    });
  }

  async function rerenderVideo() {
    await run('RERENDER_VIDEO', () => service.rerenderVideo(work.work_id), {
      successMsg: '已重新渲染画面',
    });
  }

  return (
    <div className="work-stage finished-view">
      <header className="stage-head">
        <div className="stage-head__meta">
          <span className="done-badge">已完成</span>
          <PackagePill status={work.package_status} />
        </div>
        <h1 className="song-title">{work.song_title || '燕云未名曲'}</h1>
        {work.song_summary && <p className="song-summary">{work.song_summary}</p>}
      </header>

      {/* 媒体预览 */}
      <section className="card media-card">
        <div className="media-cover">
          {media?.cover_url ? (
            <img src={media.cover_url} alt="歌曲封面" />
          ) : (
            <div className="media-cover__placeholder">封面生成中</div>
          )}
        </div>

        <div className="media-body">
          {media?.audio_url ? (
            <div className="media-row">
              <span className="media-row__label">试听</span>
              <audio controls preload="none" src={media.audio_url} className="audio-player">
                你的浏览器不支持音频播放。
              </audio>
            </div>
          ) : null}

          {media?.video_url ? (
            <div className="media-row">
              <span className="media-row__label">画面</span>
              <video
                controls
                preload="none"
                src={media.video_url}
                poster={media.cover_url ?? undefined}
                className="video-player"
              >
                你的浏览器不支持视频播放。
              </video>
            </div>
          ) : null}
        </div>
      </section>

      {/* 作品交接区 */}
      <section className="card handoff-card">
        <div className="card__head">
          <h2 className="card__title">交给社区发布</h2>
        </div>

        <Banner tone={blocked ? 'danger' : fetched ? 'gold' : 'success'}>
          {blocked
            ? '作品暂不能交给社区发布。'
            : fetched
              ? '作品已交接给社区发布流程。'
              : '作品已准备好，可交给社区发布。'}
        </Banner>

        {pkgLoading && !pkg ? (
          <div className="handoff-loading">
            <Spinner size={22} label="正在准备作品" />
          </div>
        ) : pkgError ? (
          <Banner
            tone="danger"
            action={
              <Button tone="secondary" size="sm" onClick={() => void loadPackage()}>
                重试
              </Button>
            }
          >
            <span>{pkgError.message}</span>
            {pkgError.requestId && <span className="request-id">{pkgError.requestId}</span>}
          </Banner>
        ) : pkg ? (
          <>
            {pkg.package_url && (
              <div className="handoff-block">
                <span className="handoff-block__label">交接下载链接</span>
                <a className="handoff-url" href={pkg.package_url} target="_blank" rel="noreferrer">
                  {pkg.package_url}
                </a>
              </div>
            )}

            {pkg.package_url_expires_at && (
              <p className="handoff-note">
                作品下载链接有效期至{' '}
                {new Date(pkg.package_url_expires_at).toLocaleString('zh-CN', {
                  month: 'numeric',
                  day: 'numeric',
                  hour: '2-digit',
                  minute: '2-digit',
                })}
                ，过期可刷新。
              </p>
            )}

            {packageJson && (
              <div className="handoff-assets" aria-label="作品交接内容">
                <div className="handoff-block">
                  <span className="handoff-block__label">视频地址</span>
                  <a
                    className="handoff-url"
                    href={packageJson.video.url}
                    target="_blank"
                    rel="noreferrer"
                  >
                    {packageJson.video.url}
                  </a>
                </div>
                <div className="handoff-block">
                  <span className="handoff-block__label">封面地址</span>
                  <a
                    className="handoff-url"
                    href={packageJson.cover.url}
                    target="_blank"
                    rel="noreferrer"
                  >
                    {packageJson.cover.url}
                  </a>
                </div>
                <div className="handoff-block">
                  <span className="handoff-block__label">歌词正文</span>
                  <pre className="handoff-lyrics">{packageJson.lyrics.text}</pre>
                </div>
              </div>
            )}

            <div className="action-bar action-bar--stack">
              {hasAvailableAction('MARK_PACKAGE_FETCHED') && !fetched && !blocked && (
                <Button
                  tone="primary"
                  size="lg"
                  block
                  loading={busyKey === 'MARK_PACKAGE_FETCHED'}
                  disabled={busyKey !== null}
                  onClick={markFetched}
                >
                  标记已交接
                </Button>
              )}
              {hasAvailableAction('REFRESH_PACKAGE_URL') && (
                <Button
                  tone="secondary"
                  block
                  loading={busyKey === 'REFRESH_PACKAGE_URL'}
                  disabled={busyKey !== null}
                  onClick={refreshUrl}
                >
                  刷新下载链接
                </Button>
              )}
              {hasAvailableAction('RERENDER_VIDEO') && (
                <Button
                  tone="secondary"
                  block
                  loading={busyKey === 'RERENDER_VIDEO'}
                  disabled={busyKey !== null}
                  onClick={rerenderVideo}
                >
                  {actionLabel('RERENDER_VIDEO')}
                </Button>
              )}
              {hasAvailableAction('CONTACT_SUPPORT') && (
                <Button tone="ghost" block disabled={busyKey !== null} onClick={() => void 0}>
                  联系平台协助
                </Button>
              )}
            </div>
          </>
        ) : null}
      </section>

      <button className="textlink textlink--center" onClick={onBackToHome}>
        再创作一首
      </button>
    </div>
  );
}
