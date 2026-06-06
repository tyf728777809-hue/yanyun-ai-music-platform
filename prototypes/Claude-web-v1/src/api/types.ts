// 与后端 OpenAPI v0.1 对齐的领域类型。
// 后端 Jackson 使用 SNAKE_CASE 序列化，枚举为大写常量字符串，null 字段会被省略。
// 这些字符串联合类型直接来自 com.yanyun.music.workdomain 下的枚举定义，
// 前端只“读取并展示”后端状态，绝不自行猜测状态机。

export type WorkStatus =
  | 'DRAFT'
  | 'LYRICS_GENERATING'
  | 'LYRICS_READY'
  | 'LYRICS_FAILED'
  | 'GENERATING'
  | 'GENERATED'
  | 'FAILED'
  | 'CANCELLED';

export type GenerationStage =
  | 'NONE'
  | 'USER_INPUT_PRECHECK'
  | 'LYRICS_GENERATING'
  | 'LYRICS_PRECHECK'
  | 'WAITING_CONFIRM'
  | 'QUOTA_LOCKING'
  | 'MUSIC_GENERATING'
  | 'COVER_GENERATING'
  | 'TIMELINE_BUILDING'
  | 'VIDEO_RENDERING'
  | 'PACKAGE_BUILDING'
  | 'PACKAGE_PRECHECK'
  | 'PACKAGE_READY'
  | 'FAILED';

export type PackageStatus =
  | 'PACKAGE_NOT_READY'
  | 'PACKAGE_READY'
  | 'PACKAGE_FETCHED'
  | 'PACKAGE_EXPIRED'
  | 'PACKAGE_BLOCKED';

export type AvailableAction =
  | 'POLISH_LYRICS'
  | 'CONTINUE_LYRICS'
  | 'CONFIRM_WORK'
  | 'RETRY_LYRICS'
  | 'RETRY_MUSIC'
  | 'RETRY_COVER'
  | 'RERENDER_VIDEO'
  | 'REFRESH_PACKAGE_URL'
  | 'MARK_PACKAGE_FETCHED'
  | 'RETURN_TO_EDIT'
  | 'CONTACT_SUPPORT';

export type FailureCode =
  | 'USER_INPUT_BLOCKED'
  | 'LYRICS_GENERATION_FAILED'
  | 'LYRICS_PRECHECK_FAILED'
  | 'QUOTA_LOCK_FAILED'
  | 'MUSIC_GENERATION_FAILED'
  | 'MUSIC_QUALITY_FAILED'
  | 'COVER_GENERATION_FAILED'
  | 'VIDEO_RENDER_FAILED'
  | 'PACKAGE_BUILD_FAILED'
  | 'PACKAGE_BLOCKED'
  | 'PROVIDER_TIMEOUT'
  | 'RATE_LIMITED'
  | 'UNKNOWN_ERROR';

export type CreationMode = 'INSPIRATION' | 'LYRICS';

export type VocalPreference = 'AUTO' | 'MALE' | 'FEMALE' | 'CHORUS';

export interface QuotaHint {
  locked: boolean;
  commit_timing: string;
  remaining_generate_count: number;
  remaining_polish_count: number;
  message?: string | null;
}

export interface LyricsDraft {
  lyrics_draft_id: string;
  version_no: number;
  song_title: string;
  song_summary: string;
  lyrics_text: string;
  music_prompt: string;
  risk_notes?: string[] | null;
  yanyun_references?: string[] | null;
}

export interface MediaAssets {
  audio_url?: string | null;
  cover_url?: string | null;
  video_url?: string | null;
  video_duration_ms?: number | null;
  video_file_size_bytes?: number | null;
}

export interface FailureInfo {
  failure_code: FailureCode;
  failure_message: string;
  retryable: boolean;
  failed_at?: string | null;
  retry_count?: number | null;
  retry_limit?: number | null;
  remaining_retry_count?: number | null;
  recommended_action?: AvailableAction | null;
}

export interface PublishHandoffHint {
  ready_for_handoff: boolean;
  message?: string | null;
}

export interface WorkDetail {
  work_id: string;
  work_code: string;
  creation_mode: CreationMode;
  status: WorkStatus;
  generation_stage: GenerationStage;
  package_status: PackageStatus;
  song_title?: string | null;
  song_summary?: string | null;
  lyrics_draft?: LyricsDraft | null;
  media_assets?: MediaAssets | null;
  polish_used_count: number;
  polish_remaining_count: number;
  quota_hint?: QuotaHint | null;
  failure?: FailureInfo | null;
  available_actions: AvailableAction[];
  publish_handoff_hint?: PublishHandoffHint | null;
  created_at: string;
  updated_at: string;
  generated_at?: string | null;
}

export interface Pagination {
  page: number;
  page_size: number;
  total: number;
  has_more: boolean;
}

export interface WorkSummary {
  work_id: string;
  work_code: string;
  song_title?: string | null;
  status: WorkStatus;
  generation_stage: GenerationStage;
  package_status: PackageStatus;
  cover_url?: string | null;
  video_preview_url?: string | null;
  updated_at: string;
}

export interface WorkListResponse {
  items: WorkSummary[];
  pagination: Pagination;
}

export interface CreateWorkResponse {
  work_id: string;
  work_code: string;
  status: WorkStatus;
  generation_stage: GenerationStage;
  job_id?: string | null;
  quota_hint?: QuotaHint | null;
  available_actions: AvailableAction[];
}

export interface JobAcceptedResponse {
  work_id: string;
  status: WorkStatus;
  generation_stage: GenerationStage;
  job_id?: string | null;
  available_actions: AvailableAction[];
}

export interface PublishAsset {
  url: string;
  mime_type: string;
  file_size_bytes?: number | null;
  checksum?: string | null;
}

export interface PublishPackageJson {
  work_id: string;
  audio?: PublishAsset | null;
  video: PublishAsset;
  cover: PublishAsset;
  lyrics: {
    text: string;
    timeline_url?: string | null;
  };
  metadata: {
    song_title?: string | null;
    song_summary?: string | null;
    source?: string | null;
    [key: string]: unknown;
  };
}

export interface PublishPackage {
  work_id: string;
  package_status: PackageStatus;
  package_url?: string | null;
  package_url_expires_at?: string | null;
  package_json?: PublishPackageJson | null;
  available_actions: AvailableAction[];
  blocked_reason?: string | null;
}

// 请求体（注意：后端读取 snake_case 字段）
export interface InspirationCreateRequest {
  story_input: string;
  mood?: string;
  scene?: string;
  relationship?: string;
  music_style?: string;
  vocal_preference?: VocalPreference;
}

export interface LyricsCreateRequest {
  lyrics_input: string;
  song_title?: string;
  music_style?: string;
  vocal_preference?: VocalPreference;
}

export interface LyricsPolishRequest {
  instruction: string;
}

export interface LyricsContinueRequest {
  instruction?: string;
}

export interface ConfirmWorkRequest {
  lyrics_draft_id?: string;
  user_confirmed_at?: string;
  music_provider?: string;
}

export interface RetryMusicRequest {
  music_provider?: string;
}

// 后端统一错误信封：{ "error": { code, message, details, request_id, timestamp } }
export interface ApiErrorBody {
  error?: {
    code?: string;
    message?: string;
    details?: Array<{ path?: string }>;
    request_id?: string;
    timestamp?: string;
  };
}
