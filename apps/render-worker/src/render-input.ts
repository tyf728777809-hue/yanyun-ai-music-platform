import {videoLyricLayout, videoSafeArea} from './video-style';

export type LyricLine = {
  readonly startFrame: number;
  readonly endFrame: number;
  readonly text: string;
};

export type LyricVideoTemplateId =
  | 'lyric-video-16x9-v1'
  | 'lyric-video-16x9-v2'
  | 'lyric-video-16x9-v3';

export type LyricVideoSafeArea = {
  readonly left: number;
  readonly right: number;
  readonly top: number;
  readonly bottom: number;
};

export type LyricVideoLyricLayout = {
  readonly maxCharsPerLine: number;
  readonly maxLines: number;
};

export type LyricVideoProps = {
  readonly workId: string;
  readonly songTitle: string;
  readonly songSummary: string;
  readonly coverLabel: string;
  readonly audioSource: string;
  readonly coverSource: string;
  readonly templateId: LyricVideoTemplateId;
  readonly safeArea: LyricVideoSafeArea;
  readonly lyricLayout: LyricVideoLyricLayout;
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
  readonly audio_source_path?: string;
  readonly cover_source_path?: string;
  readonly template_id?: string;
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
    .filter((line) => line.length > 0 && !isLyricSectionLabel(line));
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

function isLyricSectionLabel(line: string): boolean {
  return /^(主歌|副歌|桥段|间奏|尾声|verse|chorus|bridge)\s*\d*\s*[:：]$/i.test(
    line.trim(),
  );
}

export function wrapLyricText(
  text: string,
  maxCharsPerLine = videoLyricLayout.maxCharsPerLine,
  maxLines = videoLyricLayout.maxLines,
): readonly string[] {
  const normalized = text.trim().replace(/\s+/g, ' ');
  if (normalized.length === 0) {
    return [''];
  }

  const safeMaxCharsPerLine = Math.max(4, Math.floor(maxCharsPerLine));
  const safeMaxLines = Math.max(1, Math.floor(maxLines));
  const characters = Array.from(normalized);
  const lines: string[] = [];
  let currentLine = '';

  for (const character of characters) {
    if (currentLine.length >= safeMaxCharsPerLine) {
      lines.push(currentLine);
      currentLine = '';
    }
    currentLine += character;
  }

  if (currentLine.length > 0) {
    lines.push(currentLine);
  }

  if (lines.length <= safeMaxLines) {
    return lines;
  }

  const visibleLines = lines.slice(0, safeMaxLines);
  const lastLineIndex = visibleLines.length - 1;
  const lastLine = visibleLines[lastLineIndex] ?? '';
  visibleLines[lastLineIndex] =
    lastLine.length >= safeMaxCharsPerLine
      ? `${lastLine.slice(0, safeMaxCharsPerLine - 3)}...`
      : `${lastLine}...`;
  return visibleLines;
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
    audioSource: input.audio_source_path?.trim() || input.audio_object_key,
    coverSource: input.cover_source_path?.trim() || input.cover_object_key,
    templateId: lyricVideoTemplateIdFromInput(input.template_id),
    safeArea: videoSafeArea,
    lyricLayout: videoLyricLayout,
    durationInFrames,
    lyrics: lyricLinesFromText(input.lyrics_text, durationInFrames),
  };
}

export const sampleLyricVideoProps: LyricVideoProps = {
  workId: 'sample-work',
  songTitle: '燕云月下',
  songSummary: '一段关于边城月色、旧约与重逢的燕云歌。',
  coverLabel: 'YANYUN',
  audioSource: sampleSilentWavDataUri(),
  coverSource: sampleCoverDataUri(),
  templateId: 'lyric-video-16x9-v2',
  safeArea: videoSafeArea,
  lyricLayout: videoLyricLayout,
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

function lyricVideoTemplateIdFromInput(
  templateId: string | undefined,
): LyricVideoTemplateId {
  if (templateId === 'lyric-video-16x9-v1') {
    return 'lyric-video-16x9-v1';
  }
  if (templateId === 'lyric-video-16x9-v3') {
    return 'lyric-video-16x9-v3';
  }
  return 'lyric-video-16x9-v2';
}

function sampleCoverDataUri(): string {
  const svg = [
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 1200">',
    '<defs>',
    '<radialGradient id="moon" cx="36%" cy="30%" r="58%">',
    '<stop offset="0" stop-color="#f6eddc"/>',
    '<stop offset="0.28" stop-color="#c7a45a"/>',
    '<stop offset="0.66" stop-color="#23342f"/>',
    '<stop offset="1" stop-color="#0b1110"/>',
    '</radialGradient>',
    '<linearGradient id="seal" x1="0" x2="1" y1="0" y2="1">',
    '<stop offset="0" stop-color="#9f3d2e"/>',
    '<stop offset="1" stop-color="#5f241c"/>',
    '</linearGradient>',
    '</defs>',
    '<rect width="1200" height="1200" fill="url(#moon)"/>',
    '<circle cx="380" cy="340" r="138" fill="rgba(246,237,220,0.52)"/>',
    '<path d="M140 792 C326 650 468 706 610 602 C748 500 904 478 1060 352 L1060 1200 L140 1200 Z" fill="rgba(11,17,16,0.58)"/>',
    '<path d="M146 854 C318 738 498 770 688 676 C806 618 944 598 1064 532" fill="none" stroke="#c7a45a" stroke-width="8" opacity="0.62"/>',
    '<rect x="830" y="832" width="124" height="124" rx="8" fill="url(#seal)" opacity="0.92"/>',
    '<text x="600" y="642" text-anchor="middle" font-size="92" font-weight="700" font-family="Arial, sans-serif" fill="#f6eddc" opacity="0.9">YANYUN</text>',
    '</svg>',
  ].join('');
  return `data:image/svg+xml;base64,${base64FromBytes(new TextEncoder().encode(svg))}`;
}

function sampleSilentWavDataUri(): string {
  const sampleRate = 8000;
  const seconds = 1;
  const bytesPerSample = 2;
  const sampleCount = sampleRate * seconds;
  const dataSize = sampleCount * bytesPerSample;
  const buffer = new ArrayBuffer(44 + dataSize);
  const view = new DataView(buffer);

  writeAscii(view, 0, 'RIFF');
  view.setUint32(4, 36 + dataSize, true);
  writeAscii(view, 8, 'WAVE');
  writeAscii(view, 12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, 1, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * bytesPerSample, true);
  view.setUint16(32, bytesPerSample, true);
  view.setUint16(34, 16, true);
  writeAscii(view, 36, 'data');
  view.setUint32(40, dataSize, true);

  return `data:audio/wav;base64,${base64FromBytes(new Uint8Array(buffer))}`;
}

function writeAscii(view: DataView, offset: number, value: string): void {
  for (let index = 0; index < value.length; index += 1) {
    view.setUint8(offset + index, value.charCodeAt(index));
  }
}

function base64FromBytes(bytes: Uint8Array): string {
  let binary = '';
  const chunkSize = 8192;
  for (let index = 0; index < bytes.length; index += chunkSize) {
    binary += String.fromCharCode(...bytes.subarray(index, index + chunkSize));
  }
  return globalThis.btoa(binary);
}
