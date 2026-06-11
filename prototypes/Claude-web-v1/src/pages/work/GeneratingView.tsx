import { useEffect, useState } from 'react';
import type { WorkViewProps } from './types';
import { PRODUCTION_PROGRESS_STEPS, progressStepStates, STAGE_LABELS } from '../../api/workState';

const STAGE_HINTS: Partial<Record<string, string>> = {
  QUOTA_LOCKING: '正在准备生成资源，通常只需要几秒。',
  MUSIC_GENERATING: '正在谱写旋律，真实模型通常需要 2-5 分钟。',
  COVER_GENERATING: '正在绘制封面，通常需要 30-90 秒。',
  TIMELINE_BUILDING: '正在整理成片素材，当前默认不做字幕同步。',
  VIDEO_RENDERING: '正在封装默认视频，通常需要 10-30 秒。',
  PACKAGE_BUILDING: '正在整理作品素材，通常只需要几秒。',
  PACKAGE_PRECHECK: '正在做发布前检查，通常只需要几秒。',
  PACKAGE_READY: '作品即将完成，页面会自动跳转。',
};

function formatElapsed(seconds: number) {
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  if (minutes <= 0) return `${rest} 秒`;
  return `${minutes} 分 ${rest.toString().padStart(2, '0')} 秒`;
}

// 出歌生成中：用后端 generation_stage 推导进度步骤，轮询由 useWorkDetail 负责。
export function GeneratingView({ work }: WorkViewProps) {
  const [elapsed, setElapsed] = useState(0);
  const stage = work.generation_stage;
  const states = progressStepStates(stage);
  const activeIndex = states.findIndex((s) => s === 'active');
  const total = PRODUCTION_PROGRESS_STEPS.length;
  const percent = Math.round(((activeIndex < 0 ? total : activeIndex) / total) * 100);
  const stageHint = STAGE_HINTS[stage] ?? '这一步正在处理中，页面会自动更新。';

  useEffect(() => {
    setElapsed(0);
    const timer = window.setInterval(() => {
      setElapsed((value) => value + 1);
    }, 1000);
    return () => window.clearInterval(timer);
  }, [work.work_id, stage]);

  return (
    <div className="work-stage generating-view">
      <header className="stage-head stage-head--center">
        <div className="brush-orb brush-orb--spin" aria-hidden="true">
          <span className="brush-orb__ring" />
          <span className="brush-orb__glyph">曲</span>
        </div>
        <h1 className="stage-title">{work.song_title || '作品'}，正在出歌</h1>
        <p className="stage-lead">当前：{STAGE_LABELS[stage]} · 页面会自动更新。</p>
      </header>

      <div className="progress-track" aria-hidden="true">
        <div className="progress-track__fill" style={{ width: `${percent}%` }} />
      </div>

      <section className="generation-status" aria-live="polite">
        <span>{stageHint}</span>
        <strong>已等待 {formatElapsed(elapsed)}</strong>
      </section>

      <ol className="steps">
        {PRODUCTION_PROGRESS_STEPS.map((step, i) => (
          <li key={step.key} className={`step step--${states[i]}`}>
            <span className="step__marker">
              {states[i] === 'done' ? '✓' : states[i] === 'active' ? '' : i + 1}
            </span>
            <span className="step__label">{step.label}</span>
            {states[i] === 'active' && <span className="step__pulse" aria-hidden="true" />}
          </li>
        ))}
      </ol>

      <p className="work-code">作品编号 {work.work_code}</p>
    </div>
  );
}
