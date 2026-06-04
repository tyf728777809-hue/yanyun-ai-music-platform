import {Composition} from 'remotion';
import {YanyunScaffold} from './YanyunScaffold.js';

export const VIDEO_WIDTH = 1920;
export const VIDEO_HEIGHT = 1080;
export const VIDEO_FPS = 30;
export const VIDEO_DURATION_IN_FRAMES = 150;

export function RemotionRoot() {
  return (
    <Composition
      id="YanyunScaffold"
      component={YanyunScaffold}
      durationInFrames={VIDEO_DURATION_IN_FRAMES}
      fps={VIDEO_FPS}
      width={VIDEO_WIDTH}
      height={VIDEO_HEIGHT}
    />
  );
}
