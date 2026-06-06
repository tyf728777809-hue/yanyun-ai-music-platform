import type { WorkViewProps } from './types';
import { Spinner } from '../../components/Spinner';

// 歌词生成中：轮询由 useWorkDetail 自动进行，这里只展示状态。
export function LyricsGeneratingView({ work }: WorkViewProps) {
  return (
    <div className="work-stage page-center work-stage--waiting">
      <div className="brush-orb" aria-hidden="true">
        <span className="brush-orb__ring" />
        <span className="brush-orb__glyph">词</span>
      </div>
      <h1 className="stage-title">正在为你谱写歌词</h1>
      <p className="stage-lead">
        燕云乐坊正循着你的灵感落笔，片刻之后就能看到初稿。
      </p>
      <div className="waiting-spinner">
        <Spinner size={22} label="创作中" />
      </div>
      <p className="work-code">作品编号 {work.work_code}</p>
    </div>
  );
}
