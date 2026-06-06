import { ApiError } from '../api/client';
import type {
  ConfirmWorkRequest,
  CreateWorkResponse,
  InspirationCreateRequest,
  JobAcceptedResponse,
  LyricsContinueRequest,
  LyricsCreateRequest,
  LyricsPolishRequest,
  PublishPackage,
  RetryMusicRequest,
  WorkDetail,
  WorkListResponse,
  WorkStatus,
} from '../api/types';
import { makeCoverDataUri, makeToneWavDataUri } from './media';

// 内置「演示模式」后端：纯前端内存模拟，忠实复刻后端 v0.1 的状态机与 JSON 契约，
// 不发起任何网络请求，也不调用真实 DeepSeek / Suno / MiniMax / Image 2。
// 目的：在没有本地 Java/Postgres/Temporal 时也能完整演示与验收创作链路。

const POLISH_LIMIT = 2; // 与后端 WorkService.POLISH_LIMIT 一致
const MUSIC_RETRY_LIMIT = 2; // 与后端 MUSIC_RETRY_LIMIT 一致

// 演示用的阶段时长（毫秒）——比真实链路快很多，便于观察轮询推进。
const LYRICS_MS = 2200;
const PRODUCTION_STEPS: { stage: WorkDetail['generation_stage']; ms: number }[] = [
  { stage: 'QUOTA_LOCKING', ms: 900 },
  { stage: 'MUSIC_GENERATING', ms: 2600 },
  { stage: 'COVER_GENERATING', ms: 1800 },
  { stage: 'TIMELINE_BUILDING', ms: 1100 },
  { stage: 'VIDEO_RENDERING', ms: 2200 },
  { stage: 'PACKAGE_BUILDING', ms: 1500 },
  { stage: 'PACKAGE_PRECHECK', ms: 900 },
];

interface MockWork {
  workId: string;
  workCode: string;
  creationMode: 'INSPIRATION' | 'LYRICS';
  createdAt: number;
  songTitle: string;
  songSummary: string;
  lyricsText: string;
  musicPrompt: string;
  yanyunReferences: string[];
  riskNotes: string[];
  lyricsVersion: number;
  polishUsed: number;
  musicRetryUsed: number;
  // 调度时间戳
  lyricsStartedAt: number;
  productionStartedAt: number | null;
  // 失败模拟：输入里带「失败/fail」时，首次出歌在旋律阶段失败一次。
  shouldFailMusicOnce: boolean;
  musicFailedConsumed: boolean;
  packageFetched: boolean;
  urlRefreshedAt: number | null;
}

const store = new Map<string, MockWork>();

function uuid(): string {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = (Math.random() * 16) | 0;
        return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
      });
}

// 演示用占位歌词：保留燕云山水气质，不调用任何模型。
function draftLyrics(theme: string): string {
  const t = theme.trim().slice(0, 12) || '远山长';
  return [
    '[主歌]',
    `提灯夜行过${t}，风过檐角铃声轻`,
    '一程烟雨一程山，旧约还在心头停',
    '',
    '[副歌]',
    '燕云十六声，声声唤归人',
    '若问此情何处寄，且看月落又月生',
    '',
    '[尾声]',
    '长歌一曲付清风，来路归途两从容',
  ].join('\n');
}

function makeWork(
  mode: 'INSPIRATION' | 'LYRICS',
  seedText: string,
  title: string | undefined,
  shouldFail: boolean,
): MockWork {
  const id = uuid();
  const now = Date.now();
  return {
    workId: id,
    workCode: `YY-${id.slice(0, 8).toUpperCase()}`,
    creationMode: mode,
    createdAt: now,
    songTitle: title?.trim() || '燕云未名曲',
    songSummary: '一首关于山河与故人的歌，循着燕云十六声里的烟雨与归途。',
    lyricsText: draftLyrics(seedText),
    musicPrompt:
      'Chinese cinematic folk, guzheng and dizi lead, soft female vocal, misty mountains mood, 80 BPM, pentatonic, gentle reverb',
    yanyunReferences: ['清河镇 · 夜雨长街', '燕云十六声 · 提灯夜行', '远山霁色 · 归人渡口'],
    riskNotes: [],
    lyricsVersion: 1,
    polishUsed: 0,
    musicRetryUsed: 0,
    lyricsStartedAt: now,
    productionStartedAt: null,
    shouldFailMusicOnce: shouldFail,
    musicFailedConsumed: false,
    packageFetched: false,
    urlRefreshedAt: null,
  };
}

function detectFailIntent(text: string): boolean {
  return /失败|fail|出错|报错/i.test(text);
}

// 根据「调度时间戳 + 当前时间」推导作品状态，模拟后端异步推进 + 前端轮询的真实节奏。
interface Derived {
  status: WorkDetail['status'];
  stage: WorkDetail['generation_stage'];
  packageStatus: WorkDetail['package_status'];
  failed: boolean;
}

function derive(w: MockWork, now: number): Derived {
  // 1) 歌词生成阶段
  if (w.lyricsStartedAt && now - w.lyricsStartedAt < LYRICS_MS) {
    return {
      status: 'LYRICS_GENERATING',
      stage: 'LYRICS_GENERATING',
      packageStatus: 'PACKAGE_NOT_READY',
      failed: false,
    };
  }

  // 2) 还没确认出歌 → 歌词就绪待确认
  if (w.productionStartedAt === null) {
    return {
      status: 'LYRICS_READY',
      stage: 'WAITING_CONFIRM',
      packageStatus: 'PACKAGE_NOT_READY',
      failed: false,
    };
  }

  // 3) 出歌生产阶段：按累计时长推进
  const elapsed = now - w.productionStartedAt;

  // 失败模拟：首次出歌在旋律阶段 60% 处失败，并“粘滞”在失败，直到用户重试。
  if (w.shouldFailMusicOnce && !w.musicFailedConsumed) {
    const failAt = PRODUCTION_STEPS[0].ms + PRODUCTION_STEPS[1].ms * 0.6;
    if (elapsed >= failAt) {
      return {
        status: 'FAILED',
        stage: 'FAILED',
        packageStatus: 'PACKAGE_NOT_READY',
        failed: true,
      };
    }
  }

  let acc = 0;
  for (const step of PRODUCTION_STEPS) {
    acc += step.ms;
    if (elapsed < acc) {
      return {
        status: 'GENERATING',
        stage: step.stage,
        packageStatus: 'PACKAGE_NOT_READY',
        failed: false,
      };
    }
  }

  // 4) 成品就绪
  return {
    status: 'GENERATED',
    stage: 'PACKAGE_READY',
    packageStatus: w.packageFetched ? 'PACKAGE_FETCHED' : 'PACKAGE_READY',
    failed: false,
  };
}

function availableActions(w: MockWork, d: Derived): WorkDetail['available_actions'] {
  const actions: WorkDetail['available_actions'] = [];
  if (d.stage === 'WAITING_CONFIRM' && d.status === 'LYRICS_READY') {
    actions.push('POLISH_LYRICS', 'CONTINUE_LYRICS', 'CONFIRM_WORK');
  }
  if (d.failed) {
    // 演示中失败发生在旋律阶段：可重试音乐或返回编辑。
    if (w.musicRetryUsed < MUSIC_RETRY_LIMIT) actions.push('RETRY_MUSIC');
    actions.push('RETURN_TO_EDIT');
    actions.push('CONTACT_SUPPORT');
  }
  if (d.status === 'GENERATED') {
    actions.push('RERENDER_VIDEO');
    actions.push('REFRESH_PACKAGE_URL');
    if (!w.packageFetched) actions.push('MARK_PACKAGE_FETCHED');
  }
  return actions;
}

function lyricsReady(w: MockWork, now: number): boolean {
  return now - w.lyricsStartedAt >= LYRICS_MS;
}

function packageUrl(w: MockWork): string {
  const stamp = w.urlRefreshedAt ?? w.createdAt;
  // 演示用的“带签名过期参数”的占位下载地址，不指向真实存储。
  return `https://demo.local/yanyun/${w.workId}/publish-package.zip?sig=demo-${stamp.toString(36)}&expires=${stamp + 3600_000}`;
}

function toDetail(w: MockWork): WorkDetail {
  const now = Date.now();
  const d = derive(w, now);
  const ready = d.status === 'GENERATED';
  const cover = makeCoverDataUri(w.workId);
  const audio = makeToneWavDataUri(w.workId);
  return {
    work_id: w.workId,
    work_code: w.workCode,
    creation_mode: w.creationMode,
    status: d.status,
    generation_stage: d.stage,
    package_status: d.packageStatus,
    song_title: w.songTitle,
    song_summary: w.songSummary,
    lyrics_draft: lyricsReady(w, now)
      ? {
          lyrics_draft_id: `${w.workId}-v${w.lyricsVersion}`,
          version_no: w.lyricsVersion,
          song_title: w.songTitle,
          song_summary: w.songSummary,
          lyrics_text: w.lyricsText,
          music_prompt: w.musicPrompt,
          risk_notes: w.riskNotes,
          yanyun_references: w.yanyunReferences,
        }
      : null,
    media_assets: ready
      ? {
          audio_url: audio,
          cover_url: cover,
          video_url: audio, // 演示中用同一段音频占位视频源
          video_duration_ms: 3000,
          video_file_size_bytes: 1_280_000,
        }
      : { audio_url: null, cover_url: null, video_url: null },
    polish_used_count: w.polishUsed,
    polish_remaining_count: Math.max(0, POLISH_LIMIT - w.polishUsed),
    quota_hint: {
      locked: w.productionStartedAt !== null,
      commit_timing: 'ON_PACKAGE_READY',
      remaining_generate_count: 3,
      remaining_polish_count: Math.max(0, POLISH_LIMIT - w.polishUsed),
      message: '出歌额度将在成品就绪时结算',
    },
    failure: d.failed
      ? {
          failure_code: 'MUSIC_GENERATION_FAILED',
          failure_message: '旋律生成未成功，可重试一次',
          retryable: w.musicRetryUsed < MUSIC_RETRY_LIMIT,
          failed_at: new Date(now).toISOString(),
          retry_count: w.musicRetryUsed,
          retry_limit: MUSIC_RETRY_LIMIT,
          remaining_retry_count: Math.max(0, MUSIC_RETRY_LIMIT - w.musicRetryUsed),
          recommended_action: w.musicRetryUsed < MUSIC_RETRY_LIMIT ? 'RETRY_MUSIC' : 'CONTACT_SUPPORT',
        }
      : null,
    available_actions: availableActions(w, d),
    publish_handoff_hint: ready
      ? {
          ready_for_handoff: !w.packageFetched,
          message: w.packageFetched
            ? '作品已交接给社区发布流程'
            : '作品已准备好，可交给社区发布',
        }
      : null,
    created_at: new Date(w.createdAt).toISOString(),
    updated_at: new Date(now).toISOString(),
    generated_at: ready ? new Date(now).toISOString() : null,
  };
}

function toSummary(w: MockWork): WorkListResponse['items'][number] {
  const detail = toDetail(w);
  return {
    work_id: detail.work_id,
    work_code: detail.work_code,
    song_title: detail.song_title,
    status: detail.status,
    generation_stage: detail.generation_stage,
    package_status: detail.package_status,
    cover_url: detail.media_assets?.cover_url ?? null,
    video_preview_url: detail.media_assets?.video_url ?? null,
    updated_at: detail.updated_at,
  };
}

function toPublishPackage(w: MockWork): PublishPackage {
  const now = Date.now();
  const d = derive(w, now);
  if (d.status !== 'GENERATED') {
    return {
      work_id: w.workId,
      package_status: 'PACKAGE_NOT_READY',
      available_actions: [],
      blocked_reason: null,
    };
  }
  const cover = makeCoverDataUri(w.workId);
  const audio = makeToneWavDataUri(w.workId);
  return {
    work_id: w.workId,
    package_status: w.packageFetched ? 'PACKAGE_FETCHED' : 'PACKAGE_READY',
    package_url: packageUrl(w),
    package_url_expires_at: new Date((w.urlRefreshedAt ?? now) + 3600_000).toISOString(),
    package_json: {
      work_id: w.workId,
      audio: { url: audio, mime_type: 'audio/wav', file_size_bytes: 64_000, checksum: 'demo' },
      video: { url: audio, mime_type: 'video/mp4', file_size_bytes: 1_280_000, checksum: 'demo' },
      cover: { url: cover, mime_type: 'image/svg+xml', file_size_bytes: 4_096, checksum: 'demo' },
      lyrics: { text: w.lyricsText, timeline_url: `https://demo.local/${w.workId}/timeline.json` },
      metadata: {
        song_title: w.songTitle,
        song_summary: w.songSummary,
        source: 'demo-mode',
      },
    },
    available_actions: w.packageFetched
      ? ['REFRESH_PACKAGE_URL']
      : ['REFRESH_PACKAGE_URL', 'MARK_PACKAGE_FETCHED'],
    blocked_reason: null,
  };
}

function requireWork(workId: string): MockWork {
  const w = store.get(workId);
  if (!w) {
    throw new ApiError(404, 'NOT_FOUND', '没有找到这个作品');
  }
  return w;
}

// ---- 对外的演示后端接口（与 src/api/works.ts 签名一致） ----
export const mockBackend = {
  async createFromInspiration(body: InspirationCreateRequest): Promise<CreateWorkResponse> {
    const w = makeWork('INSPIRATION', body.story_input ?? '', undefined, detectFailIntent(body.story_input ?? ''));
    if (body.music_style) w.musicPrompt = `${body.music_style}. ${w.musicPrompt}`;
    store.set(w.workId, w);
    return {
      work_id: w.workId,
      work_code: w.workCode,
      status: 'LYRICS_GENERATING',
      generation_stage: 'LYRICS_GENERATING',
      job_id: uuid(),
      quota_hint: toDetail(w).quota_hint,
      available_actions: [],
    };
  },

  async createFromLyrics(body: LyricsCreateRequest): Promise<CreateWorkResponse> {
    const w = makeWork('LYRICS', body.lyrics_input ?? '', body.song_title, detectFailIntent(body.lyrics_input ?? ''));
    if (body.lyrics_input?.trim()) {
      w.lyricsText = body.lyrics_input.trim();
    }
    if (body.music_style) w.musicPrompt = `${body.music_style}. ${w.musicPrompt}`;
    store.set(w.workId, w);
    return {
      work_id: w.workId,
      work_code: w.workCode,
      status: 'LYRICS_GENERATING',
      generation_stage: 'LYRICS_GENERATING',
      job_id: uuid(),
      quota_hint: toDetail(w).quota_hint,
      available_actions: [],
    };
  },

  async listWorks(
    params: { page?: number; page_size?: number; status?: WorkStatus } = {},
  ): Promise<WorkListResponse> {
    const page = Math.max(1, params.page ?? 1);
    const pageSize = Math.min(50, Math.max(1, params.page_size ?? 20));
    const all = [...store.values()]
      .map(toSummary)
      .filter((item) => (params.status ? item.status === params.status : true))
      .sort((a, b) => Date.parse(b.updated_at) - Date.parse(a.updated_at));
    const start = (page - 1) * pageSize;
    const items = all.slice(start, start + pageSize);
    return {
      items,
      pagination: {
        page,
        page_size: pageSize,
        total: all.length,
        has_more: start + pageSize < all.length,
      },
    };
  },

  async getWork(workId: string): Promise<WorkDetail> {
    return toDetail(requireWork(workId));
  },

  async polishLyrics(workId: string, body: LyricsPolishRequest): Promise<JobAcceptedResponse> {
    const w = requireWork(workId);
    const instruction = body.instruction.trim();
    if (!instruction) {
      throw new ApiError(400, 'VALIDATION_ERROR', '润色说明不能为空');
    }
    if (!lyricsReady(w, Date.now()) || w.productionStartedAt !== null) {
      throw new ApiError(409, 'CONFLICT', '当前状态不能润色歌词');
    }
    if (w.polishUsed >= POLISH_LIMIT) {
      throw new ApiError(409, 'CONFLICT', 'No remaining polish attempts');
    }
    w.polishUsed += 1;
    w.lyricsVersion += 1;
    w.lyricsText = `${w.lyricsText}\n\n[已润色 · 第${w.polishUsed}次 · ${instruction}]`;
    const d = derive(w, Date.now());
    return jobAccepted(w, d);
  },

  async continueLyrics(workId: string, body: LyricsContinueRequest): Promise<JobAcceptedResponse> {
    const w = requireWork(workId);
    if (!lyricsReady(w, Date.now()) || w.productionStartedAt !== null) {
      throw new ApiError(409, 'CONFLICT', '当前状态不能续写歌词');
    }
    if (w.polishUsed >= POLISH_LIMIT) {
      throw new ApiError(409, 'CONFLICT', 'No remaining AI edit attempts');
    }
    w.polishUsed += 1;
    w.lyricsVersion += 1;
    const note = body.instruction?.trim();
    w.lyricsText = `${w.lyricsText}\n\n[续写 · 第${w.polishUsed}次]\n再行一程山高水又长，灯火不灭照归航${note ? '（' + note + '）' : ''}`;
    const d = derive(w, Date.now());
    return jobAccepted(w, d);
  },

  async confirmWork(workId: string, _body: ConfirmWorkRequest): Promise<JobAcceptedResponse> {
    void _body;
    const w = requireWork(workId);
    if (!lyricsReady(w, Date.now())) {
      throw new ApiError(409, 'CONFLICT', '歌词还在生成中，请稍候');
    }
    w.productionStartedAt = Date.now();
    const d = derive(w, Date.now());
    return jobAccepted(w, d);
  },

  async retryMusic(workId: string, _body: RetryMusicRequest): Promise<JobAcceptedResponse> {
    void _body;
    const w = requireWork(workId);
    if (w.musicRetryUsed >= MUSIC_RETRY_LIMIT) {
      throw new ApiError(409, 'CONFLICT', '已达到重试上限');
    }
    w.musicRetryUsed += 1;
    w.musicFailedConsumed = true; // 重试后不再失败
    w.productionStartedAt = Date.now(); // 重新跑生产链路
    const d = derive(w, Date.now());
    return jobAccepted(w, d);
  },

  async regenerateCover(workId: string): Promise<JobAcceptedResponse> {
    const w = requireWork(workId);
    const d = derive(w, Date.now());
    if (d.status !== 'GENERATED') {
      throw new ApiError(409, 'CONFLICT', '当前状态不能重新生成封面');
    }
    return jobAccepted(w, d);
  },

  async rerenderVideo(workId: string): Promise<JobAcceptedResponse> {
    const w = requireWork(workId);
    const d = derive(w, Date.now());
    if (d.status !== 'GENERATED') {
      throw new ApiError(409, 'CONFLICT', '当前状态不能重新渲染画面');
    }
    return jobAccepted(w, d);
  },

  async getPublishPackage(workId: string): Promise<PublishPackage> {
    return toPublishPackage(requireWork(workId));
  },

  async markPublishPackageFetched(workId: string): Promise<PublishPackage> {
    const w = requireWork(workId);
    w.packageFetched = true;
    return toPublishPackage(w);
  },

  async refreshPublishPackageUrl(workId: string): Promise<PublishPackage> {
    const w = requireWork(workId);
    w.urlRefreshedAt = Date.now();
    return toPublishPackage(w);
  },
};

function jobAccepted(w: MockWork, d: Derived): JobAcceptedResponse {
  return {
    work_id: w.workId,
    status: d.status,
    generation_stage: d.stage,
    job_id: uuid(),
    available_actions: availableActions(w, d),
  };
}
