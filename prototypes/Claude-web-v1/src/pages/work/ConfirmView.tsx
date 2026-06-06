import { useState } from 'react';
import type { WorkViewProps } from './types';
import { Button } from '../../components/Button';
import { Modal } from '../../components/Modal';
import { Banner } from '../../components/Banner';
import { TextAreaField } from '../../components/Field';
import { PackagePill, StagePill, StatusPill } from '../../components/StatusPill';
import { useAction } from '../../hooks/useAction';
import { hasAction } from '../../api/workState';
import { service } from '../../mock/service';

type EditKind = 'polish' | 'continue';

// 歌词确认 / 作品详情页。
export function ConfirmView({ work, refresh }: WorkViewProps) {
  const { run, busyKey } = useAction(refresh);
  const [editKind, setEditKind] = useState<EditKind | null>(null);
  const [instruction, setInstruction] = useState('');

  const draft = work.lyrics_draft;
  const remaining = work.polish_remaining_count;
  const canPolish = hasAction(work, 'POLISH_LYRICS');
  const canContinue = hasAction(work, 'CONTINUE_LYRICS');
  const canConfirm = hasAction(work, 'CONFIRM_WORK');
  const editsUsedUp = remaining <= 0;

  function openEditor(kind: EditKind) {
    setEditKind(kind);
    setInstruction('');
  }

  async function submitEdit() {
    if (!editKind) return;
    const kind = editKind;
    const trimmed = instruction.trim();
    if (kind === 'polish' && !trimmed) return;
    await run(
      kind,
      () =>
        kind === 'polish'
          ? service.polishLyrics(work.work_id, { instruction: trimmed })
          : service.continueLyrics(work.work_id, { instruction: trimmed || undefined }),
      {
        successMsg: kind === 'polish' ? '已为你润色歌词' : '已为你续写歌词',
        conflictMsg: '改词次数已用完，本次未生效',
        onSuccess: () => setEditKind(null),
      },
    );
  }

  async function confirm() {
    await run(
      'confirm',
      () =>
        service.confirmWork(work.work_id, {
          lyrics_draft_id: draft?.lyrics_draft_id,
          user_confirmed_at: new Date().toISOString(),
          music_provider: 'mock',
        }),
      { successMsg: '已确认，开始出歌' },
    );
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
        {work.publish_handoff_hint?.message && (
          <span className="banner-line">{work.publish_handoff_hint.message}</span>
        )}
      </Banner>

      {/* 歌词正文 */}
      <section className="card lyrics-card">
        <div className="card__head">
          <h2 className="card__title">歌词</h2>
          {draft && <span className="version-tag">第 {draft.version_no} 版</span>}
        </div>
        <pre className="lyrics-text">{draft?.lyrics_text || '歌词准备中…'}</pre>
      </section>

      {/* 燕云引用 + music prompt */}
      {draft && (draft.yanyun_references?.length || draft.music_prompt) ? (
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
          {draft.music_prompt && (
            <div className="detail-block">
              <h3 className="detail-block__title">编曲方向</h3>
              <p className="music-prompt">{draft.music_prompt}</p>
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
            disabled={busyKey !== null}
            onClick={() => openEditor('polish')}
          >
            AI 润色
          </Button>
        )}
        {canContinue && (
          <Button
            tone="secondary"
            loading={busyKey === 'continue'}
            disabled={busyKey !== null}
            onClick={() => openEditor('continue')}
          >
            AI 续写
          </Button>
        )}
        {canConfirm && (
          <Button
            tone="primary"
            loading={busyKey === 'confirm'}
            disabled={busyKey !== null}
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
  onCancel,
  onSubmit,
}: {
  kind: EditKind | null;
  remaining: number;
  instruction: string;
  onInstruction: (v: string) => void;
  busy: boolean;
  onCancel: () => void;
  onSubmit: () => void;
}) {
  const isPolish = kind === 'polish';
  const needsInstruction = isPolish && instruction.trim().length === 0;
  return (
    <Modal
      open={kind !== null}
      title={isPolish ? 'AI 润色歌词' : 'AI 续写歌词'}
      subtitle={`本次将消耗 1 次改词额度，还剩 ${remaining} 次`}
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
        onChange={(e) => onInstruction(e.target.value)}
      />
      {needsInstruction && <p className="field-error">请先写下润色方向。</p>}
    </Modal>
  );
}
