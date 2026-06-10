# Render Worker Local Process Integration v0.1

更新时间：2026-06-06

## 1. 标题与元数据

- 标题：第 11 批 Java 主链路到 Remotion render-worker 的本地进程调用边界
- 作者：Codex
- 状态：Approved for local process implementation
- 适用范围：Java `VideoRenderService` 真实调用边界、`apps/render-worker` 作业 CLI、本地对象存储回写
- 评审依据：PRD v0.3、技术方案 v0.2、OpenAPI v0.1、第 7 批封面与视频成片规格、第 8 批对象存储规格

## 2. 背景

第 7 批已经把视频生成从业务 workflow 中抽象为 `VideoRenderService`，并提供了 Remotion 16:9 样例渲染。但 Java 主链路当前默认仍只调用 `MockVideoRenderService`，无法把真实 Remotion 产物纳入作品生成闭环。

本批目标是在不影响前端并行开发、不调用真实 Image 2 或公司系统的前提下，补齐 Java 到 `apps/render-worker` 的最小真实调用边界。默认配置仍使用 Mock，显式切换到本地进程模式后，Java 侧调用 render-worker CLI，render-worker 输出 MP4 和 timeline JSON，Java 侧再写入当前对象存储并返回媒体资产描述。

## 3. 功能需求

- FR-1：系统 MUST 保留 `yanyun.render-worker.mode=mock` 默认值，默认链路不得触发真实 Remotion 渲染。
- FR-2：系统 MUST 支持 `yanyun.render-worker.mode=local-process`，由 Java `VideoRenderService` 通过外部进程调用 `apps/render-worker`。
- FR-3：render-worker CLI MUST 接收 JSON 输入文件，至少包含 `work_id`、`song_title`、`song_summary`、`lyrics_text`、`audio_object_key`、`audio_mime_type`、`cover_object_key`、`duration_ms`。
- FR-4：render-worker CLI MUST 输出 JSON 结果文件，至少包含 `work_id`、`video_file_path`、`timeline_file_path`、`width`、`height`、`fps`、`duration_ms`、`duration_in_frames`、`renderer`、`composition_id`。
- FR-5：Java 本地进程适配器 MUST 将 render-worker 输出的 MP4 和 timeline JSON 写入 `ObjectStorageClient`，并返回 `VIDEO` 与 `TIMELINE` 两类 `MediaAssetDescriptor`。
- FR-6：Java 本地进程适配器 MUST 使用对象存储 object key，而不是本地临时文件路径，作为 workflow 后续发布包引用。
- FR-7：render-worker `LyricVideo16x9V2` composition MUST 支持按输入 `duration_ms` 计算 `durationInFrames`，避免默认 8 秒样例污染真实歌曲链路，并输出包含 video/audio 双轨的 MP4。
- FR-8：本批 MUST NOT 接入真实公司账号、审核、权益、发布、分享系统。
- FR-9：本批 MUST NOT 提交大体积 MP4、Node `node_modules`、真实密钥或本地构建产物。

## 4. 非功能需求

- NFR-1：自动化测试 MUST NOT 启动真实 Remotion 长视频渲染。
- NFR-2：本地进程适配器 MUST 有超时控制；超时或进程非 0 退出时必须抛出可被 workflow 收口为 `PACKAGE_BUILD_FAILED` 的异常。
- NFR-3：进程日志进入异常信息前 SHOULD 截断，避免过长日志污染失败记录。
- NFR-4：Java 侧返回的 `fileSizeBytes` MUST 来自已写入对象存储的实际字节数。
- NFR-5：Java 侧返回的 `checksum` MUST 为产物内容的 SHA-256 hex。
- NFR-6：CLI 输入输出 MUST 不包含 AccessKey、SecretKey、JWT、Cookie 或公司系统用户 token。

## 5. 验收标准

- AC-1：Given 默认配置，When Spring 创建 `VideoRenderService`，Then 使用 `MockVideoRenderService`，不会调用 render-worker。覆盖 FR-1。
- AC-2：Given `local-process` 配置和一个成功的本地进程，When `renderVideo` 执行，Then Java 会上传 MP4 和 timeline，并返回对象存储 object key。覆盖 FR-2、FR-5、FR-6。
- AC-3：Given render-worker CLI 输入 `duration_ms=180000`，When 转换为 Remotion props，Then `durationInFrames=5400` 且歌词时间轴覆盖完整视频区间。覆盖 FR-7。
- AC-4：Given 本地进程非 0 退出，When `renderVideo` 执行，Then 抛出异常且不返回本地文件路径。覆盖 NFR-2。
- AC-5：Given render-worker 单元测试运行，When 执行 `npm test`，Then 不产生真实长视频渲染。覆盖 NFR-1。
- AC-6：Given 后端全量测试运行，When 执行 Gradle 测试，Then 默认仍不触发真实 render-worker 或外部系统调用。覆盖 FR-1、NFR-1。

## 6. 边界情况

- EC-1：render-worker 输出 JSON 文件不存在时，Java 适配器必须失败。
- EC-2：render-worker 输出的 MP4 或 timeline 文件不存在时，Java 适配器必须失败。
- EC-3：输入歌词为空时，render-worker 必须生成可渲染的兜底字幕行。
- EC-4：输入 `duration_ms` 缺失或非法时，render-worker 必须使用安全默认时长。
- EC-5：Java 进程超时时，必须强制终止子进程并返回明确异常。
- EC-6：本地进程模式只适合本地验证和内网部署前 smoke；生产可替换为 HTTP/队列化 render service，但不得改变 `VideoRenderService` 调用方。

## 7. API Contracts

本批不新增用户侧 OpenAPI 路径。内部 CLI 输入：

```ts
type RenderWorkerJobInput = {
  work_id: string;
  song_title: string;
  song_summary?: string;
  lyrics_text: string;
  audio_object_key: string;
  audio_mime_type?: string;
  cover_object_key: string;
  audio_source_path?: string;
  cover_source_path?: string;
  template_id?: "lyric-video-16x9-v2" | "lyric-video-16x9-v3";
  duration_ms?: number;
  composition_id?: "LyricVideo16x9V2" | "LyricVideo16x9V3";
};
```

`audio_source_path` 和 `cover_source_path` 如果是本地文件路径，render-worker CLI 必须先把文件复制到本次输出目录下的 Remotion public 目录，再通过 `staticFile()` 生成浏览器可加载 URL。不要直接把裸 `file://` 路径交给 `<Img>` 或 `<Audio>`，否则 Chromium 可能拒绝加载本地资源，尤其是在路径包含中文、空格或来自输出目录外部时。

内部 CLI 输出：

```ts
type RenderWorkerJobOutput = {
  work_id: string;
  video_file_path: string;
  timeline_file_path: string;
  width: 1920;
  height: 1080;
  fps: 30;
  duration_ms: number;
  duration_in_frames: number;
  renderer: "remotion";
  composition_id: "LyricVideo16x9V2" | "LyricVideo16x9V3";
};
```

Java 配置：

```yaml
yanyun:
  render-worker:
    mode: mock # mock | local-process
    working-directory: apps/render-worker
    command: npm
    arguments:
      - run
      - render:job
      - --
    timeout: PT10M
```

## 8. 数据模型

本批不新增数据库表。`media_assets` 继续承载结果：

| 实体 | 字段 | 类型 | 约束 |
|---|---|---|---|
| `media_assets` | `asset_type` | string | `VIDEO` / `TIMELINE` |
| `media_assets` | `object_key` | string | 来自对象存储写入结果 |
| `media_assets` | `mime_type` | string | `video/mp4` 或 `application/json` |
| `media_assets` | `file_size_bytes` | long | 实际写入字节数 |
| `media_assets` | `checksum` | string | SHA-256 hex |
| `media_assets` | `metadata_json` | JSON | renderer、composition、safe area、source mode |

## 9. 非目标

- OS-1：不实现真实 Image 2 封面生成。
- OS-2：不实现公司系统真实账号、审核、权益、发布、分享接入。
- OS-3：不实现队列化渲染服务或 HTTP render service；本批只做本地进程模式。
- OS-4：不保证自动化测试渲染 2-4 分钟真实 MP4；长视频渲染只作为手动 smoke。
- OS-5：不让本 Agent 修改用户正在并行开发的 `prototypes/` 前端原型。
