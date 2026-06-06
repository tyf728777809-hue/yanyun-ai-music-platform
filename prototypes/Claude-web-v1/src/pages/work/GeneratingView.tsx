import type { WorkViewProps } from './types';
import { PRODUCTION_PROGRESS_STEPS, progressStepStates, STAGE_LABELS } from '../../api/workState';

// 出歌生成中：用后端 generation_stage 推导进度步骤，轮询由 useWorkDetail 负责。
export function GeneratingView({ work }: WorkViewProps) {
  const stage = work.generation_stage;
  const states = progressStepStates(stage);
  const activeIndex = states.findIndex((s) => s === 'active');
  const total = PRODUCTION_PROGRESS_STEPS.length;
  const percent = Math.round(((activeIndex < 0 ? total : activeIndex) / total) * 100);

  return (
    <div className="work-stage generating-view">
      <header className="stage-head stage-head--center">
        <div className="brush-orb brush-orb--spin" aria-hidden="true">
          <span className="brush-orb__ring" />
          <span className="brush-orb__glyph">曲</span>
        </div>
        <h1 className="stage-title">{work.song_title || '作品'}，正在出歌</h1>
        <p className="stage-lead">当前：{STAGE_LABELS[stage]} · 这一步通常需要一点时间，页面会自动更新。</p>
      </header>

      <div className="progress-track" aria-hidden="true">
        <div className="progress-track__fill" style={{ width: `${percent}%` }} />
      </div>

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
