# Cover and Video Rendering Pipeline v0.1

更新时间：2026-06-06

## 1. 标题与元数据

- 标题：第 7 批封面与 Remotion/FFmpeg MP4 成片链路
- 作者：Codex
- 状态：Approved for local Mock implementation
- 适用范围：本地 Mock/Fake 链路、后端媒体生成边界、render-worker 最小样例渲染
- 评审依据：PRD v0.3、技术方案 v0.2、OpenAPI v0.1、Stage 6 写词链路规格

## 2. 背景

燕云 AI 作曲平台的核心交付物不是单独音频，而是可交给公司社区发布链路的 16:9 MP4 歌词视频。第 6 批已经把歌词草案中的 `cover_prompt_seed`、知识库版本和 Prompt 版本持久化；第 7 批需要把确认出歌后的封面、时间轴和视频从硬编码 Mock 资产推进到可替换的媒体生成边界。

本批仍不调用真实 Image 2、真实公司系统或真实外部渲染服务。目标是建立商用级边界和可验证最小闭环：后端通过 `CoverGenerationService` 与 `VideoRenderService` 生成资产描述，workflow 统一写入 `media_assets` 和发布包；render-worker 至少能以 Remotion/FFmpeg 生成本地样例 MP4，用于证明成片工具链可运行。

## 3. 功能需求

- FR-1：确认出歌成功后，系统 MUST 生成并持久化 `AUDIO`、`COVER`、`VIDEO`、`TIMELINE` 四类媒体资产。
- FR-2：封面生成 MUST 通过 `CoverGenerationService` 边界完成，业务 workflow MUST NOT 直接硬编码封面生成逻辑。
- FR-3：视频渲染 MUST 通过 `VideoRenderService` 边界完成，业务 workflow MUST NOT 直接硬编码视频和时间轴生成逻辑。
- FR-4：本地阶段的封面和视频服务 MUST 使用 Mock/Fake 实现，不得调用真实 Image 2、真实 Remotion 服务、真实公司系统或外部网络。
- FR-5：首期视频输出规格 MUST 为 16:9，宽 1920、高 1080、容器 MP4、编码口径为 H.264/AAC 目标。
- FR-6：歌词字幕时间轴 MUST 以 `TIMELINE` 媒体资产形式记录，供发布包 `lyrics.timeline_url` 引用。
- FR-7：封面生成失败 SHOULD 使用默认封面兜底；若本地 Mock 边界尚未实现兜底分支，必须至少把失败收口为 `PACKAGE_BUILD_FAILED` 并释放权益。
- FR-8：视频渲染失败 MUST 收口为 `PACKAGE_BUILD_FAILED`，释放已锁权益，不提交主出歌权益，不生成发布包。
- FR-9：发布包 JSON MUST 使用实际生成的 media asset object key 组装 `video.url`、`cover.url` 和 `lyrics.timeline_url`，不得再写死路径。
- FR-10：render-worker MUST 保留 Remotion 16:9 composition，并 SHOULD 提供本地样例渲染命令输出 MP4。

## 4. 非功能需求

- NFR-1：自动化测试 MUST NOT 触发真实 Image 2、真实 DreamMaker、真实 DeepSeek、真实公司系统或外部渲染服务。
- NFR-2：Mock 媒体生成结果 MUST deterministic，便于测试和发布包 smoke。
- NFR-3：视频和封面元数据 MUST 记录 width、height、duration、renderer/provider 标识。
- NFR-4：失败信息入库前 MUST 复用现有脱敏与截断规则，不泄露 token、JWT、AccessKey 或 SecretKey。
- NFR-5：render-worker 样例渲染 SHOULD 在本地开发机完成；若系统缺少 Remotion/FFmpeg 依赖，必须明确记录失败原因。

## 5. 验收标准

- AC-1：Given Mock 音乐 Provider 成功，When `SongProductionWorkflow.produce` 执行，Then `media_assets` 写入 `AUDIO`、`COVER`、`VIDEO`、`TIMELINE` 四类资产。覆盖 FR-1。
- AC-2：Given Mock workflow 成功，When 捕获 `COVER` 和 `VIDEO` 资产，Then 二者 width=1920、height=1080。覆盖 FR-5、NFR-3。
- AC-3：Given Mock workflow 成功，When 读取发布包 JSON，Then `video.url`、`cover.url`、`lyrics.timeline_url` 来自对应媒体资产 object key。覆盖 FR-9。
- AC-4：Given VideoRenderService 抛出异常，When workflow 执行，Then 作品失败码为 `PACKAGE_BUILD_FAILED`，权益释放，发布包不生成。覆盖 FR-8。
- AC-5：Given render-worker 依赖可用，When 运行样例渲染命令，Then 生成一个可存在性检查的 MP4 文件。覆盖 FR-10。
- AC-6：Given 全量后端测试运行，When 执行 Gradle 测试，Then 不发生真实 Image 2、DeepSeek、DreamMaker 或公司系统调用。覆盖 FR-4、NFR-1。

## 6. 边界情况

- EC-1：音乐 Provider 返回远程音频 URL 时，workflow 必须先导入本地对象存储，再把导入后的 audio object key 传给视频渲染边界。
- EC-2：封面 Mock 失败时，本批允许收口为 `PACKAGE_BUILD_FAILED`；真实 Image 2 阶段必须补默认封面兜底。
- EC-3：视频 Mock 失败时，不得调用发布包准备、发布包审核、对象存储写包或权益提交。
- EC-4：timeline 生成失败视为视频渲染失败，按 `PACKAGE_BUILD_FAILED` 收口。
- EC-5：render-worker 本地样例渲染失败时，不影响后端 Mock 测试通过，但必须记录为第 7 批剩余风险。

## 7. API Contracts

本批不新增 OpenAPI 路径，复用既有接口：

```ts
type MediaAssets = {
  audio_url?: string;
  cover_url?: string;
  video_url?: string;
  video_duration_ms?: number;
  video_file_size_bytes?: number;
};

type PublishPackageJson = {
  work_id: string;
  video: {
    url: string;
    mime_type: "video/mp4";
    file_size_bytes: number;
    checksum: string;
  };
  cover: {
    url: string;
    mime_type: "image/png";
    file_size_bytes: number;
    checksum: string;
  };
  lyrics: {
    text: string;
    timeline_url: string;
  };
  metadata: {
    song_title: string;
    song_summary: string;
    source: string;
  };
};
```

内部服务合约：

```ts
type CoverGenerationRequest = {
  workId: string;
  songTitle: string;
  songSummary: string;
  lyricsText: string;
  musicPrompt: string;
};

type VideoRenderRequest = {
  workId: string;
  songTitle: string;
  lyricsText: string;
  audioObjectKey: string;
  audioMimeType: string;
  coverObjectKey: string;
  durationMs: number;
};
```

## 8. 数据模型

| 实体 | 字段 | 类型 | 约束 |
|---|---|---|---|
| `media_assets` | `asset_type` | string | `AUDIO` / `COVER` / `VIDEO` / `TIMELINE` |
| `media_assets` | `object_key` | string | 必填，发布包 URL 由该 key 推导 |
| `media_assets` | `mime_type` | string | 必填 |
| `media_assets` | `width` | integer | 封面和视频为 1920 |
| `media_assets` | `height` | integer | 封面和视频为 1080 |
| `media_assets` | `duration_ms` | integer | 音频、视频、时间轴必须有 |
| `media_assets` | `metadata_json` | JSON | 记录 provider/renderer/template/safe-area 等审计信息 |
| `publish_packages` | `package_json` | JSON | 必须引用生成后的媒体资产 |

## 9. 非目标

- OS-1：不接入真实 Image 2 API；真实协议仍待公司确认。
- OS-2：不实现封面重生完整真实链路；保留既有 API，后续批次强化。
- OS-3：不实现多视频比例导出；首期只做 16:9。
- OS-4：不实现逐字 K 歌字幕；首期只做句级或 Mock timeline。
- OS-5：不让 Gemini 前端修改正式 `apps/web`；前端原型可放在独立 `prototypes/gemini-web-v1`。
