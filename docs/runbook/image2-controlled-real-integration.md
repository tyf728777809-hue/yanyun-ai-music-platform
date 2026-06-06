# Image 2 封面受控真实联调 Runbook

## 目标

本 Runbook 用于后续手动联调真实 Image 2 封面生成链路。目标是在只打开 Image 2 生图 Provider 的前提下，验证 `CoverPromptAgent` 输出的视觉提示词可以生成 16:9 封面，并导入平台对象存储、进入媒体资产和发布包。

当前代码状态：截至 2026-06-06，`CoverGenerationService` 仍只注册 `MockCoverGenerationService`。本 Runbook 是真实客户端实现前的操作基线；执行真实联调前，必须先补 `RealImage2CoverGenerationService` / 等价真实 Provider、硬开关、超时、失败码映射、默认封面兜底和脱敏日志。

## 硬性规则

- 默认禁止真实 Image 2 请求：`IMAGE_PROVIDER=mock` 且 `IMAGE_REAL_CALLS_ENABLED=false`。
- 只有手动联调窗口内才允许设置 `IMAGE_PROVIDER=image2` 和 `IMAGE_REAL_CALLS_ENABLED=true`。
- 单次联调只打开 Image 2；DeepSeek、Suno、MiniMax 和公司 Adapter 继续 Mock 或关闭。
- 不把 API Key、鉴权 header、完整图像生成 Prompt、完整请求/响应、供应商临时图片 URL 写入仓库、文档、测试、截图、日志或提交信息。
- 自动化测试仍只允许 Mock/Fake，不调用真实 Image 2、DeepSeek、Suno、MiniMax 或公司系统。
- Image 2 是 Provider Adapter；`CoverPromptAgent` 只负责规划视觉 prompt，不直接调用生图服务。

## 前置检查

```bash
git status --short
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :modules:image2:test :modules:creative-agent:test :apps:music-api:test --tests '*MockSongProductionWorkflow*'
docker compose -f deploy/docker-compose.yml --env-file .env.example up -d
docker compose -f deploy/docker-compose.yml ps
```

确认 PostgreSQL、MinIO/S3、Redis 等本地服务健康后再继续。

## 凭据注入

只在当前终端会话或公司 Secret 系统注入，禁止写入 `.env.example` 或任何提交文件：

```bash
export IMAGE_PROVIDER=image2
export IMAGE_REAL_CALLS_ENABLED=true
export IMAGE2_BASE_URL="<from-secure-channel>"
export IMAGE2_API_KEY="<from-secure-channel>"
export IMAGE2_MODEL_NAME="<approved-model>"
export IMAGE2_TIMEOUT_MS=45000
export IMAGE2_MAX_ATTEMPTS=1
export IMAGE2_WIDTH=1920
export IMAGE2_HEIGHT=1080
```

联调结束后执行：

```bash
unset IMAGE2_BASE_URL IMAGE2_API_KEY IMAGE2_MODEL_NAME
unset IMAGE_REAL_CALLS_ENABLED IMAGE_PROVIDER
unset IMAGE2_TIMEOUT_MS IMAGE2_MAX_ATTEMPTS IMAGE2_WIDTH IMAGE2_HEIGHT
```

## 推荐启动方式

Image 2 封面生成发生在确认出歌后的生产 workflow 中。真实联调建议仍保持音乐和 DeepSeek Mock，避免一次打开多个外部成本点：

```bash
IMAGE_PROVIDER=image2 \
IMAGE_REAL_CALLS_ENABLED=true \
MUSIC_PROVIDER=mock \
DREAMMAKER_REAL_CALLS_ENABLED=false \
DEEPSEEK_REAL_CALLS_ENABLED=false \
COMPANY_ACCOUNT_ADAPTER_MODE=mock \
COMPANY_MODERATION_ADAPTER_MODE=mock \
COMPANY_QUOTA_ADAPTER_MODE=mock \
COMPANY_PUBLISH_ADAPTER_MODE=mock \
COMPANY_SHARE_ADAPTER_MODE=mock \
  ./gradlew :apps:music-api:bootRun
```

如果后续把封面生成拆到 worker 或 Temporal activity，真实环境变量必须注入到实际发起 Image 2 请求的进程。

## 联调步骤

### 1. 基础健康检查

```bash
curl http://localhost:8080/health
curl http://localhost:8080/internal/integration-readiness
```

期望：

- API 可用。
- readiness 能看出 Image 2 真实调用开关状态。
- DeepSeek、DreamMaker、公司 Adapter 仍处于 Mock 或关闭状态。

### 2. 创建作品并确认出歌

1. 调用 `POST /api/v1/works/lyrics` 或 `POST /api/v1/works/inspiration`，拿到 `work_id`。
2. 查询 `GET /api/v1/works/{work_id}`，拿到 `lyrics_draft_id`。
3. 调用 `POST /api/v1/works/{work_id}/confirm`，保持 `music_provider=mock` 或不传 Provider。
4. 轮询作品直到 `GENERATED / PACKAGE_READY` 或进入失败状态。

### 3. 抽查封面资产

```bash
docker exec yanyun-postgres psql -U postgres -d yanyun_music -Atc \
  "select asset_type, provider, object_key, mime_type, width, height, metadata_json from media_assets where work_id = '{work_id}' and asset_type = 'COVER'"
```

期望：

- `asset_type=COVER`。
- `provider` 或 metadata 能区分真实 Image 2 模型。
- `width=1920`、`height=1080`。
- `object_key` 指向平台对象存储，不是供应商原始 URL。
- metadata 不包含 API Key、鉴权 header 或完整供应商 payload。

### 4. 验证发布包

调用 `GET /api/v1/works/{work_id}/publish-package`，确认：

- `cover.url` 可访问。
- 发布包只暴露平台签名 URL。
- `video.url`、`lyrics.timeline_url` 仍正常。
- 成品页可使用 `media_assets.cover_url` 展示封面。

## 失败处理

- `IMAGE_REAL_CALLS_ENABLED=false` 导致失败：这是保护机制，确认是否真的进入手动联调窗口。
- 缺失 `IMAGE2_BASE_URL` 或 `IMAGE2_API_KEY`：必须在外部 HTTP 请求前失败，不得发出真实请求。
- HTTP 401 / 403：停止继续联调，确认密钥权限，不要重复试错。
- HTTP 429 或限流：停止触发新样本，记录脱敏错误码和时间窗口。
- 生成内容违规或被供应商阻断：记录脱敏错误码，确认是否需要调整 `CoverPromptAgent` 的 negative prompt 或安全约束。
- 供应商图片 URL 下载失败：记录 `work_id`、供应商任务 id、平台错误码，确认 URL 是否过期或需要鉴权。
- 任何异常都不要粘贴完整请求、完整响应、鉴权 header、供应商原始图片 URL 或用户敏感输入。

## 默认封面兜底

真实 Image 2 阶段必须补默认封面兜底策略：

- 供应商失败但音乐和视频可继续时，优先使用平台默认 16:9 封面。
- 若默认封面不可用，才按 `PACKAGE_BUILD_FAILED` 收口。
- 兜底封面也必须进入 `media_assets` 和发布包，不得使用前端临时占位图冒充平台资产。

## 快速回滚

```bash
unset IMAGE2_BASE_URL IMAGE2_API_KEY IMAGE2_MODEL_NAME
unset IMAGE_REAL_CALLS_ENABLED IMAGE_PROVIDER
export IMAGE_PROVIDER=mock
```

必要时停止 API 进程，确认本地主链路回到 Mock：

```bash
EXPECTED_DURATION_MS=1000 scripts/smoke/api-main-flow.sh
```

## 证据记录

每次联调只记录以下字段，不记录真实密钥、鉴权 header、完整 Prompt、完整供应商请求/响应或供应商原始图片 URL：

| Field | Value |
|---|---|
| 时间 |  |
| Image 2 模型 |  |
| `work_id` |  |
| 封面尺寸 |  |
| 平台 `cover.object_key` |  |
| 供应商 task id |  |
| 对象存储导入 | 成功 / 失败 |
| `works.status` |  |
| `package_status` |  |
| 失败码 |  |
| 是否使用默认封面兜底 | 是 / 否 |
| 结论 | 通过 / 待修复 |

## 进入真实联调前仍需用户提供

- Image 2 API base URL。
- API Key 或公司 Secret 注入方式。
- 认证 header 格式。
- 模型名和可用参数。
- 请求/响应 schema 或供应商文档。
- 图片尺寸、格式、风格、安全词和输出 URL TTL。
- 限流、超时、重试、计费和内容安全要求。
- 失败码样本和 retryable / non-retryable 口径。
