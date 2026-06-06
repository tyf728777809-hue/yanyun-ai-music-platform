import {bundle} from '@remotion/bundler';
import {renderMedia, selectComposition} from '@remotion/renderer';
import {mkdir, readFile, stat, writeFile} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath, pathToFileURL} from 'node:url';
import {
  durationMsFromFrames,
  lyricVideoPropsFromJobInput,
  type RenderWorkerJobInput,
  type RenderWorkerJobOutput,
  VIDEO_FPS,
  VIDEO_HEIGHT,
  VIDEO_WIDTH,
} from './render-input';

const DEFAULT_COMPOSITION_ID = 'LyricVideo16x9';

type RenderJobPaths = {
  readonly inputPath: string;
  readonly outputPath: string;
  readonly outDir: string;
};

type TimelineJson = {
  readonly work_id: string;
  readonly fps: number;
  readonly duration_ms: number;
  readonly duration_in_frames: number;
  readonly safe_area: {
    readonly left: number;
    readonly right: number;
    readonly top: number;
    readonly bottom: number;
  };
  readonly lines: readonly {
    readonly start_frame: number;
    readonly end_frame: number;
    readonly text: string;
  }[];
};

export async function runRenderJob(paths: RenderJobPaths): Promise<RenderWorkerJobOutput> {
  const input = JSON.parse(
    await readFile(paths.inputPath, 'utf8'),
  ) as RenderWorkerJobInput;
  const props = lyricVideoPropsFromJobInput(input);
  const compositionId = input.composition_id || DEFAULT_COMPOSITION_ID;
  const outputDirectory = path.resolve(paths.outDir);
  await mkdir(outputDirectory, {recursive: true});

  const safeWorkId = sanitizeFileSegment(input.work_id);
  const videoFilePath = path.join(outputDirectory, `${safeWorkId}.mp4`);
  const timelineFilePath = path.join(outputDirectory, `${safeWorkId}.timeline.json`);
  const entryPoint = path.join(renderWorkerRoot(), 'src', 'index.ts');
  const serveUrl = await bundle(entryPoint);
  const composition = await selectComposition({
    serveUrl,
    id: compositionId,
    inputProps: props,
    logLevel: 'warn',
  });

  await writeFile(timelineFilePath, JSON.stringify(timelineJson(input, props), null, 2));
  await renderMedia({
    codec: 'h264',
    composition,
    inputProps: props,
    logLevel: 'warn',
    muted: true,
    outputLocation: videoFilePath,
    overwrite: true,
    serveUrl,
  });
  await stat(videoFilePath);

  const output: RenderWorkerJobOutput = {
    work_id: input.work_id,
    video_file_path: videoFilePath,
    timeline_file_path: timelineFilePath,
    width: VIDEO_WIDTH,
    height: VIDEO_HEIGHT,
    fps: VIDEO_FPS,
    duration_ms: durationMsFromFrames(props.durationInFrames),
    duration_in_frames: props.durationInFrames,
    renderer: 'remotion',
    composition_id: compositionId,
  };
  await writeFile(paths.outputPath, JSON.stringify(output, null, 2));
  return output;
}

function timelineJson(
  input: RenderWorkerJobInput,
  props: ReturnType<typeof lyricVideoPropsFromJobInput>,
): TimelineJson {
  return {
    work_id: input.work_id,
    fps: VIDEO_FPS,
    duration_ms: durationMsFromFrames(props.durationInFrames),
    duration_in_frames: props.durationInFrames,
    safe_area: {
      left: 120,
      right: 120,
      top: 72,
      bottom: 72,
    },
    lines: props.lyrics.map((line) => ({
      start_frame: line.startFrame,
      end_frame: line.endFrame,
      text: line.text,
    })),
  };
}

function sanitizeFileSegment(value: string): string {
  const sanitized = value.replace(/[^a-zA-Z0-9._-]/g, '-');
  return sanitized.length > 0 ? sanitized : 'work';
}

function renderWorkerRoot(): string {
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
}

function parseArgs(argv: readonly string[]): RenderJobPaths {
  const inputPath = valueAfter(argv, '--input');
  const outputPath = valueAfter(argv, '--output');
  const outDir = valueAfter(argv, '--out-dir');
  if (!inputPath || !outputPath || !outDir) {
    throw new Error(
      'Usage: tsx src/render-job.ts --input <input.json> --output <output.json> --out-dir <dir>',
    );
  }
  return {inputPath, outputPath, outDir};
}

function valueAfter(argv: readonly string[], name: string): string | undefined {
  const index = argv.indexOf(name);
  return index >= 0 ? argv[index + 1] : undefined;
}

async function main(): Promise<void> {
  await runRenderJob(parseArgs(process.argv.slice(2)));
}

if (import.meta.url === pathToFileURL(process.argv[1] ?? '').href) {
  main().catch((error: unknown) => {
    console.error(error instanceof Error ? error.message : String(error));
    process.exitCode = 1;
  });
}
