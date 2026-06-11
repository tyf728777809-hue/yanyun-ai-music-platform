# render-worker album-ffmpeg 快速成片集成 v0.1

## Summary

默认视频成片路线改为 `album-ffmpeg`：使用对象存储中的音频和 16:9 封面，通过 FFmpeg 快速封装为 H.264 + AAC MP4，并生成无字幕 timeline JSON。该路线用于当前真实用户测试和公司交接默认路径；Remotion v4、高级 Visualizer、弱歌词卡和无可靠时间轴字幕不再作为产品路径推进。

## Scope

- 输入：`AUDIO` media asset、`COVER` media asset。
- 输出：`VIDEO` media asset、`TIMELINE` media asset。
- 视频：1920x1080、30fps、H.264、AAC、`faststart`。
- 字幕：默认无字幕；`lyrics_timing_source=none`。
- 模板标识：`album-cover-mp4-v5`。

## Runtime Behavior

1. API / worker 通过 `RENDER_WORKER_MODE=album-ffmpeg` 启用 `FfmpegAlbumVideoRenderService`。
2. 服务从对象存储读取音频和封面到临时目录。
3. 封面预处理为 1920x1080 安全画幅，不强裁主体。
4. FFmpeg 使用静态封面和原曲音频生成 MP4。
5. FFprobe 校验输出必须包含 H.264 video、AAC audio、1920x1080，且时长接近音频。
6. 校验通过后写入对象存储，并入库 `VIDEO` 与 `TIMELINE` 资产。

## Non Goals

- 不做逐句字幕、歌词卡、波形动画或高级视觉模板。
- 不调用 Remotion render-worker CLI。
- 不解决封面生成质量；封面质量由 `CoverPromptAgent` 和封面质量门负责。
- 不替代生产容量规划；并发能力需要单独压测和队列限流设计。

## Acceptance

- `RENDER_WORKER_MODE=album-ffmpeg EXPECT_RENDER_WORKER=album-ffmpeg scripts/smoke/api-main-flow.sh` 通过。
- MP4 可被 `ffprobe` 识别为 H.264 video + AAC audio。
- timeline JSON 标记 `lyrics_timing_source=none`、`subtitle_strategy=none`。
- `RENDER_WORKER_MODE=local-process` 不再被 Spring 生产配置接受。
