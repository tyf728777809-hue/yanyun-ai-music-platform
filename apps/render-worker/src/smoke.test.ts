import assert from 'node:assert/strict';
import test from 'node:test';
import {
  VIDEO_DURATION_IN_FRAMES,
  VIDEO_FPS,
  VIDEO_HEIGHT,
  VIDEO_WIDTH,
} from './root.js';

test('Remotion scaffold uses 16:9 output settings', () => {
  assert.equal(VIDEO_WIDTH, 1920);
  assert.equal(VIDEO_HEIGHT, 1080);
  assert.equal(VIDEO_FPS, 30);
  assert.ok(VIDEO_DURATION_IN_FRAMES > 0);
});
