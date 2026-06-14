import { useCallback, useEffect, useState } from 'react';
import type { WorkViewProps } from './types';
import type { AvailableAction, PublishPackage } from '../../api/types';
import { Button } from '../../components/Button';
import { Banner } from '../../components/Banner';
import { Modal } from '../../components/Modal';
import { Spinner } from '../../components/Spinner';
import { PackagePill } from '../../components/StatusPill';
import { useToast } from '../../components/Toast';
import { useAction } from '../../hooks/useAction';
import { ApiError } from '../../api/client';
import { actionLabel } from '../../api/actions';
import { requestIdLine } from '../../api/friendlyError';
import { service } from '../../mock/service';

function copyTextWithTextarea(text: string): boolean {
  if (typeof document === 'undefined' || !document.body) {
    return false;
  }

  const textarea = document.createElement('textarea');
  textarea.value = text;
  textarea.setAttribute('readonly', '');
  textarea.style.position = 'fixed';
  textarea.style.top = '0';
  textarea.style.left = '-9999px';
  textarea.style.width = '1px';
  textarea.style.height = '1px';
  textarea.style.opacity = '0';
  textarea.style.pointerEvents = 'none';

  document.body.appendChild(textarea);
  textarea.focus({ preventScroll: true });
  textarea.select();
  textarea.setSelectionRange(0, textarea.value.length);

  try {
    return document.execCommand?.('copy') === true;
  } finally {
    textarea.remove();
  }
}

async function writeClipboardText(text: string): Promise<boolean> {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
      return true;
    }
  } catch {
    // Some embedded browsers expose navigator.clipboard but reject writes without focus/permission.
  }

  return copyTextWithTextarea(text);
}

// 成品页：作品已生成且可交接发布。媒体优先用 media_assets，交接信息来自 publish-package。
export function FinishedView({ work, refresh, onBackToHome }: WorkViewProps) {
  const { run, busyKey } = useAction(refresh);
  const toast = useToast();
  const [pkg, setPkg] = useState<PublishPackage | null>(null);
  const [pkgError, setPkgError] = useState<{ message: string; requestId?: string | null } | null>(null);
  const [pkgLoading, setPkgLoading] = useState(true);
  const [supportOpen, setSupportOpen] = useState(false);

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
  const expired = work.package_status === 'PACKAGE_EXPIRED' || pkg?.package_status === 'PACKAGE_EXPIRED';
  const availableActions = pkg?.available_actions ?? work.available_actions;
  const hasAvailableAction = (action: AvailableAction) => availableActions.includes(action);
  const lyricsText =
    work.lyrics_draft?.lyrics_text?.trim() || pkg?.package_json?.lyrics?.text?.trim() || '';
  const hasLyrics = lyricsText.length > 0;

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

  async function copyLyrics() {
    if (!hasLyrics) return;
    const copied = await writeClipboardText(lyricsText);
    if (copied) {
      toast.success('歌词已复制');
    } else {
      toast.error('复制失败，请长按歌词手动复制');
    }
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

      {/* 作品预览 */}
      <section className="card preview-card">
        <div className="card__head">
          <h2 className="card__title">作品预览</h2>
        </div>

        <div className="preview-layout">
          <div className="preview-media">
            <div className="preview-video-shell">
              {media?.video_url ? (
                <video
                  controls
                  preload="none"
                  src={media.video_url}
                  poster={media.cover_url ?? undefined}
                  className="video-player preview-video"
                >
                  你的浏览器不支持视频播放。
                </video>
              ) : media?.cover_url ? (
                <img className="preview-cover-fallback" src={media.cover_url} alt="歌曲封面" />
              ) : (
                <div className="preview-media__placeholder">作品画面准备中</div>
              )}
            </div>

            {media?.audio_url ? (
              <details className="audio-fallback">
                <summary>仅听音频</summary>
                <audio controls preload="none" src={media.audio_url} className="audio-player">
                  你的浏览器不支持音频播放。
                </audio>
              </details>
            ) : null}
          </div>

          <aside className="lyrics-panel" aria-label="歌词">
            <div className="lyrics-panel__head">
              <h3 className="lyrics-panel__title">歌词</h3>
              {hasLyrics && (
                <Button tone="secondary" size="sm" onClick={copyLyrics}>
                  复制歌词
                </Button>
              )}
            </div>
            {hasLyrics ? (
              <pre className="finished-lyrics-text">{lyricsText}</pre>
            ) : (
              <div className="finished-lyrics-empty">歌词暂未准备好</div>
            )}
          </aside>
        </div>
      </section>

      {/* 作品交接区 */}
      <section className="card handoff-card">
        <div className="card__head">
          <h2 className="card__title">交给社区发布</h2>
        </div>

        <Banner tone={blocked ? 'danger' : expired ? 'gold' : fetched ? 'gold' : 'success'}>
          {blocked
            ? '作品暂不能交给社区发布。'
            : expired
              ? '作品链接已过期，请先刷新下载链接。'
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
                <span className="handoff-block__label">作品素材</span>
                <a className="handoff-url" href={pkg.package_url} target="_blank" rel="noreferrer">
                  打开作品素材
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

            <div className="handoff-assets" aria-label="作品交接内容">
              {media?.audio_url && (
                <div className="handoff-block">
                  <span className="handoff-block__label">音频</span>
                  <a
                    className="handoff-url"
                    href={media.audio_url}
                    target="_blank"
                    rel="noreferrer"
                  >
                    打开音频
                  </a>
                </div>
              )}
              {media?.video_url && (
                <div className="handoff-block">
                  <span className="handoff-block__label">视频</span>
                  <a
                    className="handoff-url"
                    href={media.video_url}
                    target="_blank"
                    rel="noreferrer"
                  >
                    打开视频
                  </a>
                </div>
              )}
              {media?.cover_url && (
                <div className="handoff-block">
                  <span className="handoff-block__label">封面</span>
                  <a
                    className="handoff-url"
                    href={media.cover_url}
                    target="_blank"
                    rel="noreferrer"
                  >
                    打开封面
                  </a>
                </div>
              )}
              {work.song_summary && (
                <div className="handoff-block">
                  <span className="handoff-block__label">作品摘要</span>
                  <p className="handoff-summary">{work.song_summary}</p>
                </div>
              )}
              </div>

            <div className="action-bar action-bar--stack">
              {hasAvailableAction('MARK_PACKAGE_FETCHED') && !fetched && !blocked && !expired && (
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
                <Button
                  tone="ghost"
                  block
                  disabled={busyKey !== null}
                  onClick={() => setSupportOpen(true)}
                >
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
      <Modal
        open={supportOpen}
        title="联系平台协助"
        subtitle="把这些信息发给平台或开发同事，方便定位发布交接问题。"
        onClose={() => setSupportOpen(false)}
        footer={
          <Button tone="primary" onClick={() => setSupportOpen(false)}>
            我知道了
          </Button>
        }
      >
        <div className="support-panel">
          <p>作品编号：{work.work_code}</p>
          <p>作品 ID：{work.work_id}</p>
          <p>交接状态：{pkg?.package_status ?? work.package_status}</p>
        </div>
      </Modal>
    </div>
  );
}
