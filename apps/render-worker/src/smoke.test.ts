import assert from 'node:assert/strict';
import test from 'node:test';
import {
  VIDEO_DURATION_IN_FRAMES,
  VIDEO_FPS,
  VIDEO_HEIGHT,
  VIDEO_WIDTH,
} from './root';
import {sampleLyricVideoProps} from './render-input';

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
