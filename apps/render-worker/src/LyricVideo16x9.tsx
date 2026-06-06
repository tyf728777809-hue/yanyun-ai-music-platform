import {AbsoluteFill, interpolate, useCurrentFrame} from 'remotion';
import type {LyricLine, LyricVideoProps} from './render-input';

function activeLine(lines: readonly LyricLine[], frame: number): LyricLine {
  return (
    lines.find((line) => frame >= line.startFrame && frame <= line.endFrame) ??
    lines[lines.length - 1] ?? {startFrame: 0, endFrame: 1, text: ''}
  );
}

export function LyricVideo16x9({
  songTitle,
  songSummary,
  coverLabel,
  lyrics,
}: LyricVideoProps) {
  const frame = useCurrentFrame();
  const line = activeLine(lyrics, frame);
  const lineFrameCount = Math.max(1, line.endFrame - line.startFrame + 1);
  const fadeFrames = Math.min(12, Math.floor(lineFrameCount / 3));
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
          [0, 1, 1, 0],
          {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'},
        )
      : 1;
  const coverScale = interpolate(frame, [0, 240], [1, 1.035], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  return (
    <AbsoluteFill
      style={{
        background:
          'linear-gradient(135deg, #111714 0%, #1f302c 42%, #6e4f28 100%)',
        color: '#f8f4e8',
        fontFamily:
          '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
      }}
    >
      <div
        style={{
          bottom: 72,
          display: 'grid',
          gap: 72,
          gridTemplateColumns: '660px 1fr',
          left: 120,
          position: 'absolute',
          right: 120,
          top: 72,
        }}
      >
        <div
          style={{
            alignItems: 'center',
            alignSelf: 'center',
            aspectRatio: '1 / 1',
            background:
              'radial-gradient(circle at 35% 28%, #f0c878 0%, #8d6230 34%, #1c2824 70%)',
            border: '1px solid rgba(248, 244, 232, 0.2)',
            boxShadow: '0 40px 90px rgba(0, 0, 0, 0.42)',
            display: 'flex',
            justifyContent: 'center',
            overflow: 'hidden',
            transform: `scale(${coverScale})`,
          }}
        >
          <div
            style={{
              fontSize: 78,
              fontWeight: 700,
              letterSpacing: 0,
              opacity: 0.92,
            }}
          >
            {coverLabel}
          </div>
        </div>

        <div
          style={{
            alignSelf: 'center',
            display: 'flex',
            flexDirection: 'column',
            gap: 38,
            justifyContent: 'center',
            minWidth: 0,
          }}
        >
          <div
            style={{
              color: '#f0c878',
              fontSize: 34,
              fontWeight: 650,
              letterSpacing: 0,
            }}
          >
            燕云乐坊
          </div>
          <div
            style={{
              fontSize: 92,
              fontWeight: 760,
              letterSpacing: 0,
              lineHeight: 1.04,
              maxWidth: 820,
            }}
          >
            {songTitle}
          </div>
          <div
            style={{
              color: 'rgba(248, 244, 232, 0.78)',
              fontSize: 34,
              lineHeight: 1.42,
              maxWidth: 760,
            }}
          >
            {songSummary}
          </div>
          <div
            style={{
              backgroundColor: 'rgba(0, 0, 0, 0.26)',
              border: '1px solid rgba(248, 244, 232, 0.16)',
              color: '#fff7df',
              fontSize: 46,
              fontWeight: 640,
              lineHeight: 1.32,
              marginTop: 28,
              maxWidth: 900,
              minHeight: 148,
              opacity: lineOpacity,
              padding: '34px 42px',
            }}
          >
            {line.text}
          </div>
        </div>
      </div>
    </AbsoluteFill>
  );
}
