import {Composition} from 'remotion';
import {LyricVideo16x9} from './LyricVideo16x9';
import {sampleLyricVideoProps} from './render-input';
import {YanyunScaffold} from './YanyunScaffold';

export const VIDEO_WIDTH = 1920;
export const VIDEO_HEIGHT = 1080;
export const VIDEO_FPS = 30;
export const VIDEO_DURATION_IN_FRAMES = 240;

export function RemotionRoot() {
  return (
    <>
      <Composition
        id="YanyunScaffold"
        component={YanyunScaffold}
        durationInFrames={VIDEO_DURATION_IN_FRAMES}
        fps={VIDEO_FPS}
        width={VIDEO_WIDTH}
        height={VIDEO_HEIGHT}
      />
      <Composition
        id="LyricVideo16x9"
        component={LyricVideo16x9}
        durationInFrames={VIDEO_DURATION_IN_FRAMES}
        fps={VIDEO_FPS}
        width={VIDEO_WIDTH}
        height={VIDEO_HEIGHT}
        defaultProps={sampleLyricVideoProps}
      />
    </>
  );
}
