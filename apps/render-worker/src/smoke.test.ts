import assert from 'node:assert/strict';
import test from 'node:test';
import {
  VIDEO_DURATION_IN_FRAMES,
  VIDEO_FPS,
  VIDEO_HEIGHT,
  VIDEO_WIDTH,
} from './root';
import {toRemotionMediaSource} from './LyricVideo16x9V2';
import {validateMp4ProbeOutput} from './render-job';
import {
  lyricLinesFromText,
  lyricVideoPropsFromJobInput,
  sampleLyricVideoProps,
  wrapLyricText,
} from './render-input';

test('Remotion lyric video uses 16:9 output settings', () => {
  assert.equal(VIDEO_WIDTH, 1920);
  assert.equal(VIDEO_HEIGHT, 1080);
  assert.equal(VIDEO_FPS, 30);
  assert.equal(VIDEO_DURATION_IN_FRAMES, 240);
});

test('sample lyric video props cover the full composition duration', () => {
  assert.equal(sampleLyricVideoProps.workId, 'sample-work');
  assert.ok(sampleLyricVideoProps.songTitle.length > 0);
  assert.ok(sampleLyricVideoProps.lyrics.length > 0);

  const firstLine = sampleLyricVideoProps.lyrics[0];
  const lastLine =
    sampleLyricVideoProps.lyrics[sampleLyricVideoProps.lyrics.length - 1];

  assert.equal(firstLine.startFrame, 0);
  assert.equal(lastLine.endFrame, VIDEO_DURATION_IN_FRAMES - 1);
});

test('render job input creates dynamic duration props', () => {
  const props = lyricVideoPropsFromJobInput({
    work_id: 'work-001',
    song_title: '边城旧梦',
    song_summary: '旧梦重回燕云。',
    lyrics_text: '第一句\n第二句\n第三句',
    audio_object_key: 'audio/work-001.mp3',
    audio_mime_type: 'audio/mpeg',
    cover_object_key: 'covers/work-001.png',
    duration_ms: 180000,
    lyrics_timing_source: 'provider_timestamped',
  });

  assert.equal(props.durationInFrames, 5400);
  assert.equal(props.lyrics[0].startFrame, 0);
  assert.equal(props.lyrics[props.lyrics.length - 1].endFrame, 5399);
});

test('estimated lyrics timing uses weak lyric cards instead of hard-sync subtitles', () => {
  const props = lyricVideoPropsFromJobInput({
    work_id: 'work-estimated',
    song_title: '边城旧梦',
    lyrics_text: '第一句\n第二句\n第三句\n第四句',
    audio_object_key: 'audio/work-estimated.mp3',
    cover_object_key: 'covers/work-estimated.png',
    duration_ms: 120000,
    lyrics_timing_source: 'estimated',
  });

  assert.equal(props.lyrics.length, 2);
  assert.ok(props.lyrics[0].startFrame > 0);
  assert.notEqual(props.lyrics[0].endFrame, props.lyrics[1].startFrame - 1);
});

test('v2 render props include staged media sources and safe area metadata', () => {
  const props = lyricVideoPropsFromJobInput({
    work_id: 'work-v2',
    song_title: '雁门雪夜',
    song_summary: '故人归来。',
    lyrics_text: '第一句歌词很长需要安全换行\n第二句',
    audio_object_key: 'audio/work-v2.mp3',
    audio_mime_type: 'audio/mpeg',
    cover_object_key: 'covers/work-v2.png',
    audio_source_path: '/tmp/audio.mp3',
    cover_source_path: '/tmp/cover.png',
    template_id: 'lyric-video-16x9-v2',
    duration_ms: 120000,
  });

  assert.equal(props.audioSource, '/tmp/audio.mp3');
  assert.equal(props.coverSource, '/tmp/cover.png');
  assert.equal(props.templateId, 'lyric-video-16x9-v2');
  assert.equal(props.safeArea.left, 120);
  assert.equal(props.safeArea.top, 84);
  assert.equal(props.lyricLayout.maxLines, 2);
  assert.ok(props.lyrics[0].text.length > 0);
});

test('sample props default to v2 template', () => {
  assert.equal(sampleLyricVideoProps.templateId, 'lyric-video-16x9-v2');
  assert.ok(sampleLyricVideoProps.audioSource.startsWith('data:audio/wav'));
  assert.ok(sampleLyricVideoProps.coverSource.startsWith('data:image/svg+xml'));
});

test('render job input can explicitly select the v3 visualizer template', () => {
  const props = lyricVideoPropsFromJobInput({
    work_id: 'work-v3',
    song_title: '雁门雪夜故人归',
    song_summary: '故人踏雪归来。',
    lyrics_text: '雁门关上雪花飘',
    audio_object_key: 'audio/work-v3.mp3',
    cover_object_key: 'covers/work-v3.png',
    template_id: 'lyric-video-16x9-v3',
    duration_ms: 168900,
  });

  assert.equal(props.templateId, 'lyric-video-16x9-v3');
  assert.equal(props.durationInFrames, 5067);
});

test('long lyric lines wrap inside the configured v2 lyric layout', () => {
  const wrapped = wrapLyricText('第一句歌词很长需要安全换行并避免越过画面边界', 12, 2);

  assert.equal(wrapped.length, 2);
  assert.ok(wrapped.every((line) => line.length <= 12));
});

test('v2 remotion media sources support local filesystem paths', () => {
  assert.equal(
    toRemotionMediaSource('/tmp/燕云 cover #1.png'),
    'file:///tmp/%E7%87%95%E4%BA%91%20cover%20%231.png',
  );
  assert.equal(
    toRemotionMediaSource('data:audio/wav;base64,AAAA'),
    'data:audio/wav;base64,AAAA',
  );
});

test('blank lyrics still produce a safe renderable timeline', () => {
  const lines = lyricLinesFromText('', 90);

  assert.equal(lines.length, 1);
  assert.ok(lines[0].startFrame > 0);
  assert.ok(lines[0].text.length > 0);
});

test('lyric section labels are not rendered as standalone subtitles', () => {
  const lines = lyricLinesFromText('主歌1:\n第一句\n副歌:\n第二句', 120);

  assert.deepEqual(
    lines.map((line) => line.text),
    ['第一句', '第二句'],
  );
  assert.ok(lines[0].startFrame > 0);
});

test('ffprobe validator requires h264 aac 1080p mp4 with duration', () => {
  assert.doesNotThrow(() =>
    validateMp4ProbeOutput({
      streams: [
        {codec_type: 'video', codec_name: 'h264', width: 1920, height: 1080},
        {codec_type: 'audio', codec_name: 'aac'},
      ],
      format: {duration: '180.0'},
    }),
  );

  assert.throws(
    () =>
      validateMp4ProbeOutput({
        streams: [{codec_type: 'video', codec_name: 'h264', width: 1920, height: 1080}],
        format: {duration: '180.0'},
      }),
    /audio stream/,
  );
});
