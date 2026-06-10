import {
  AbsoluteFill,
  Audio,
  Img,
  interpolate,
  useCurrentFrame,
  useVideoConfig,
} from 'remotion';
import type {LyricLine, LyricVideoProps} from './render-input';
import {wrapLyricText} from './render-input';
import {toRemotionMediaSource} from './LyricVideo16x9V2';
import {videoColors, videoTypography} from './video-style';

const snowParticles = Array.from({length: 72}, (_, index) => ({
  delay: (index * 17) % 140,
  drift: ((index % 9) - 4) * 8,
  left: (index * 37) % 1920,
  opacity: 0.1 + (index % 5) * 0.035,
  size: 2 + (index % 4),
  speed: 0.28 + (index % 7) * 0.035,
  top: (index * 83) % 1080,
}));

const audioTraceBars = Array.from({length: 72}, (_, index) => ({
  phase: index * 0.47,
  width: index % 13 === 0 ? 2 : 1,
}));

function activeLine(lines: readonly LyricLine[], frame: number): LyricLine {
  return (
    lines.find((line) => frame >= line.startFrame && frame <= line.endFrame) ??
    lines[lines.length - 1] ?? {startFrame: 0, endFrame: 1, text: ''}
  );
}

function isSectionLabel(text: string): boolean {
  return /^(主歌|副歌|桥段|间奏|尾声|verse|chorus|bridge)\s*\d*\s*[:：]$/i.test(
    text.trim(),
  );
}

function cleanSectionLabel(text: string): string {
  return text.trim().replace(/[:：]$/, '');
}

function lyricFontSize(line: string): number {
  const length = Array.from(line).length;
  if (length > 34) {
    return 54;
  }
  if (length > 24) {
    return 60;
  }
  return videoTypography.lyric + 2;
}

export function LyricVideo16x9V3(props: LyricVideoProps) {
  const frame = useCurrentFrame();
  const {durationInFrames} = useVideoConfig();
  const audioSource = toRemotionMediaSource(props.audioSource);
  const coverSource = toRemotionMediaSource(props.coverSource);
  const line = activeLine(props.lyrics, frame);
  const section = isSectionLabel(line.text);
  const lyricParts = section
    ? [cleanSectionLabel(line.text)]
    : wrapLyricText(line.text, 20, 2);

  const progress = Math.min(1, frame / Math.max(1, durationInFrames - 1));
  const outroStart = Math.max(0, durationInFrames - 105);
  const introOpacity = interpolate(frame, [0, 16, 42, 64], [0, 1, 1, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });
  const outroOpacity = interpolate(
    frame,
    [outroStart, outroStart + 34, durationInFrames - 1],
    [0, 1, 1],
    {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'},
  );
  const mainOpacity = interpolate(
    frame,
    [0, 60, outroStart - 18, outroStart + 20],
    [0, 1, 1, 0],
    {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'},
  );
  const lineFrameCount = Math.max(1, line.endFrame - line.startFrame + 1);
  const fadeFrames = Math.min(14, Math.floor(lineFrameCount / 3));
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
          [0, section ? 0.72 : 1, section ? 0.72 : 1, 0],
          {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'},
        )
      : 1;
  const backgroundScale = interpolate(
    frame,
    [0, durationInFrames],
    [1.018, 1.06],
    {
      extrapolateLeft: 'clamp',
      extrapolateRight: 'clamp',
    },
  );
  const backgroundX = interpolate(frame, [0, durationInFrames], [-18, 18], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });
  const lightSweep = interpolate(
    Math.sin(frame / 92),
    [-1, 1],
    [0.16, 0.32],
  );

  return (
    <AbsoluteFill
      style={{
        backgroundColor: '#080b0c',
        color: videoColors.ivory,
        fontFamily:
          'Georgia, "Songti SC", "STSong", "Noto Serif CJK SC", serif',
        overflow: 'hidden',
      }}
    >
      <Audio src={audioSource} />
      <Img
        src={coverSource}
        style={{
          height: 1080,
          objectFit: 'cover',
          position: 'absolute',
          transform: `translateX(${backgroundX}px) scale(${backgroundScale})`,
          width: 1920,
        }}
      />
      <AbsoluteFill
        style={{
          background:
            'linear-gradient(180deg, rgba(4,7,8,0.38) 0%, rgba(4,7,8,0.08) 36%, rgba(4,7,8,0.54) 76%, rgba(4,7,8,0.88) 100%)',
        }}
      />
      <AbsoluteFill
        style={{
          background:
            'radial-gradient(circle at 50% 28%, rgba(246,237,220,0.12), rgba(246,237,220,0.02) 24%, rgba(4,7,8,0.62) 78%)',
          opacity: lightSweep,
        }}
      />
      <AbsoluteFill
        style={{
          background:
            'linear-gradient(90deg, rgba(4,7,8,0.72), rgba(4,7,8,0.06) 36%, rgba(4,7,8,0.24) 64%, rgba(4,7,8,0.72))',
        }}
      />

      {snowParticles.map((particle, index) => {
        const y =
          (particle.top +
            (frame + particle.delay) * particle.speed * 2.6) %
          1110;
        const x =
          particle.left +
          Math.sin((frame + particle.delay) / 48) * particle.drift;
        return (
          <div
            key={index}
            style={{
              backgroundColor: 'rgba(246, 237, 220, 0.86)',
              borderRadius: 999,
              filter: 'blur(0.4px)',
              height: particle.size,
              left: x,
              opacity: particle.opacity,
              position: 'absolute',
              top: y - 20,
              width: particle.size,
            }}
          />
        );
      })}

      <div
        style={{
          borderBottom: '1px solid rgba(246, 237, 220, 0.18)',
          borderTop: '1px solid rgba(246, 237, 220, 0.08)',
          bottom: 94,
          height: 214,
          left: 112,
          opacity: mainOpacity,
          position: 'absolute',
          right: 112,
        }}
      />

      <div
        style={{
          color: 'rgba(246, 237, 220, 0.78)',
          fontFamily:
            '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
          fontSize: 23,
          fontWeight: 620,
          left: 112,
          letterSpacing: 0,
          lineHeight: 1.2,
          opacity: mainOpacity,
          position: 'absolute',
          textShadow: '0 2px 14px rgba(0,0,0,0.55)',
          top: 70,
        }}
      >
        燕云乐坊
        <span
          style={{
            color: 'rgba(199, 164, 90, 0.86)',
            marginLeft: 20,
          }}
        >
          YANYUN MUSIC HOUSE
        </span>
      </div>

      <div
        style={{
          color: 'rgba(246, 237, 220, 0.62)',
          fontFamily:
            '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
          fontSize: 20,
          fontWeight: 500,
          letterSpacing: 0,
          opacity: mainOpacity,
          position: 'absolute',
          right: 112,
          top: 72,
        }}
      >
        WORK {props.workId.slice(0, 8).toUpperCase()}
      </div>

      <div
        style={{
          bottom: 336,
          left: 112,
          opacity: introOpacity,
          position: 'absolute',
          textShadow: '0 8px 36px rgba(0,0,0,0.72)',
          width: 980,
        }}
      >
        <div
          style={{
            color: 'rgba(199, 164, 90, 0.9)',
            fontFamily:
              '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
            fontSize: 24,
            fontWeight: 650,
            letterSpacing: 0,
            marginBottom: 18,
          }}
        >
          ORIGINAL SONG
        </div>
        <div
          style={{
            color: 'rgba(246, 237, 220, 0.96)',
            fontSize: 88,
            fontWeight: 680,
            letterSpacing: 0,
            lineHeight: 1.05,
          }}
        >
          {props.songTitle}
        </div>
        <div
          style={{
            color: 'rgba(246, 237, 220, 0.68)',
            fontFamily:
              '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
            fontSize: 28,
            lineHeight: 1.45,
            marginTop: 22,
            maxWidth: 820,
          }}
        >
          {props.songSummary}
        </div>
      </div>

      <div
        style={{
          bottom: 176,
          color: section
            ? 'rgba(199, 164, 90, 0.9)'
            : 'rgba(246, 237, 220, 0.96)',
          fontSize: section ? 38 : lyricFontSize(line.text),
          fontWeight: section ? 620 : 660,
          left: 214,
          letterSpacing: 0,
          lineHeight: 1.26,
          maxWidth: 1190,
          minHeight: 146,
          opacity: lineOpacity * mainOpacity,
          position: 'absolute',
          textShadow: '0 4px 22px rgba(0,0,0,0.82)',
          wordBreak: 'break-word',
        }}
      >
        {lyricParts.map((part, index) => (
          <div key={`${line.startFrame}-${index}`}>{part}</div>
        ))}
      </div>

      <div
        style={{
          alignItems: 'center',
          bottom: 132,
          display: 'flex',
          gap: 7,
          height: 38,
          left: 214,
          opacity: 0.64 * mainOpacity,
          position: 'absolute',
          width: 760,
        }}
      >
        {audioTraceBars.map((bar, index) => {
          const pulse = Math.sin(frame / 11 + bar.phase);
          const slowPulse = Math.sin(frame / 41 + bar.phase * 0.7);
          const height = 7 + Math.round((pulse + 1) * 7 + (slowPulse + 1) * 4);
          return (
            <div
              key={index}
              style={{
                backgroundColor:
                  index % 16 === 0
                    ? 'rgba(159, 61, 46, 0.78)'
                    : 'rgba(199, 164, 90, 0.68)',
                height,
                opacity: 0.22 + (index % 6) * 0.045,
                width: bar.width,
              }}
            />
          );
        })}
      </div>

      <div
        style={{
          backgroundColor: 'rgba(246, 237, 220, 0.14)',
          bottom: 74,
          height: 2,
          left: 112,
          opacity: mainOpacity,
          position: 'absolute',
          right: 112,
        }}
      >
        <div
          style={{
            backgroundColor: 'rgba(199, 164, 90, 0.92)',
            height: 2,
            width: `${progress * 100}%`,
          }}
        />
      </div>

      <div
        style={{
          bottom: 306,
          left: 112,
          opacity: outroOpacity,
          position: 'absolute',
          textShadow: '0 8px 34px rgba(0,0,0,0.78)',
          width: 980,
        }}
      >
        <div
          style={{
            color: 'rgba(246, 237, 220, 0.96)',
            fontSize: 76,
            fontWeight: 680,
            lineHeight: 1.08,
          }}
        >
          {props.songTitle}
        </div>
        <div
          style={{
            color: 'rgba(199, 164, 90, 0.88)',
            fontFamily:
              '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
            fontSize: 28,
            fontWeight: 640,
            marginTop: 26,
          }}
        >
          燕云乐坊 · 玩家原创曲
        </div>
        <div
          style={{
            color: 'rgba(246, 237, 220, 0.62)',
            fontFamily:
              '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
            fontSize: 22,
            marginTop: 14,
          }}
        >
          WORK {props.workId.slice(0, 8).toUpperCase()}
        </div>
      </div>
    </AbsoluteFill>
  );
}
