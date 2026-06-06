import type { WorkStatus, GenerationStage, PackageStatus } from '../api/types';
import { STAGE_LABELS, STATUS_LABELS } from '../api/workState';

type Tone = 'neutral' | 'progress' | 'success' | 'danger' | 'gold';

const STATUS_TONE: Record<WorkStatus, Tone> = {
  DRAFT: 'neutral',
  LYRICS_GENERATING: 'progress',
  LYRICS_READY: 'gold',
  LYRICS_FAILED: 'danger',
  GENERATING: 'progress',
  GENERATED: 'success',
  FAILED: 'danger',
  CANCELLED: 'neutral',
};

export function StatusPill({ status }: { status: WorkStatus }) {
  return (
    <span className={`pill pill--${STATUS_TONE[status]}`}>
      <span className="pill__dot" aria-hidden="true" />
      {STATUS_LABELS[status]}
    </span>
  );
}

export function StagePill({ stage }: { stage: GenerationStage }) {
  return <span className="pill pill--progress pill--soft">{STAGE_LABELS[stage]}</span>;
}

const PACKAGE_LABELS: Record<PackageStatus, string> = {
  PACKAGE_NOT_READY: '未就绪',
  PACKAGE_READY: '可交接',
  PACKAGE_FETCHED: '已交接',
  PACKAGE_EXPIRED: '链接已过期',
  PACKAGE_BLOCKED: '暂不可发布',
};

const PACKAGE_TONE: Record<PackageStatus, Tone> = {
  PACKAGE_NOT_READY: 'neutral',
  PACKAGE_READY: 'success',
  PACKAGE_FETCHED: 'gold',
  PACKAGE_EXPIRED: 'danger',
  PACKAGE_BLOCKED: 'danger',
};

export function PackagePill({ status }: { status: PackageStatus }) {
  return (
    <span className={`pill pill--${PACKAGE_TONE[status]}`}>
      <span className="pill__dot" aria-hidden="true" />
      {PACKAGE_LABELS[status]}
    </span>
  );
}
