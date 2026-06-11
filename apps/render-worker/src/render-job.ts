import {bundle} from '@remotion/bundler';
import {renderMedia, selectComposition} from '@remotion/renderer';
import {execFile} from 'node:child_process';
import {copyFile, mkdir, readFile, stat, writeFile} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath, pathToFileURL} from 'node:url';
import {promisify} from 'node:util';
import {
  durationMsFromFrames,
  lyricVideoPropsFromJobInput,
  type RenderWorkerJobInput,
  type RenderWorkerJobOutput,
  VIDEO_FPS,
  VIDEO_HEIGHT,
  VIDEO_WIDTH,
} from './render-input';

const DEFAULT_COMPOSITION_ID = 'LyricVideo16x9V2';
const execFileAsync = promisify(execFile);

type RenderJobPaths = {
  readonly inputPath: string;
  readonly outputPath: string;
  readonly outDir: string;
};

type TimelineJson = {
  readonly work_id: string;
  readonly template_id: string;
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
  const compositionId = input.composition_id || DEFAULT_COMPOSITION_ID;
  const outputDirectory = path.resolve(paths.outDir);
  await mkdir(outputDirectory, {recursive: true});

  const safeWorkId = sanitizeFileSegment(input.work_id);
  const {input: renderInput, publicDir} = await stageLocalRenderAssets(
    input,
    outputDirectory,
    safeWorkId,
  );
  const props = lyricVideoPropsFromJobInput(renderInput);
  const videoFilePath = path.join(outputDirectory, `${safeWorkId}.mp4`);
  const timelineFilePath = path.join(outputDirectory, `${safeWorkId}.timeline.json`);
  const entryPoint = path.join(renderWorkerRoot(), 'src', 'index.ts');
  const serveUrl = publicDir
    ? await bundle({entryPoint, publicDir})
    : await bundle(entryPoint);
  const composition = await selectComposition({
    serveUrl,
    id: compositionId,
    inputProps: props,
    logLevel: 'warn',
  });

  await writeFile(
    timelineFilePath,
    JSON.stringify(timelineJson(renderInput, props), null, 2),
  );
  await renderMedia({
    audioCodec: 'aac',
    codec: 'h264',
    composition,
    inputProps: props,
    logLevel: 'warn',
    outputLocation: videoFilePath,
    overwrite: true,
    serveUrl,
  });
  await stat(videoFilePath);
  await assertPlayableMp4(videoFilePath);

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
    template_id: props.templateId,
    fps: VIDEO_FPS,
    duration_ms: durationMsFromFrames(props.durationInFrames),
    duration_in_frames: props.durationInFrames,
    safe_area: props.safeArea,
    lines: props.lyrics.map((line) => ({
      start_frame: line.startFrame,
      end_frame: line.endFrame,
      text: line.text,
    })),
  };
}

export async function assertPlayableMp4(videoFilePath: string): Promise<void> {
  const {stdout} = await execFileAsync('ffprobe', [
    '-v',
    'error',
    '-show_entries',
    'stream=codec_type,codec_name,width,height:format=duration',
    '-of',
    'json',
    videoFilePath,
  ]);
  validateMp4ProbeOutput(JSON.parse(stdout));
}

export function validateMp4ProbeOutput(parsed: unknown): void {
  const probe = parsed as {
    streams?: Array<{
      codec_type?: string;
      codec_name?: string;
      width?: number;
      height?: number;
    }>;
    format?: {duration?: string | number};
  };
  const streams = probe.streams ?? [];
  const video = streams.find((stream) => stream.codec_type === 'video');
  const audio = streams.find((stream) => stream.codec_type === 'audio');
  if (!video) {
    throw new Error('Rendered MP4 is missing video stream');
  }
  if (!audio) {
    throw new Error('Rendered MP4 is missing audio stream');
  }
  if (video.codec_name !== 'h264') {
    throw new Error(`Rendered MP4 video codec is not h264: ${video.codec_name ?? 'unknown'}`);
  }
  if (audio.codec_name !== 'aac') {
    throw new Error(`Rendered MP4 audio codec is not aac: ${audio.codec_name ?? 'unknown'}`);
  }
  if (video.width !== VIDEO_WIDTH || video.height !== VIDEO_HEIGHT) {
    throw new Error(
      `Rendered MP4 resolution is not ${VIDEO_WIDTH}x${VIDEO_HEIGHT}: ${video.width ?? 0}x${video.height ?? 0}`,
    );
  }
  const durationSeconds = Number(probe.format?.duration ?? 0);
  if (!Number.isFinite(durationSeconds) || durationSeconds <= 0) {
    throw new Error('Rendered MP4 duration is missing or invalid');
  }
}

function sanitizeFileSegment(value: string): string {
  const sanitized = value.replace(/[^a-zA-Z0-9._-]/g, '-');
  return sanitized.length > 0 ? sanitized : 'work';
}

async function stageLocalRenderAssets(
  input: RenderWorkerJobInput,
  outputDirectory: string,
  safeWorkId: string,
): Promise<{input: RenderWorkerJobInput; publicDir?: string}> {
  const publicDir = path.join(outputDirectory, `${safeWorkId}.public`);
  let stagedInput = input;
  let stagedAnyAsset = false;

  const stagedAudio = await stageLocalAsset(
    input.audio_source_path,
    publicDir,
    'audio',
  );
  if (stagedAudio) {
    stagedAnyAsset = true;
    stagedInput = {...stagedInput, audio_source_path: `static://${stagedAudio}`};
  }

  const stagedCover = await stageLocalAsset(
    input.cover_source_path,
    publicDir,
    'cover',
  );
  if (stagedCover) {
    stagedAnyAsset = true;
    stagedInput = {...stagedInput, cover_source_path: `static://${stagedCover}`};
  }

  return stagedAnyAsset ? {input: stagedInput, publicDir} : {input};
}

async function stageLocalAsset(
  source: string | undefined,
  publicDir: string,
  baseName: string,
): Promise<string | undefined> {
  const localPath = localFilePathFromSource(source);
  if (!localPath) {
    return undefined;
  }

  const sourceStat = await stat(localPath).catch(() => undefined);
  if (!sourceStat?.isFile()) {
    return undefined;
  }

  await mkdir(publicDir, {recursive: true});
  const extension = path.extname(localPath) || '.bin';
  const publicFileName = `${baseName}${extension}`;
  await copyFile(localPath, path.join(publicDir, publicFileName));
  return publicFileName;
}

function localFilePathFromSource(source: string | undefined): string | undefined {
  const trimmedSource = source?.trim();
  if (!trimmedSource) {
    return undefined;
  }
  if (
    trimmedSource.startsWith('http://') ||
    trimmedSource.startsWith('https://') ||
    trimmedSource.startsWith('data:') ||
    trimmedSource.startsWith('blob:') ||
    trimmedSource.startsWith('static://')
  ) {
    return undefined;
  }
  if (trimmedSource.startsWith('file://')) {
    return fileURLToPath(trimmedSource);
  }
  if (path.isAbsolute(trimmedSource)) {
    return trimmedSource;
  }
  return undefined;
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
