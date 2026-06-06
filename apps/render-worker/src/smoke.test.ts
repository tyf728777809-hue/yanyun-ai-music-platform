import assert from 'node:assert/strict';
import test from 'node:test';
import {
  VIDEO_DURATION_IN_FRAMES,
  VIDEO_FPS,
  VIDEO_HEIGHT,
  VIDEO_WIDTH,
} from './root';
import {
  lyricLinesFromText,
  lyricVideoPropsFromJobInput,
  sampleLyricVideoProps,
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
  });

  assert.equal(props.durationInFrames, 5400);
  assert.equal(props.lyrics[0].startFrame, 0);
  assert.equal(props.lyrics[props.lyrics.length - 1].endFrame, 5399);
});

test('blank lyrics still produce a safe renderable timeline', () => {
  const lines = lyricLinesFromText('', 90);

  assert.equal(lines.length, 1);
  assert.equal(lines[0].startFrame, 0);
  assert.equal(lines[0].endFrame, 89);
  assert.ok(lines[0].text.length > 0);
});
