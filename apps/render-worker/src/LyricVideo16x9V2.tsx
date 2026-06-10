import {
  AbsoluteFill,
  Audio,
  Img,
  interpolate,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from 'remotion';
import type {LyricLine, LyricVideoProps} from './render-input';
import {wrapLyricText} from './render-input';
import {videoColors, videoTypography} from './video-style';

function activeLine(lines: readonly LyricLine[], frame: number): LyricLine {
  return (
    lines.find((line) => frame >= line.startFrame && frame <= line.endFrame) ??
    lines[lines.length - 1] ?? {startFrame: 0, endFrame: 1, text: ''}
  );
}

export function toRemotionMediaSource(source: string): string {
  const trimmedSource = source.trim();
  if (trimmedSource.startsWith('static://')) {
    return staticFile(trimmedSource.slice('static://'.length));
  }

  if (
    trimmedSource.startsWith('http://') ||
    trimmedSource.startsWith('https://') ||
    trimmedSource.startsWith('file://') ||
    trimmedSource.startsWith('blob:') ||
    trimmedSource.startsWith('data:')
  ) {
    return trimmedSource;
  }

  if (trimmedSource.startsWith('/')) {
    return `file://${encodeFilePath(trimmedSource)}`;
  }

  if (/^[a-zA-Z]:[\\/]/.test(trimmedSource)) {
    return `file:///${encodeFilePath(trimmedSource.replace(/\\/g, '/'))}`;
  }

  return trimmedSource;
}

function encodeFilePath(filePath: string): string {
  return filePath
    .split('/')
    .map((segment, index) =>
      index === 0 && /^[a-zA-Z]:$/.test(segment)
        ? segment
        : encodeURIComponent(segment),
    )
    .join('/');
}

export function LyricVideo16x9V2(props: LyricVideoProps) {
  const frame = useCurrentFrame();
  const {durationInFrames} = useVideoConfig();
  const line = activeLine(props.lyrics, frame);
  const lyricParts = wrapLyricText(
    line.text,
    props.lyricLayout.maxCharsPerLine,
    props.lyricLayout.maxLines,
  );
  const audioSource = toRemotionMediaSource(props.audioSource);
  const coverSource = toRemotionMediaSource(props.coverSource);
  const safeArea = props.safeArea;
  const lineFrameCount = Math.max(1, line.endFrame - line.startFrame + 1);
  const fadeFrames = Math.min(16, Math.floor(lineFrameCount / 3));
  const lineOpacity =
    lineFrameCount >= 4
      ? interpolate(
          frame,
          [
            line.startFrame,
            line.startFrame + fadeFrames,
            line.endFrame - fadeFrames,
            line.endFrame,
          ],
          [0.18, 1, 1, 0.18],
          {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'},
        )
      : 1;
  const introOpacity = interpolate(frame, [0, 24, 72], [0, 1, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });
  const progress = Math.min(1, frame / Math.max(1, durationInFrames - 1));
  const backgroundScale = interpolate(
    frame,
    [0, durationInFrames],
    [1.1, 1.15],
    {
      extrapolateLeft: 'clamp',
      extrapolateRight: 'clamp',
    },
  );
  const coverScale = interpolate(frame, [0, durationInFrames], [1, 1.025], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  return (
    <AbsoluteFill
      style={{
        backgroundColor: videoColors.ink,
        color: videoColors.ivory,
        fontFamily:
          '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
        overflow: 'hidden',
      }}
    >
      <Audio src={audioSource} />
      <Img
        src={coverSource}
        style={{
          filter: 'blur(26px) saturate(1.1)',
          height: 1200,
          inset: -60,
          objectFit: 'cover',
          opacity: 0.52,
          position: 'absolute',
          transform: `scale(${backgroundScale})`,
          width: 2040,
        }}
      />
      <AbsoluteFill
        style={{
          background:
            'linear-gradient(90deg, rgba(11,17,16,0.92), rgba(11,17,16,0.48) 48%, rgba(11,17,16,0.78))',
        }}
      />
      <AbsoluteFill
        style={{
          background:
            'linear-gradient(0deg, rgba(11,17,16,0.9), rgba(11,17,16,0.12) 46%, rgba(11,17,16,0.54))',
        }}
      />

      <div
        style={{
          color: videoColors.gold,
          fontSize: videoTypography.meta,
          fontWeight: 650,
          left: safeArea.left,
          letterSpacing: 0,
          position: 'absolute',
          top: safeArea.top,
        }}
      >
        燕云乐坊
      </div>

      <Img
        src={coverSource}
        style={{
          border: '1px solid rgba(246, 237, 220, 0.18)',
          boxShadow: `0 42px 120px ${videoColors.shadow}`,
          height: 590,
          left: safeArea.left,
          objectFit: 'cover',
          position: 'absolute',
          top: 214,
          transform: `scale(${coverScale})`,
          width: 590,
        }}
      />

      <div
        style={{
          left: 770,
          opacity: introOpacity,
          position: 'absolute',
          top: 238,
          width: 880,
        }}
      >
        <div
          style={{
            fontSize: videoTypography.title,
            fontWeight: 750,
            letterSpacing: 0,
            lineHeight: 1.04,
            maxWidth: 840,
          }}
        >
          {props.songTitle}
        </div>
        <div
          style={{
            color: videoColors.mutedIvory,
            fontSize: videoTypography.subtitle,
            lineHeight: 1.4,
            marginTop: 24,
            maxWidth: 760,
          }}
        >
          {props.songSummary}
        </div>
      </div>

      <div
        style={{
          bottom: 210,
          color: videoColors.ivory,
          fontSize: videoTypography.lyric,
          fontWeight: 680,
          left: 770,
          letterSpacing: 0,
          lineHeight: 1.24,
          minHeight: 156,
          opacity: lineOpacity,
          position: 'absolute',
          right: safeArea.right,
          textShadow: '0 4px 22px rgba(0, 0, 0, 0.75)',
          wordBreak: 'break-word',
        }}
      >
        {lyricParts.map((part, index) => (
          <div key={`${line.startFrame}-${index}`}>{part}</div>
        ))}
      </div>

      <div
        style={{
          alignItems: 'end',
          bottom: 128,
          display: 'flex',
          gap: 5,
          height: 48,
          left: 770,
          opacity: 0.72,
          position: 'absolute',
          right: safeArea.right,
        }}
      >
        {Array.from({length: 42}).map((_, index) => {
          const lift = Math.sin((frame + index * 5) / 9);
          const height = 10 + Math.round((lift + 1) * 15);
          return (
            <div
              key={index}
              style={{
                backgroundColor:
                  index % 9 === 0 ? videoColors.cinnabar : videoColors.gold,
                height,
                opacity: 0.28 + (index % 5) * 0.06,
                width: 7,
              }}
            />
          );
        })}
      </div>

      <div
        style={{
          backgroundColor: 'rgba(246, 237, 220, 0.18)',
          bottom: safeArea.bottom - 18,
          height: 3,
          left: safeArea.left,
          position: 'absolute',
          right: safeArea.right,
        }}
      >
        <div
          style={{
            backgroundColor: videoColors.gold,
            height: 3,
            width: `${progress * 100}%`,
          }}
        />
      </div>
    </AbsoluteFill>
  );
}
