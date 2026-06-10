import {Composition} from 'remotion';
import {LyricVideo16x9} from './LyricVideo16x9';
import {LyricVideo16x9V2} from './LyricVideo16x9V2';
import {LyricVideo16x9V3} from './LyricVideo16x9V3';
import {
  sampleLyricVideoProps,
  VIDEO_DURATION_IN_FRAMES,
  VIDEO_FPS,
  VIDEO_HEIGHT,
  VIDEO_WIDTH,
} from './render-input';
import {YanyunScaffold} from './YanyunScaffold';

export {
  VIDEO_DURATION_IN_FRAMES,
  VIDEO_FPS,
  VIDEO_HEIGHT,
  VIDEO_WIDTH,
} from './render-input';

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
        calculateMetadata={({props}) => ({
          durationInFrames: props.durationInFrames,
        })}
      />
      <Composition
        id="LyricVideo16x9V2"
        component={LyricVideo16x9V2}
        durationInFrames={VIDEO_DURATION_IN_FRAMES}
        fps={VIDEO_FPS}
        width={VIDEO_WIDTH}
        height={VIDEO_HEIGHT}
        defaultProps={sampleLyricVideoProps}
        calculateMetadata={({props}) => ({
          durationInFrames: props.durationInFrames,
        })}
      />
      <Composition
        id="LyricVideo16x9V3"
        component={LyricVideo16x9V3}
        durationInFrames={VIDEO_DURATION_IN_FRAMES}
        fps={VIDEO_FPS}
        width={VIDEO_WIDTH}
        height={VIDEO_HEIGHT}
        defaultProps={sampleLyricVideoProps}
        calculateMetadata={({props}) => ({
          durationInFrames: props.durationInFrames,
        })}
      />
    </>
  );
}
