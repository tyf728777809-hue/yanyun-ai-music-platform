import type {
  AvailableAction,
  FailureCode,
  GenerationStage,
  PackageStatus,
  WorkDetail,
  WorkStatus,
} from './types';

// 把后端状态收敛成 5 个 UI 阶段。规则只看后端字段，绝不自行猜测。
export type WorkViewPhase =
  | 'LYRICS_GENERATING' // 正在创作歌词
  | 'CONFIRM' // 歌词确认 / 作品详情
  | 'GENERATING' // 出歌生成中
  | 'FAILED' // 失败
  | 'FINISHED'; // 成品

const LYRICS_GENERATING_STAGES = new Set<GenerationStage>([
  'USER_INPUT_PRECHECK',
  'LYRICS_GENERATING',
  'LYRICS_PRECHECK',
]);

const PRODUCTION_STAGES = new Set<GenerationStage>([
  'QUOTA_LOCKING',
  'MUSIC_GENERATING',
  'COVER_GENERATING',
  'TIMELINE_BUILDING',
  'VIDEO_RENDERING',
  'PACKAGE_BUILDING',
  'PACKAGE_PRECHECK',
]);

const HANDOFF_PACKAGE_STATUSES = new Set<PackageStatus>([
  'PACKAGE_READY',
  'PACKAGE_FETCHED',
  'PACKAGE_EXPIRED',
  'PACKAGE_BLOCKED',
]);

export function deriveViewPhase(work: WorkDetail): WorkViewPhase {
  const { status, generation_stage: stage, package_status: pkg, failure } = work;

  // 失败优先：只要后端给了失败状态或失败信息，就走失败页。
  if (status === 'FAILED' || status === 'LYRICS_FAILED' || stage === 'FAILED' || failure) {
    return 'FAILED';
  }

  if (status === 'LYRICS_GENERATING' || LYRICS_GENERATING_STAGES.has(stage)) {
    return 'LYRICS_GENERATING';
  }

  // 成品 / 交接态：作品已生成，可能可交接、已交接、链接过期或发布阻断。
  if (status === 'GENERATED' && HANDOFF_PACKAGE_STATUSES.has(pkg)) {
    return 'FINISHED';
  }

  if (status === 'GENERATING' || PRODUCTION_STAGES.has(stage) || stage === 'PACKAGE_READY') {
    return 'GENERATING';
  }

  // 其余（DRAFT / LYRICS_READY / WAITING_CONFIRM）进入歌词确认。
  return 'CONFIRM';
}

export function hasAction(work: WorkDetail, action: AvailableAction): boolean {
  return work.available_actions?.includes(action) ?? false;
}

// ---- 生成进度步骤（用于 GENERATING 页的进度条） ----
export interface ProgressStep {
  key: string;
  label: string;
  stages: GenerationStage[];
}

export const PRODUCTION_PROGRESS_STEPS: ProgressStep[] = [
  { key: 'quota', label: '准备资源', stages: ['QUOTA_LOCKING'] },
  { key: 'music', label: '谱写旋律', stages: ['MUSIC_GENERATING'] },
  { key: 'cover', label: '绘制封面', stages: ['COVER_GENERATING'] },
  {
    key: 'video',
    label: '渲染画面',
    stages: ['TIMELINE_BUILDING', 'VIDEO_RENDERING'],
  },
  {
    key: 'package',
    label: '收尾成品',
    stages: ['PACKAGE_BUILDING', 'PACKAGE_PRECHECK', 'PACKAGE_READY'],
  },
];

export type StepState = 'done' | 'active' | 'pending';

export function progressStepStates(stage: GenerationStage): StepState[] {
  const activeIndex = PRODUCTION_PROGRESS_STEPS.findIndex((step) =>
    step.stages.includes(stage),
  );
  // 找不到对应步骤时（例如刚进入生成），把第一步视为进行中。
  const resolved = activeIndex === -1 ? 0 : activeIndex;
  return PRODUCTION_PROGRESS_STEPS.map((_, idx) => {
    if (idx < resolved) return 'done';
    if (idx === resolved) return 'active';
    return 'pending';
  });
}

// ---- 普通用户友好文案（避免技术词） ----

export const STATUS_LABELS: Record<WorkStatus, string> = {
  DRAFT: '草稿',
  LYRICS_GENERATING: '歌词创作中',
  LYRICS_READY: '歌词待确认',
  LYRICS_FAILED: '歌词创作遇阻',
  GENERATING: '出歌中',
  GENERATED: '已完成',
  FAILED: '生成遇阻',
  CANCELLED: '已取消',
};

export const STAGE_LABELS: Record<GenerationStage, string> = {
  NONE: '准备中',
  USER_INPUT_PRECHECK: '校验灵感',
  LYRICS_GENERATING: '谱写歌词',
  LYRICS_PRECHECK: '润色歌词',
  WAITING_CONFIRM: '等待确认',
  QUOTA_LOCKING: '准备资源',
  MUSIC_GENERATING: '谱写旋律',
  COVER_GENERATING: '绘制封面',
  TIMELINE_BUILDING: '编排画面',
  VIDEO_RENDERING: '渲染画面',
  PACKAGE_BUILDING: '收尾成品',
  PACKAGE_PRECHECK: '成品质检',
  PACKAGE_READY: '成品就绪',
  FAILED: '遇到问题',
};

// 失败码 → 给普通用户的解释与建议（不暴露内部代码）。
export const FAILURE_COPY: Record<FailureCode, { title: string; hint: string }> = {
  USER_INPUT_BLOCKED: {
    title: '这段灵感暂时没法谱曲',
    hint: '内容可能触及了创作边界，调整一下描述再试试。',
  },
  LYRICS_GENERATION_FAILED: {
    title: '歌词没能顺利生成',
    hint: '重新生成歌词通常就能恢复。',
  },
  LYRICS_PRECHECK_FAILED: {
    title: '歌词没通过校验',
    hint: '换个表达或调整内容后重试。',
  },
  QUOTA_LOCK_FAILED: {
    title: '创作额度暂时锁定失败',
    hint: '稍等片刻再重试一次。',
  },
  MUSIC_GENERATION_FAILED: {
    title: '旋律生成遇到了问题',
    hint: '重试生成通常可以恢复，歌词会保留。',
  },
  MUSIC_QUALITY_FAILED: {
    title: '这一版旋律质量没达标',
    hint: '重试会重新谱写一版旋律。',
  },
  COVER_GENERATION_FAILED: {
    title: '封面没能画好',
    hint: '可以重新生成封面。',
  },
  VIDEO_RENDER_FAILED: {
    title: '画面渲染遇到了问题',
    hint: '可以重新渲染画面。',
  },
  PACKAGE_BUILD_FAILED: {
    title: '作品收尾时出了点问题',
    hint: '稍后重试，作品内容会保留。',
  },
  PACKAGE_BLOCKED: {
    title: '作品暂时无法发布',
    hint: '请联系平台协助处理。',
  },
  PROVIDER_AUTH_FAILED: {
    title: '创作服务暂时不可用',
    hint: '这是平台接入配置问题，请联系平台协助处理。',
  },
  PROVIDER_TIMEOUT: {
    title: '创作服务响应超时',
    hint: '网络繁忙，稍后重试一次。',
  },
  RATE_LIMITED: {
    title: '请求太频繁了',
    hint: '歇一会儿再试。',
  },
  UNKNOWN_ERROR: {
    title: '遇到了未知问题',
    hint: '可以重试，或联系平台协助。',
  },
};
