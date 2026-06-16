import { useState } from 'react';
import type { WorkViewProps } from './types';
import { Button } from '../../components/Button';
import { Modal } from '../../components/Modal';
import { Banner } from '../../components/Banner';
import { TextAreaField } from '../../components/Field';
import { PackagePill, StagePill, StatusPill } from '../../components/StatusPill';
import { useToast } from '../../components/Toast';
import { useAction } from '../../hooks/useAction';
import { hasAction } from '../../api/workState';
import { service } from '../../mock/service';

type EditKind = 'polish' | 'continue';
type EditRecoveryResult = 'queued' | 'completed' | null;

const EDIT_RECOVERY_ATTEMPTS = 12;
const EDIT_RECOVERY_INTERVAL_MS = 2000;
const SLOW_LYRICS_EDIT_MS = 90_000;

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

function isConnectionInterrupted(message: string): boolean {
  return message.includes('作曲服务连接中断');
}

function jobElapsedMs(createdAt?: string | null): number {
  if (!createdAt) return 0;
  const startedAt = new Date(createdAt).getTime();
  if (!Number.isFinite(startedAt)) return 0;
  return Math.max(0, Date.now() - startedAt);
}

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
    // Embedded browsers may expose clipboard APIs but reject writes without permission.
  }

  return copyTextWithTextarea(text);
}

// 歌词确认 / 作品详情页。
export function ConfirmView({ work, refresh }: WorkViewProps) {
  const { run, busyKey } = useAction();
  const toast = useToast();
  const [editKind, setEditKind] = useState<EditKind | null>(null);
  const [instruction, setInstruction] = useState('');
  const [editError, setEditError] = useState<string | null>(null);

  const draft = work.lyrics_draft;
  const remaining = work.polish_remaining_count;
  const canPolish = hasAction(work, 'POLISH_LYRICS');
  const canContinue = hasAction(work, 'CONTINUE_LYRICS');
  const canConfirm = hasAction(work, 'CONFIRM_WORK');
  const editsUsedUp = remaining <= 0;
  const lyricsText = draft?.lyrics_text?.trim() ?? '';
  const hasLyrics = lyricsText.length > 0;
  const activeLyricsJob = work.active_lyrics_job;
  const lyricsEditRunning = Boolean(activeLyricsJob);
  const lyricsEditSlow = lyricsEditRunning && jobElapsedMs(activeLyricsJob?.created_at) >= SLOW_LYRICS_EDIT_MS;
  const lastLyricsEditFailure = work.last_lyrics_edit_failure;

  function openEditor(kind: EditKind) {
    setEditKind(kind);
    setInstruction('');
    setEditError(null);
  }

  async function submitEdit() {
    if (!editKind) return;
    const kind = editKind;
    const trimmed = instruction.trim();
    if (kind === 'polish' && !trimmed) return;
    setEditError(null);
    const previousVersion = draft?.version_no ?? 0;
    await run(
      kind,
      () =>
        kind === 'polish'
          ? service.polishLyrics(work.work_id, { instruction: trimmed })
          : service.continueLyrics(work.work_id, { instruction: trimmed || undefined }),
      {
        successMsg: kind === 'polish' ? '已开始润色歌词' : '已开始续写歌词',
        conflictMsg: '改词次数已用完，本次未生效',
        onSuccess: async () => {
          await refresh();
          setEditKind(null);
        },
        onError: async (message) => {
          if (isConnectionInterrupted(message)) {
            setEditError('请求可能仍在后台处理中，正在确认任务状态…');
            const recovered = await waitForRecoveredEdit(previousVersion);
            if (recovered === 'completed') {
              setEditError(null);
              setEditKind(null);
              toast.success(kind === 'polish' ? '已获取润色后的歌词' : '已获取续写后的歌词');
              return;
            }
            if (recovered === 'queued') {
              setEditError(null);
              setEditKind(null);
              toast.show(kind === 'polish' ? 'AI 润色已进入后台处理' : 'AI 续写已进入后台处理');
              return;
            }
          }
          setEditError(message);
        },
        suppressErrorToast: true,
      },
    );
  }

  async function waitForRecoveredEdit(previousVersion: number): Promise<EditRecoveryResult> {
    for (let attempt = 0; attempt < EDIT_RECOVERY_ATTEMPTS; attempt += 1) {
      if (attempt > 0) {
        await sleep(EDIT_RECOVERY_INTERVAL_MS);
      }
      try {
        const latest = await service.getWork(work.work_id);
        const latestVersion = latest.lyrics_draft?.version_no ?? 0;
        if (latestVersion > previousVersion) {
          await refresh();
          return 'completed';
        }
        if (latest.active_lyrics_job) {
          await refresh();
          return 'queued';
        }
      } catch {
        // Keep polling: the short follow-up GET may race with proxy reconnects.
      }
    }
    return null;
  }

  async function confirm() {
    await run(
      'confirm',
      () =>
        service.confirmWork(work.work_id, {
          lyrics_draft_id: draft?.lyrics_draft_id,
          user_confirmed_at: new Date().toISOString(),
        }),
      {
        successMsg: '已确认，开始出歌',
        onSuccess: refresh,
        onError: async (message) => {
          if (message.includes('歌词已更新')) {
            await refresh();
          }
        },
      },
    );
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
    <div className="work-stage confirm-view">
      <header className="stage-head">
        <div className="stage-head__meta">
          <StatusPill status={work.status} />
          <StagePill stage={work.generation_stage} />
          <PackagePill status={work.package_status} />
          <span className="work-code">作品编号 {work.work_code}</span>
        </div>
        <h1 className="song-title">{draft?.song_title || work.song_title || '燕云未名曲'}</h1>
        {(draft?.song_summary || work.song_summary) && (
          <p className="song-summary">{draft?.song_summary || work.song_summary}</p>
        )}
      </header>

      {lyricsEditRunning && (
        <Banner tone="info" title={activeLyricsJob?.operation === 'CONTINUE' ? 'AI 正在续写' : 'AI 正在润色'}>
          <span>{activeLyricsJob?.message || 'AI 正在处理歌词，原歌词会保留。'}</span>
          <span className="banner-line">完成后页面会自动刷新，请先不要确认出歌。</span>
          {lyricsEditSlow && (
            <span className="banner-line">这次 AI 响应较慢，仍在后台处理中，原歌词会保留。</span>
          )}
        </Banner>
      )}

      {!lyricsEditRunning && lastLyricsEditFailure && (
        <Banner tone="gold" title="上一次 AI 改词没有完成">
          <span>{lastLyricsEditFailure.failure_message || '原歌词已保留，你可以稍后再试。'}</span>
        </Banner>
      )}

      {/* 改词额度提示 */}
      <Banner tone={editsUsedUp ? 'gold' : 'info'}>
        <span>
          {editsUsedUp ? (
            <>AI 改词次数已用完，确认满意后即可出歌。</>
          ) : (
            <>
              还可使用 <strong>{remaining}</strong> 次 AI 改词（润色 / 续写共用）。
            </>
          )}
        </span>
        {work.quota_hint?.message && <span className="banner-line">{work.quota_hint.message}</span>}
        {work.publish_handoff_hint?.ready_for_handoff && work.publish_handoff_hint.message && (
          <span className="banner-line">{work.publish_handoff_hint.message}</span>
        )}
      </Banner>

      {/* 歌词正文 */}
      <section className="card lyrics-card">
        <div className="card__head">
          <h2 className="card__title">歌词</h2>
          <div className="card__actions">
            {draft && <span className="version-tag">第 {draft.version_no} 版</span>}
            {hasLyrics && (
              <Button tone="secondary" size="sm" onClick={copyLyrics}>
                复制歌词
              </Button>
            )}
          </div>
        </div>
        <pre className="lyrics-text">{draft?.lyrics_text || '歌词准备中…'}</pre>
      </section>

      {/* 创作提示 */}
      {draft && (draft.yanyun_references?.length || draft.risk_notes?.length) ? (
        <section className="card detail-card">
          {draft.yanyun_references && draft.yanyun_references.length > 0 && (
            <div className="detail-block">
              <h3 className="detail-block__title">燕云意象</h3>
              <div className="ref-list">
                {draft.yanyun_references.map((ref, i) => (
                  <span key={i} className="ref-chip">
                    {ref}
                  </span>
                ))}
              </div>
            </div>
          )}
          {draft.risk_notes && draft.risk_notes.length > 0 && (
            <div className="detail-block">
              <h3 className="detail-block__title">创作提示</h3>
              <ul className="risk-list">
                {draft.risk_notes.map((note, i) => (
                  <li key={i}>{note}</li>
                ))}
              </ul>
            </div>
          )}
        </section>
      ) : null}

      {/* 操作区：按钮由 available_actions 驱动 */}
      <div className="action-bar">
        {canPolish && (
          <Button
            tone="secondary"
            loading={busyKey === 'polish'}
            disabled={busyKey !== null || lyricsEditRunning}
            onClick={() => openEditor('polish')}
          >
            AI 润色
          </Button>
        )}
        {canContinue && (
          <Button
            tone="secondary"
            loading={busyKey === 'continue'}
            disabled={busyKey !== null || lyricsEditRunning}
            onClick={() => openEditor('continue')}
          >
            AI 续写
          </Button>
        )}
        {canConfirm && (
          <Button
            tone="primary"
            loading={busyKey === 'confirm'}
            disabled={busyKey !== null || lyricsEditRunning}
            onClick={confirm}
          >
            确认出歌
          </Button>
        )}
      </div>

      <EditModal
        kind={editKind}
        remaining={remaining}
        instruction={instruction}
        onInstruction={setInstruction}
        busy={busyKey === 'polish' || busyKey === 'continue'}
        onCancel={() => setEditKind(null)}
        onSubmit={submitEdit}
        error={editError}
      />
    </div>
  );
}

function EditModal({
  kind,
  remaining,
  instruction,
  onInstruction,
  busy,
  error,
  onCancel,
  onSubmit,
}: {
  kind: EditKind | null;
  remaining: number;
  instruction: string;
  onInstruction: (v: string) => void;
  busy: boolean;
  error: string | null;
  onCancel: () => void;
  onSubmit: () => void;
}) {
  const isPolish = kind === 'polish';
  const needsInstruction = isPolish && instruction.trim().length === 0;
  const showInstructionError = isPolish && instruction.length > 0 && instruction.trim().length === 0;
  return (
    <Modal
      open={kind !== null}
      title={isPolish ? 'AI 润色歌词' : 'AI 续写歌词'}
      subtitle={`成功生成新版后消耗 1 次改词额度，还剩 ${remaining} 次`}
      onClose={busy ? () => {} : onCancel}
      footer={
        <>
          <Button tone="ghost" onClick={onCancel} disabled={busy}>
            取消
          </Button>
          <Button tone="primary" loading={busy} disabled={needsInstruction} onClick={onSubmit}>
            {isPolish ? '开始润色' : '开始续写'}
          </Button>
        </>
      }
    >
      <TextAreaField
        label="想让 AI 怎么改？"
        optional={!isPolish}
        hint={isPolish ? '请写明润色方向，例如：更口语化、押韵更工整' : '例如：补一段副歌、收一个有力的尾声'}
        placeholder={isPolish ? '请写下润色方向' : '不填也可以，AI 会自行把握'}
        rows={4}
        maxLength={500}
        value={instruction}
        autoFocus
        onChange={(e) => onInstruction(e.target.value)}
      />
      {showInstructionError && <p className="field-error">请先写下润色方向。</p>}
      {error && (
        <p className="modal-error" role="alert">
          {error}
        </p>
      )}
    </Modal>
  );
}
