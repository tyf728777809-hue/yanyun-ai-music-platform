export type LyricLine = {
  readonly startFrame: number;
  readonly endFrame: number;
  readonly text: string;
};

export type LyricVideoProps = {
  readonly workId: string;
  readonly songTitle: string;
  readonly songSummary: string;
  readonly coverLabel: string;
  readonly durationInFrames: number;
  readonly lyrics: readonly LyricLine[];
};

export type RenderWorkerJobInput = {
  readonly work_id: string;
  readonly song_title: string;
  readonly song_summary?: string;
  readonly lyrics_text: string;
  readonly audio_object_key: string;
  readonly audio_mime_type?: string;
  readonly cover_object_key: string;
  readonly duration_ms?: number;
  readonly composition_id?: string;
};

export type RenderWorkerJobOutput = {
  readonly work_id: string;
  readonly video_file_path: string;
  readonly timeline_file_path: string;
  readonly width: number;
  readonly height: number;
  readonly fps: number;
  readonly duration_ms: number;
  readonly duration_in_frames: number;
  readonly renderer: 'remotion';
  readonly composition_id: string;
};

export const VIDEO_WIDTH = 1920;
export const VIDEO_HEIGHT = 1080;
export const VIDEO_FPS = 30;
export const DEFAULT_VIDEO_DURATION_MS = 8000;

const FALLBACK_LYRIC_LINE = '这一曲，把江湖唱给你听';
const MAX_VIDEO_DURATION_MS = 10 * 60 * 1000;

export const VIDEO_DURATION_IN_FRAMES = durationInFramesFromMs(
  DEFAULT_VIDEO_DURATION_MS,
);

export function durationInFramesFromMs(durationMs?: number): number {
  const safeDurationMs =
    typeof durationMs === 'number' && Number.isFinite(durationMs) && durationMs > 0
      ? Math.min(Math.round(durationMs), MAX_VIDEO_DURATION_MS)
      : DEFAULT_VIDEO_DURATION_MS;
  return Math.max(1, Math.ceil((safeDurationMs / 1000) * VIDEO_FPS));
}

export function durationMsFromFrames(durationInFrames: number): number {
  return Math.round((durationInFrames / VIDEO_FPS) * 1000);
}

export function lyricLinesFromText(
  lyricsText: string | undefined,
  durationInFrames: number,
): readonly LyricLine[] {
  const sourceLines = (lyricsText ?? '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0);
  const textLines =
    sourceLines.length > 0 ? sourceLines : [FALLBACK_LYRIC_LINE];
  const safeDuration = Math.max(1, durationInFrames);

  return textLines.map((text, index) => {
    const startFrame = Math.floor((safeDuration * index) / textLines.length);
    const nextStartFrame = Math.floor(
      (safeDuration * (index + 1)) / textLines.length,
    );
    return {
      startFrame,
      endFrame:
        index === textLines.length - 1
          ? safeDuration - 1
          : Math.max(startFrame, nextStartFrame - 1),
      text,
    };
  });
}

export function lyricVideoPropsFromJobInput(
  input: RenderWorkerJobInput,
): LyricVideoProps {
  const durationInFrames = durationInFramesFromMs(input.duration_ms);
  return {
    workId: input.work_id,
    songTitle: input.song_title?.trim() || '燕云新曲',
    songSummary: input.song_summary?.trim() || '一首来自燕云乐坊的原创曲。',
    coverLabel: 'YANYUN',
    durationInFrames,
    lyrics: lyricLinesFromText(input.lyrics_text, durationInFrames),
  };
}

export const sampleLyricVideoProps: LyricVideoProps = {
  workId: 'sample-work',
  songTitle: '燕云月下',
  songSummary: '一段关于边城月色、旧约与重逢的燕云歌。',
  coverLabel: 'YANYUN',
  durationInFrames: VIDEO_DURATION_IN_FRAMES,
  lyrics: [
    {
      startFrame: 0,
      endFrame: 72,
      text: '风过边城，灯火照见旧誓',
    },
    {
      startFrame: 73,
      endFrame: 144,
      text: '月落长街，故人踏雪而来',
    },
    {
      startFrame: 145,
      endFrame: 239,
      text: '这一曲，把江湖唱给你听',
    },
  ],
};
