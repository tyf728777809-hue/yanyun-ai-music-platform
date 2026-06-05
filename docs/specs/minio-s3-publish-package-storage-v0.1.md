# MinIO/S3 Publish Package Storage v0.1

更新时间：2026-06-06

## 1. 标题与元数据

- 标题：第 8 批 MinIO/S3 发布包强化
- 作者：Codex
- 状态：Approved for local MinIO implementation
- 适用范围：对象存储配置、发布包 JSON 写入、发布包 URL 重新签发、本地 MinIO smoke
- 评审依据：PRD v0.3、技术方案 v0.2、OpenAPI v0.1、第 7 批封面与 MP4 成片基础规格

## 2. 背景

当前本地链路已经可以把作品推进到 `GENERATED / PACKAGE_READY`，并通过 `LocalObjectStorageClient` 把发布包 JSON 写入 `build/local-object-storage`。第 7 批也建立了封面、视频和时间轴的 Mock/Fake 媒体生成边界。但这仍不是公司接入前需要的对象存储口径：公司部署阶段需要能够把发布包交接物写入 MinIO/S3 兼容存储，并按可过期 URL 暴露给接入方。

第 8 批的目标是强化发布包对象存储层，不把真实云账号、公司对象存储或真实媒体上传扩大进来。系统必须能通过配置在本地文件存储和 S3/MinIO 存储之间切换；本地 smoke 必须使用 Docker Compose 里的 MinIO，写入发布包 JSON，并返回可访问的 URL。

本批仍不要求 Java workflow 调用 render-worker 产出真实业务 MP4，也不要求封面、视频、时间轴真实文件全部上传。该部分会在 render-worker 真实调用和媒体产物落盘后继续推进。

## 3. 功能需求

- FR-1：系统 MUST 支持 `OBJECT_STORAGE_PROVIDER=local|s3`，默认 `local`，显式 `s3` 时使用 S3/MinIO 兼容对象存储。
- FR-2：S3/MinIO 客户端 MUST 支持 endpoint override、region、bucket、access key、secret key、path-style 和 URL TTL 配置。
- FR-3：本地 MinIO 模式 MUST 使用 `.env.example` 中的非生产默认值，并 MUST NOT 需要真实云账号或公司凭据。
- FR-4：发布包 JSON MUST 通过 `ObjectStorageClient.putObject` 写入当前配置的对象存储。
- FR-5：发布包 URL MUST 通过对象存储客户端按 object key 生成；刷新发布包 URL 时 MUST 使用已持久化的 `package_object_key`，不得重新猜测路径。
- FR-6：S3/MinIO 模式 SHOULD 使用 presigned GET URL，并携带明确的 `package_url_expires_at`。
- FR-7：本地文件模式 MAY 继续返回公开 URL，但 MUST 同样返回可配置 TTL 的 `package_url_expires_at`，保证 API 口径一致。
- FR-8：发布包 object key SHOULD 向技术方案 v0.2 的分层结构靠拢，格式为 `yanyun-ai-music/{env}/{yyyy}/{mm}/{dd}/{work_id}/package/publish-package.json`。
- FR-9：作品详情和发布包 JSON 中的媒体 URL MUST 通过统一对象存储 URL 生成口径产生，不得继续在业务代码中硬编码 `http://localhost:9000/yanyun-works-local/`。
- FR-10：S3/MinIO 写入失败 MUST 收口为 `PACKAGE_BUILD_FAILED`，释放已锁权益，不提交主权益，不生成 `PACKAGE_READY`。

## 4. 非功能需求

- NFR-1：自动化测试 MUST NOT 调用真实 AWS、真实公司对象存储或真实生产 MinIO。
- NFR-2：对象存储客户端 MUST NOT 在日志、异常、测试快照、文档或提交信息中暴露 access key、secret key、签名 URL 中的 credential/signature 细节。
- NFR-3：object key MUST 阻止路径穿越、空 key、反斜杠和连续 `..` 段。
- NFR-4：本地 MinIO smoke SHOULD 能在 30 秒内完成一次发布包写入和 URL 访问验证。
- NFR-5：默认配置 MUST 保持现有 `local` 模式可用，避免破坏当前 Mock 主链路。

## 5. 验收标准

- AC-1：Given 默认配置，When 确认出歌，Then 发布包 JSON 写入本地文件存储，作品进入 `GENERATED / PACKAGE_READY`。覆盖 FR-1、FR-4、NFR-5。
- AC-2：Given `OBJECT_STORAGE_PROVIDER=s3` 且本地 MinIO 正常运行，When 确认出歌，Then 发布包 JSON 写入 MinIO bucket，`GET publish-package` 返回可访问 URL。覆盖 FR-1、FR-2、FR-3、FR-4、FR-6。
- AC-3：Given 发布包已生成，When 调用 `POST /publish-package/refresh-url`，Then 系统使用持久化的 `package_object_key` 重新签发 URL，并更新 `package_url_expires_at`。覆盖 FR-5、FR-6、FR-7。
- AC-4：Given Mock workflow 成功，When 抽查 `publish_packages.package_object_key`，Then key 符合 `yanyun-ai-music/{env}/{yyyy}/{mm}/{dd}/{work_id}/package/publish-package.json`。覆盖 FR-8。
- AC-5：Given storage provider 配置非法或 S3 写入失败，When workflow 执行，Then 作品失败码为 `PACKAGE_BUILD_FAILED`，权益释放，发布包不生成。覆盖 FR-10。
- AC-6：Given 业务代码生成媒体 URL，When 搜索硬编码 asset base URL，Then workflow 和 work detail 不再直接硬编码本地对象存储 URL。覆盖 FR-9。

## 6. 边界情况

- EC-1：MinIO bucket 不存在时，本地开发配置 MAY 自动创建 bucket；生产配置是否自动创建由部署策略决定。
- EC-2：S3/MinIO access key 或 secret key 缺失时，应用 MUST 在构造 S3 客户端或首次写入前失败，并给出不含密钥值的错误信息。
- EC-3：presigned URL 已过期时，接入方可调用 refresh URL；mark-fetched 只记录交接，不代表社区发布成功。
- EC-4：`publish_packages.package_object_key` 缺失时，refresh URL MUST 返回业务错误，不得猜测默认路径。
- EC-5：当前 Mock 封面、视频、时间轴 object key 可能尚无真实对象；本批不把这些 URL 的可下载性作为成功条件。

## 7. API Contracts

本批不新增 OpenAPI 路径，强化既有响应字段：

```ts
type PublishPackage = {
  work_id: string;
  package_status: "PACKAGE_READY" | "PACKAGE_NOT_READY" | "PACKAGE_FETCHED" | "PACKAGE_EXPIRED" | "PACKAGE_BLOCKED";
  package_url: string | null;
  package_url_expires_at: string | null;
  package_json: PublishPackageJson | null;
  available_actions: string[];
  blocked_reason: string | null;
};

type ObjectStorageConfig = {
  provider: "local" | "s3";
  bucket: string;
  endpoint?: string;
  public_endpoint?: string;
  region: string;
  path_style_enabled: boolean;
  url_ttl: string;
};
```

## 8. 数据模型

| 实体 | 字段 | 类型 | 约束 |
|---|---|---|---|
| `publish_packages` | `package_object_key` | text | 必填，refresh URL 使用该字段 |
| `publish_packages` | `package_url` | text | 当前有效 URL，可随 refresh 更新 |
| `publish_packages` | `package_url_expires_at` | timestamptz | 当前 URL 过期时间 |
| `media_assets` | `object_key` | text | 媒体 URL 通过统一对象存储 URL 口径推导 |
| `workflow_outbox` | N/A | N/A | 本批不修改 |

## 9. 非目标

- OS-1：不接入真实 AWS 账号或公司生产对象存储。
- OS-2：不把真实 DreamMaker 音频、真实 Image 2 封面或真实 render-worker MP4 全量上传作为本批完成条件。
- OS-3：不改变公司发布、审核、分享、账号、权益真实系统边界。
- OS-4：不新增用户侧接口路径；只强化现有发布包 URL 和对象存储实现。
- OS-5：不把 presigned URL、access key、secret key 或生产 endpoint 写入测试快照和项目文档。
